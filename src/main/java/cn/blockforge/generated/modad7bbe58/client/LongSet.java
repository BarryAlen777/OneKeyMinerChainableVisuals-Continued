package cn.blockforge.generated.modad7bbe58.client;

import java.util.Arrays;

/**
 * Tiny open-addressing long set shared by the flood fill's visited marks and
 * {@link MergedOutline}'s occupancy lookups. Primitives only: no per-cell objects, so a scan
 * touching hundreds of thousands of cells stays allocation-free.
 *
 * <p>Stores {@code key + 1} so slot 0 means empty without a fill pass. The one key whose
 * stored form collides with the empty marker ({@code key + 1 == 0}, i.e. {@code key = -1},
 * {@link #AIR_KEY} for outline probes and the packed cell (-1,-1,-1) for absolute scans) is
 * tracked in a dedicated flag so it can never be silently dropped.</p>
 *
 * <p>{@link #reset(int)} grows the backing table when the upcoming workload needs it and
 * shrinks it again once it is grossly oversized (8x) for the requested size, so one huge
 * scan no longer pins ~4 MB and re-zeroes it on every later small scan (r15 audit report
 * issue 5); a steady workload keeps its table and stays allocation-free.
 * {@link #addIfAbsent(long)} rehashes when the load factor passes 0.5, so callers cannot
 * overflow the table by under-estimating the expected size.</p>
 */
final class LongSet {
    /** The single key whose +1 form collides with the empty marker; tracked separately. */
    static final long AIR_KEY = -1L;

    private long[] table = new long[16];
    private int mask = table.length - 1;
    private int size;
    private boolean hasAirKey;

    void reset(int expectedSize) {
        int cap = 16;
        while (cap < expectedSize * 2) cap <<= 1;
        if (cap > table.length || table.length > cap * 8) {
            // Too small: reallocate up. Wildly too large: reallocate down - a fresh array
            // is zero-filled by the JVM, so this also skips the fill pass entirely.
            table = new long[cap];
        } else {
            Arrays.fill(table, 0L);
        }
        mask = table.length - 1;
        size = 0;
        hasAirKey = false;
    }

    /** Backing table length; exposed for the regression tests only. */
    int capacity() {
        return table.length;
    }

    void add(long key) {
        addIfAbsent(key);
    }

    /** @return true when the key was not present and is now stored */
    boolean addIfAbsent(long key) {
        if (key == AIR_KEY) {
            if (hasAirKey) return false;
            hasAirKey = true;
            size++;
            return true;
        }
        long stored = key + 1L;
        int i = mix(stored) & mask;
        while (table[i] != 0L) {
            if (table[i] == stored) return false;
            i = (i + 1) & mask;
        }
        table[i] = stored;
        if (++size * 2 >= table.length) rehash();
        return true;
    }

    boolean contains(long key) {
        if (key == AIR_KEY) return hasAirKey;
        long stored = key + 1L;
        int i = mix(stored) & mask;
        long slot;
        while ((slot = table[i]) != 0L) {
            if (slot == stored) return true;
            i = (i + 1) & mask;
        }
        return false;
    }

    private void rehash() {
        long[] old = table;
        table = new long[old.length << 1];
        mask = table.length - 1;
        for (long stored : old) {
            if (stored == 0L) continue;
            int i = mix(stored) & mask;
            while (table[i] != 0L) i = (i + 1) & mask;
            table[i] = stored;
        }
    }

    private static int mix(long value) {
        long x = value;
        x ^= x >>> 33;
        x *= 0xff51afd7ed558ccdL;
        x ^= x >>> 33;
        return (int) x;
    }
}
