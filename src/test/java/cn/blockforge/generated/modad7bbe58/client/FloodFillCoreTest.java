package cn.blockforge.generated.modad7bbe58.client;

import java.util.HashSet;
import java.util.Set;

/**
 * Exercises the exact BFS loop that shipped broken in r15: the root cell was enqueued
 * without counting towards {@code pending}, so the while-guard failed on the first test and
 * every local scan returned an empty list (r15 audit report issue 1, P0). These tests run
 * the production {@link FloodFillCore} against a fake block world; if that class ever
 * returns 0 for a populated root again, the build fails here instead of in someone's game.
 */
public final class FloodFillCoreTest {

    private FloodFillCoreTest() { }

    static void run() {
        neighbourTableIntegrity();
        rootRegression();
        depthGate();
        capAndRingWrap();
        diagonalModes();
        degenerateInputs();
    }

    /**
     * The 26-offset table must be exactly the distinct non-zero neighbours (the r15 table
     * shipped two zero vectors, nine duplicates, no corners and no z=-1 x-face diagonals -
     * diagonal mode silently under-connected veins through any corner).
     */
    private static void neighbourTableIntegrity() {
        java.util.Set<Long> seen = new java.util.HashSet<>();
        RegressionTests.checkEquals(26, FloodFillCore.NX.length, "NX length");
        RegressionTests.checkEquals(26, FloodFillCore.NY.length, "NY length");
        RegressionTests.checkEquals(26, FloodFillCore.NZ.length, "NZ length");
        int corners = 0;
        for (int i = 0; i < 26; i++) {
            int dx = FloodFillCore.NX[i], dy = FloodFillCore.NY[i], dz = FloodFillCore.NZ[i];
            RegressionTests.check(!(dx == 0 && dy == 0 && dz == 0), "offset " + i + " is the zero vector");
            RegressionTests.check(seen.add(PackedPos.pack(dx, dy, dz)), "offset " + i + " duplicated");
            if (Math.abs(dx) == 1 && Math.abs(dy) == 1 && Math.abs(dz) == 1) corners++;
        }
        RegressionTests.checkEquals(8, corners, "all 8 corner neighbours must be present");
        // Axial-first layout: the first six are the face-connected neighbours.
        int axial = 0;
        for (int i = 0; i < 6; i++) {
            if (Math.abs(FloodFillCore.NX[i]) + Math.abs(FloodFillCore.NY[i]) + Math.abs(FloodFillCore.NZ[i]) == 1) axial++;
        }
        RegressionTests.checkEquals(6, axial, "first six offsets are the axial ones");
    }

    /** Fake world: a set of accepted cells keyed like the scan's visited set. */
    private static FloodFillCore.CellProbe world(Set<Long> cells) {
        return (x, y, z) -> cells.contains(PackedPos.pack(x, y, z));
    }

    private static Set<Long> tunnel(int fromX, int toX, int y, int z) {
        Set<Long> cells = new HashSet<>();
        for (int x = fromX; x <= toX; x++) cells.add(PackedPos.pack(x, y, z));
        return cells;
    }

    private static int count(Set<Long> world, int rootX, int rootY, int rootZ,
                             int max, int depth, int neighbourCount,
                             long[] queue, LongSet visited, long[] out) {
        return FloodFillCore.floodFill(rootX, rootY, rootZ, max, depth, neighbourCount,
                world(world), queue, visited, out);
    }

    /** The P0 regression: 64-cell straight tunnel, axial, root at one end. */
    private static void rootRegression() {
        Set<Long> w = tunnel(0, 100, 70, 0);
        long[] queue = new long[8192 + 32];
        long[] out = new long[8192];
        LongSet visited = new LongSet();
        int found = count(w, 0, 70, 0, 64, 256, 6, queue, visited, out);
        RegressionTests.checkEquals(64, found,
                "r15 root-pending regression: full 64-cell walk expected, not the empty scan r15 shipped");
        // BFS order: straight down the +x corridor, starting at the root itself.
        for (int i = 0; i < found; i++) {
            RegressionTests.checkEquals(i, PackedPos.unpackX(out[i]), "cell " + i + " must sit at x=" + i);
            RegressionTests.checkEquals(70, PackedPos.unpackY(out[i]), "tunnel y stays 70");
        }
    }

    /** Manhattan reach: depth 16 caps a long tunnel at root + 16. */
    private static void depthGate() {
        Set<Long> w = tunnel(0, 100, 70, 0);
        long[] queue = new long[8192 + 32];
        long[] out = new long[8192];
        LongSet visited = new LongSet();
        RegressionTests.checkEquals(17, count(w, 0, 70, 0, 4096, 16, 6, queue, visited, out),
                "depth=16 axial from the tunnel end: root + 16 cells");
        // Root in the middle: reaches both directions.
        Set<Long> both = tunnel(-100, 100, 70, 0);
        RegressionTests.checkEquals(33, count(both, 0, 70, 0, 4096, 16, 6, queue, visited, out),
                "depth=16 axial mid-tunnel: -16..+16");
        // A cell whose Manhattan distance from the root is exactly 17 is gated out even when
        // a legal path exists (the depth gate is root-relative, not path-relative).
        Set<Long> reach17 = new HashSet<>();
        for (int x = 0; x <= 17; x++) reach17.add(PackedPos.pack(x, 70, 0));
        RegressionTests.checkEquals(17, count(reach17, 0, 70, 0, 64, 16, 6, queue, visited, out),
                "x=17 sits at depth 17 and must be excluded");
    }

    /**
     * Dense 3D blob with max far below the population: the found+pending<=max invariant
     * must hold, the result must cap exactly at max, and head/tail must survive wrapping the
     * ring many times without corruption or an ArrayIndexOutOfBounds.
     */
    private static void capAndRingWrap() {
        Set<Long> blob = new HashSet<>();
        int r = 24; // ~56k cells
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    if (Math.abs(x) + Math.abs(y) + Math.abs(z) <= r) blob.add(PackedPos.pack(x, 70 + y, z));
                }
            }
        }
        final int max = 256;
        long[] queue = new long[max + 32];
        long[] out = new long[max];
        LongSet visited = new LongSet();
        int found = count(blob, 0, 70, 0, max, 256, 6, queue, visited, out);
        RegressionTests.checkEquals(max, found, "capped scan fills exactly max cells");
        // Every returned cell is in the blob and within depth; roots are never duplicated.
        Set<Long> seen = new HashSet<>();
        for (int i = 0; i < found; i++) {
            RegressionTests.check(seen.add(out[i]), "duplicate cell in output at " + i);
            RegressionTests.check(blob.contains(out[i]), "output cell must be part of the world set");
            int x = PackedPos.unpackX(out[i]), y = PackedPos.unpackY(out[i]) - 70, z = PackedPos.unpackZ(out[i]);
            RegressionTests.check(Math.abs(x) + Math.abs(y) + Math.abs(z) <= 256, "depth respected");
        }
    }

    /** Diagonal-only connectivity: corner-adjacent cell shows up in 26-mode, never in 6-mode. */
    private static void diagonalModes() {
        Set<Long> w = new HashSet<>();
        w.add(PackedPos.pack(0, 70, 0));
        w.add(PackedPos.pack(1, 71, 1)); // diagonal corner neighbour of the root only
        long[] queue = new long[8192 + 32];
        long[] out = new long[8192];
        LongSet visited = new LongSet();
        RegressionTests.checkEquals(1, count(w, 0, 70, 0, 64, 8, 6, queue, visited, out),
                "axial mode must not cross the corner gap");
        RegressionTests.checkEquals(2, count(w, 0, 70, 0, 64, 8, FloodFillCore.NX.length, queue, visited, out),
                "diagonal mode must find the corner neighbour");
        // The two offsets the r15 table silently lacked: a true corner and a z=-1 x-face
        // diagonal. Corner (1,71,1) above covers the first; cover the second here.
        Set<Long> xdiag = new HashSet<>();
        xdiag.add(PackedPos.pack(0, 70, 0));
        xdiag.add(PackedPos.pack(1, 70, -1)); // offset (1,0,-1) - missing in r15's table
        RegressionTests.checkEquals(2, count(xdiag, 0, 70, 0, 64, 8, FloodFillCore.NX.length, queue, visited, out),
                "diagonal mode must cross the (1,0,-1) face diagonal (missing in the r15 table)");
        // A diagonal chain through a corner must keep going (BFS expansion, not just probes).
        Set<Long> stair = new HashSet<>();
        stair.add(PackedPos.pack(0, 70, 0));
        stair.add(PackedPos.pack(1, 71, 1));
        stair.add(PackedPos.pack(2, 72, 2));
        RegressionTests.checkEquals(3, count(stair, 0, 70, 0, 64, 8, FloodFillCore.NX.length, queue, visited, out),
                "corner staircase must be followed end to end in diagonal mode");
    }

    private static void degenerateInputs() {
        Set<Long> w = tunnel(0, 10, 70, 0);
        long[] queue = new long[8192 + 32];
        long[] out = new long[8192];
        LongSet visited = new LongSet();
        RegressionTests.checkEquals(0, count(w, 0, 70, 0, 0, 8, 6, queue, visited, out),
                "max <= 0 returns nothing");
        RegressionTests.checkEquals(1, count(w, 0, 70, 0, 1, 8, 6, queue, visited, out),
                "max == 1 returns the root only");
        RegressionTests.checkEquals(1, count(new HashSet<>(), 0, 70, 0, 64, 8, 6, queue, visited, out),
                "empty world still returns the (caller-validated) root");
        RegressionTests.checkEquals(11, count(w, 0, 70, 0, 64, 64, 6, queue, visited, out),
                "short tunnel returns every cell it connects, no more");
    }
}
