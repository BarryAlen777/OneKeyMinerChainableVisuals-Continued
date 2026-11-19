package cn.blockforge.generated.modad7bbe58.client;

/**
 * Covers the {@link LongSet} contract: the AIR_KEY sentinel that used to be a silent no-op
 * bug, dedup, load-factor growth, and the r15 issue 5 shrink-on-reset behaviour.
 */
public final class LongSetTest {

    private LongSetTest() { }

    static void run() {
        airKey();
        dedupAndGrowth();
        resetShrinksOversizedTable();
        resetKeepsRightSizedTable();
    }

    private static void airKey() {
        LongSet set = new LongSet();
        set.reset(16);
        RegressionTests.check(!set.contains(LongSet.AIR_KEY), "AIR_KEY absent before add");
        RegressionTests.check(set.addIfAbsent(LongSet.AIR_KEY), "AIR_KEY first add reports new");
        RegressionTests.check(!set.addIfAbsent(LongSet.AIR_KEY), "AIR_KEY second add reports duplicate");
        RegressionTests.check(set.contains(LongSet.AIR_KEY), "AIR_KEY contained after add");
        // Key 0 (stored as +1) must live independently of the AIR sentinel flag.
        RegressionTests.check(set.addIfAbsent(0L), "key 0 must be storable");
        RegressionTests.check(set.contains(0L), "key 0 must be contained");
        RegressionTests.check(!set.addIfAbsent(0L), "key 0 repeat reports duplicate");
    }

    private static void dedupAndGrowth() {
        LongSet set = new LongSet();
        set.reset(16);
        // 100k keys from a far larger expected table than the initial one: forces rehashes.
        for (int i = 0; i < 100_000; i++) {
            long key = (long) i * 0x9E3779B97F4A7C15L; // scattered 64-bit values
            RegressionTests.check(set.addIfAbsent(key), "fresh key must report new (i=" + i + ")");
            RegressionTests.check(!set.addIfAbsent(key), "repeat key must report duplicate (i=" + i + ")");
        }
        RegressionTests.check(set.capacity() >= 100_000, "load factor 0.5 keeps table >= 2x size");
        for (int i = 0; i < 100_000; i++) {
            long key = (long) i * 0x9E3779B97F4A7C15L;
            RegressionTests.check(set.contains(key), "key must survive every rehash (i=" + i + ")");
        }
        RegressionTests.check(!set.contains(7L), "absent key must not report present");
    }

    private static void resetShrinksOversizedTable() {
        LongSet set = new LongSet();
        set.reset(16);
        for (int i = 0; i < 100_000; i++) set.add(i * 31L + 1);
        RegressionTests.check(set.capacity() > 1024, "table grew for the big workload");
        set.reset(16); // tiny scan follows the huge one...
        RegressionTests.checkEquals(32, set.capacity(), "oversized table must shrink back on reset (r15 issue 5)");
        for (int i = 0; i < 100_000; i++) {
            RegressionTests.check(!set.contains(i * 31L + 1), "reset must have cleared all keys");
        }
    }

    private static void resetKeepsRightSizedTable() {
        LongSet set = new LongSet();
        set.reset(16);
        for (int i = 0; i < 100_000; i++) set.add(i * 31L + 1);
        int grown = set.capacity();
        set.reset(60_000); // steady-state workload of comparable size keeps its table (no thrash)
        RegressionTests.checkEquals(grown, set.capacity(), "right-sized table must be reused, not reallocated");
        set.reset(200_000); // bigger ask grows it again
        RegressionTests.check(set.capacity() >= 400_000, "reset must grow to at least 2x the expected size");
    }
}
