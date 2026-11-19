package cn.blockforge.generated.modad7bbe58.client;

/**
 * Block position <-> long packing for the flood-fill queue and the visited set.
 *
 * <p>Layout (64 bits total): {@code x} occupies bits 38..63 (26-bit signed), {@code y} bits
 * 26..37 (12-bit signed, vanilla height range), {@code z} bits 0..25 (26-bit signed). All
 * three ranges cover the build limit of ±30 000 000 horizontally and -2048..2047 vertically,
 * far beyond anything a client scan ever touches.</p>
 *
 * <p>The unpack formulas deliberately keep every shift in {@code long} arithmetic and only
 * cast to {@code int} at the very end. The r12 release wrote {@code (int) (p << 52) >> 52},
 * where the cast binds tighter than the shift: the value was truncated to 32 bits first and
 * int shifts are taken modulo 32, so {@code y}/{@code z} decoded to garbage and the whole
 * standalone mode showed nothing (audit report issue 1, P0). {@code PackedPosTest} round-trips
 * the boundary coordinates to keep that class of typo from shipping again.</p>
 *
 * <p>Kept free of any Minecraft classes on purpose: the unit test must run on a plain JVM.</p>
 */
final class PackedPos {

    private PackedPos() { }

    static long pack(int x, int y, int z) {
        return ((long) x & 0x3FFFFFFL) << 38 | ((long) y & 0xFFFL) << 26 | ((long) z & 0x3FFFFFFL);
    }

    /** x sits in the top 26 bits, so a plain arithmetic >> already sign-extends it. */
    static int unpackX(long p) {
        return (int) (p >> 38);
    }

    /** y lives in bits 26..37: shift its sign bit (37) to the top (<< 26), then read 12 bits. */
    static int unpackY(long p) {
        return (int) ((p << 26) >> 52);
    }

    /** z lives in bits 0..25: shift its sign bit (25) to the top (<< 38), then read 26 bits. */
    static int unpackZ(long p) {
        return (int) ((p << 38) >> 38);
    }
}
