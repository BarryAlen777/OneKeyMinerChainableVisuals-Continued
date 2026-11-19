package cn.blockforge.generated.modad7bbe58.client;

import net.minecraft.core.BlockPos;

import java.util.List;

/**
 * Union-silhouette line extractor written for this mod.
 *
 * <p>Drawing one wireframe box per block turns a 1000-block chain into an unreadable tangle.
 * Instead, this walks the grid edges of the block set and keeps only the <em>crease</em> lines.
 * For every unit grid edge the four blocks around it are sampled as a 2x2 occupancy mask:</p>
 * <ul>
 *   <li>mask 0 or 15 (nothing / everything solid): buried, dropped;</li>
 *   <li>mask with one or three solids: a real convex or concave corner, kept;</li>
 *   <li>mask with two <em>diagonal</em> solids (6 or 9): a pinch where two groups touch along
 *       a single edge, kept - both corners genuinely turn there;</li>
 *   <li>mask with two <em>side-adjacent</em> solids (3, 5, 10, 12): a flat coplanar seam -
 *       the surface runs straight through, dropped.</li>
 * </ul>
 * <p>Contiguous crease segments on the same grid line are rendered as one straight edge.</p>
 *
 * <p>All bookkeeping is primitive-array based (relative coordinates packed into longs and an
 * open-addressing long hash set) so rebuilding a 1000+ block outline allocates no per-block
 * objects; rebuilds stay cheap enough to run several times a second without GC hitches.
 * Result coordinates are relative to the group's minimum corner ({@link Data#ox()} etc.),
 * which the renderer uses as a camera-relative anchor - float vertex data never carries
 * absolute world magnitude, so lines do not jitter far from the origin.</p>
 *
 * <p>Edges are cached and only rebuilt when the source position list actually changes.</p>
 */
public final class MergedOutline {
    /** Line geometry in group-local space: 6 floats per edge (x1,y1,z1,x2,y2,z2), relative
     *  to the anchor (ox, oy, oz); cx/cy/cz is the centroid in the same local space. */
    public record Data(float[] core, double ox, double oy, double oz, float cx, float cy, float cz) { }

    private static final Data EMPTY = new Data(new float[0], 0, 0, 0, 0, 0, 0);

    /** Relative coordinate field width; a chain spans at most a few hundred blocks, so
     *  20 bits per axis plus 2 bits for the edge axis always fits in a long. */
    private static final int COORD_BITS = OutlineRuns.BITS;
    private static final int COORD_SHIFT_1 = COORD_BITS;
    private static final int COORD_SHIFT_2 = COORD_BITS * 2;
    private static final long COORD_MASK = OutlineRuns.MASK;

    private static List<BlockPos> source = List.of();
    private static Data data = EMPTY;
    // Scratch buffers reused across rebuilds; compute() only ever runs on the render thread.
    private static long[] candidateScratch = new long[0];
    private static final LongSet cellScratch = new LongSet();

    private MergedOutline() { }

    /**
     * Cached outline for an immutable position list. The list is stored by reference for the
     * identity fast path, so callers must never mutate a list after handing it in -
     * ChainScanner returns unmodifiable lists precisely to keep that contract cheap
     * (audit r17-2).
     */
    public static Data get(List<BlockPos> positions) {
        // Identity check first: the display list is cached per refresh, so in the common case
        // every frame hits this line for free instead of walking up to 8k BlockPos.equals.
        if (positions == source) return data;
        if (positions.size() == source.size() && positions.equals(source)) {
            source = positions;
            return data;
        }
        data = compute(positions);
        source = positions;
        return data;
    }

    /** Clears the cache; safe to call when the preview is hidden. */
    public static void reset() {
        source = List.of();
        data = EMPTY;
    }

    private static Data compute(List<BlockPos> positions) {
        int n = positions.size();
        if (n < 2) return EMPTY;

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        long sumX = 0, sumY = 0, sumZ = 0;
        for (BlockPos p : positions) {
            int x = p.getX(), y = p.getY(), z = p.getZ();
            if (x < minX) minX = x;
            if (y < minY) minY = y;
            if (z < minZ) minZ = z;
            if (x > maxX) maxX = x;
            if (y > maxY) maxY = y;
            if (z > maxZ) maxZ = z;
            sumX += x;
            sumY += y;
            sumZ += z;
        }
        // Group-local bounding box (grid corners extend one past the outer blocks).
        if (maxX - minX + 1 > COORD_MASK || maxY - minY + 1 > COORD_MASK || maxZ - minZ + 1 > COORD_MASK) {
            return EMPTY; // absurd span for a chain preview; draw nothing rather than corrupt keys
        }

        cellScratch.reset(n * 2);
        final LongSet cells = cellScratch;
        for (BlockPos p : positions) {
            cells.add(pack(p.getX() - minX, p.getY() - minY, p.getZ() - minZ));
        }

        if (candidateScratch.length < n * 12) candidateScratch = new long[n * 16];
        final long[] candidates = candidateScratch;
        int m = 0;
        for (BlockPos p : positions) {
            int rx = p.getX() - minX, ry = p.getY() - minY, rz = p.getZ() - minZ;
            for (int dy = 0; dy <= 1; dy++) for (int dz = 0; dz <= 1; dz++) candidates[m++] = packEdge(0, rx, ry + dy, rz + dz);
            for (int dx = 0; dx <= 1; dx++) for (int dz = 0; dz <= 1; dz++) candidates[m++] = packEdge(1, rx + dx, ry, rz + dz);
            for (int dx = 0; dx <= 1; dx++) for (int dy = 0; dy <= 1; dy++) candidates[m++] = packEdge(2, rx + dx, ry + dy, rz);
        }
        float[] core = OutlineRuns.collect(candidates, m, edge -> isCrease(edge, cells));
        if (core.length == 0) return EMPTY;
        return new Data(core, minX, minY, minZ,
                (float) ((double) sumX / n - minX + 0.5),
                (float) ((double) sumY / n - minY + 0.5),
                (float) ((double) sumZ / n - minZ + 0.5));
    }

    /**
     * Exact crease test on the 2x2 occupancy mask around a unit grid edge. Bit layout:
     * s00 = cell at (b-1, c-1), s10 = (b, c-1), s01 = (b-1, c), s11 = (b, c), where b and c
     * are the two axes perpendicular to the edge. Kept: 1 or 3 solids (real corners) or the
     * two diagonal 2-solid masks (6, 9: pinch corners). Dropped: 0, 4 solids, and the four
     * side-adjacent 2-solid masks (3, 5, 10, 12: flat coplanar seams).
     */
    private static boolean isCrease(long edge, LongSet cells) {
        int axis = OutlineRuns.axis(edge);
        int rx = OutlineRuns.x(edge), ry = OutlineRuns.y(edge), rz = OutlineRuns.z(edge);
        int mask;
        if (axis == 0) { // along X, cross-section in (Y, Z)
            mask = bit(cells.contains(pack(rx, ry - 1, rz - 1)), 0)
                    | bit(cells.contains(pack(rx, ry, rz - 1)), 1)
                    | bit(cells.contains(pack(rx, ry - 1, rz)), 2)
                    | bit(cells.contains(pack(rx, ry, rz)), 3);
        } else if (axis == 1) { // along Y, cross-section in (X, Z)
            mask = bit(cells.contains(pack(rx - 1, ry, rz - 1)), 0)
                    | bit(cells.contains(pack(rx, ry, rz - 1)), 1)
                    | bit(cells.contains(pack(rx - 1, ry, rz)), 2)
                    | bit(cells.contains(pack(rx, ry, rz)), 3);
        } else { // along Z, cross-section in (X, Y)
            mask = bit(cells.contains(pack(rx - 1, ry - 1, rz)), 0)
                    | bit(cells.contains(pack(rx, ry - 1, rz)), 1)
                    | bit(cells.contains(pack(rx - 1, ry, rz)), 2)
                    | bit(cells.contains(pack(rx, ry, rz)), 3);
        }
        int solids = Integer.bitCount(mask);
        return solids == 1 || solids == 3 || mask == 6 || mask == 9;
    }

    private static int bit(boolean set, int shift) {
        return set ? 1 << shift : 0;
    }

    /**
     * Packs a group-local cell coordinate triple. Valid keys are non-negative (all three
     * coordinates are clamped into {@code [0, 2^COORD_BITS)} by the span guard in
     * {@link #compute}). Negative probes - cells outside the group - deliberately collapse to
     * {@link LongSet#AIR_KEY} instead of relying on sign-extension happening to miss the valid
     * range: any negative key reads as air, and {@link LongSet} stores that key explicitly
     * rather than silently dropping it (the old key+1=0 sentinel made {@code pack(0,0,-1)=-1}
     * a real no-op bug waiting to happen).
     */
    private static long pack(int rx, int ry, int rz) {
        if (rx < 0 || ry < 0 || rz < 0) return LongSet.AIR_KEY;
        return ((long) rx << COORD_SHIFT_2) | ((long) ry << COORD_SHIFT_1) | (long) rz;
    }

    private static long packEdge(int axis, int rx, int ry, int rz) {
        return OutlineRuns.edge(axis, rx, ry, rz);
    }
}
