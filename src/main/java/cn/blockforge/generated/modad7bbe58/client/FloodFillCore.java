package cn.blockforge.generated.modad7bbe58.client;

/**
 * Minecraft-free flood-fill core behind {@link ChainScanner}'s local scan (r15 audit
 * report issue 3: the loop that shipped broken is now a pure function that the plain-JVM
 * regression tests in {@code src/test/java} execute directly - no Level, no game classes).
 *
 * <p>The scan walks same-block neighbours breadth-first from the aimed block. The queue is
 * a ring buffer, so its size bounds the <em>concurrent</em> frontier, not the total number
 * of enqueues; the remaining-slots guard below keeps the invariant
 * {@code found + pending <= max} at all times, so with {@code max <= HARD_CAP} the frontier
 * never grows past {@code HARD_CAP} cells and the {@code HARD_CAP + 32} sized queue cannot
 * overflow (the old monotonic-index version could run off the end on dense veins).</p>
 *
 * <p>The visited set stores packed absolute coordinates in a primitive {@link LongSet}, so
 * marking a rejected probe allocates nothing (the old {@code HashSet<BlockPos>} made ~100k
 * objects per scan at extreme settings). Each probe pays one 64-bit mix hash instead of the
 * BitSet index arithmetic r12 used; that trade is deliberate - a depth-256 scan would need a
 * 513^3-bit (~17 MB) bitmap otherwise (r15 audit report issue 6: acknowledged cost, the
 * allocation win and the depth ceiling are worth more).</p>
 */
final class FloodFillCore {

    /**
     * Neighbour order for the flood fill: the 6 axial cells first, then (diagonal mode only)
     * the 12 face-diagonals and finally the 8 corners. Axial-first keeps the BFS frontier
     * narrow along corridors, so a capped scan covers long thin veins before it burns the
     * budget on blob spread.
     *
     * <p>The r15 table was silently wrong and is fixed here: it contained two zero vectors,
     * nine duplicate offsets, all eight true corners missing and the z=-1 x-face diagonals
     * missing - diagonal mode could never follow a corner connection (never covered by the
     * r15 report; caught while extracting this class for the regression tests, which now
     * assert the table is exactly the 26 distinct non-zero offsets).</p>
     */
    static final int[] NX = {1, -1, 0, 0, 0, 0,
            1, 1, -1, -1, 1, 1, -1, -1, 0, 0, 0, 0,
            1, 1, 1, 1, -1, -1, -1, -1};
    static final int[] NY = {0, 0, 1, -1, 0, 0,
            1, -1, 1, -1, 0, 0, 0, 0, 1, 1, -1, -1,
            1, 1, -1, -1, 1, 1, -1, -1};
    static final int[] NZ = {0, 0, 0, 0, 1, -1,
            0, 0, 0, 0, 1, -1, 1, -1, 1, -1, 1, -1,
            1, -1, 1, -1, 1, -1, 1, -1};

    /** True when the cell at these absolute coordinates belongs to the chain target set. */
    @FunctionalInterface
    interface CellProbe {
        boolean isTarget(int x, int y, int z);
    }

    private FloodFillCore() { }

    /**
     * Expands from the root in BFS order, accepting at most {@code max} cells (the root
     * included) within {@code depth} Manhattan distance of the root. The root itself is
     * always accepted - the caller validates it before invoking.
     *
     * @param queue   ring-buffer scratch; must be at least {@code max + 32} long
     * @param visited scratch visited set; reset here with a size estimate for this scan
     *                (r15 audit report issue 5: reset also shrinks an oversized table back
     *                down, so one huge scan no longer pins ~4 MB and re-zeroes it per scan
     *                while the player stands on small veins)
     * @param out     receives the visited cells' packed keys in BFS order; at least
     *                {@code max} long
     * @return number of keys written to {@code out} (0 only when {@code max <= 0})
     */
    static int floodFill(int rootX, int rootY, int rootZ, int max, int depth,
                         int neighbourCount, CellProbe probe,
                         long[] queue, LongSet visited, long[] out) {
        if (max <= 0) return 0;
        // Rough probe estimate for table sizing: accepted cells * neighbours / 3 (most
        // neighbour probes are interior hits the set already knows). Clamped so a default
        // 64-block scan starts tiny and an 8k-block diagonal scan pre-sizes instead of
        // rehashing its way up from nothing.
        visited.reset(Math.min(max * Math.max(neighbourCount, 6) / 3 + 16, 1 << 17));
        final int qLen = queue.length;
        int head = 0, tail = 1, pending = 1; // the queued root IS one pending frontier cell
        long rootKey = PackedPos.pack(rootX, rootY, rootZ);
        visited.add(rootKey);
        queue[0] = rootKey;

        int found = 0;
        while (pending > 0 && found < max) {
            long cur = queue[head];
            if (++head == qLen) head = 0;
            pending--;
            int cx = PackedPos.unpackX(cur), cy = PackedPos.unpackY(cur), cz = PackedPos.unpackZ(cur);
            out[found++] = cur;
            for (int i = 0; i < neighbourCount; i++) {
                int nx = cx + NX[i], ny = cy + NY[i], nz = cz + NZ[i];
                int dx = nx - rootX, dy = ny - rootY, dz = nz - rootZ;
                if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > depth) continue;
                long key = PackedPos.pack(nx, ny, nz);
                if (!visited.addIfAbsent(key)) continue; // probed once, ever
                if (!probe.isTarget(nx, ny, nz)) continue;
                // Never keep more cells queued than the accept limit still has free:
                // found + pending <= max holds at all times, so the ring can never overflow.
                if (found + pending >= max) break;
                queue[tail] = key;
                if (++tail == qLen) tail = 0;
                pending++;
            }
        }
        return found;
    }
}
