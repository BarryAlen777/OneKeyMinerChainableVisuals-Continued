package cn.blockforge.generated.modad7bbe58.client;

/**
 * Plain-JVM regression test runner (r15 audit report issue 3).
 *
 * <p>These tests deliberately use no test framework and no Minecraft classes: the build
 * container resolves dependencies offline, so a JUnit dependency could not be downloaded,
 * and the whole point of {@link FloodFillCore} / {@link PackedPos} / {@link LongSet} being
 * Minecraft-free is that a bare {@code java} invocation runs the exact shipped code paths.
 * The Gradle task {@code runRegressionTests} wires this into {@code check} (runs on every
 * {@code build}); it can also be run standalone. Failing a check aborts the build.</p>
 */
public final class RegressionTests {

    private RegressionTests() { }

    public static void main(String[] args) {
        run("PackedPos", PackedPosTest::run);
        run("LongSet", LongSetTest::run);
        run("FloodFillCore", FloodFillCoreTest::run);
        run("AnchorGate", AnchorGateTest::run);
        run("OutlineRuns", OutlineRunsTest::run);
        System.out.println("[RegressionTests] all suites passed");
    }

    private static void run(String name, Runnable suite) {
        System.out.print("[RegressionTests] " + name + " ... ");
        suite.run();
        System.out.println("ok");
    }

    static void check(boolean condition, String what) {
        if (!condition) throw new AssertionError(what);
    }

    static void checkEquals(long expected, long actual, String what) {
        if (expected != actual) {
            throw new AssertionError(what + " (expected " + expected + ", got " + actual + ")");
        }
    }
}
