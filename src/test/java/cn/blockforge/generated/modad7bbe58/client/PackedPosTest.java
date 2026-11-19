package cn.blockforge.generated.modad7bbe58.client;

/**
 * Round-trips the packed-coordinate codec, including the r12 regression case that broke
 * standalone mode wholesale: the old {@code (int) (p << 52) >> 52} decoded y/z to garbage
 * because the cast bound tighter than the shift (see the class comment of {@link PackedPos}).
 */
public final class PackedPosTest {

    private PackedPosTest() { }

    static void run() {
        // The exact coordinates of the r12 report: y/z must survive the round trip.
        roundTrip(0, 70, 0);
        roundTrip(-1, 70, -1);
        roundTrip(100, 200, -300);
        roundTrip(-300, 200, 100);

        // Sign-bit positions of each field (x: bit 25 of the field is the sign, etc.).
        roundTrip(1, 1, 1);
        roundTrip(-1, -1, -1);
        roundTrip(1 << 24, 1 << 10, 1 << 24);
        roundTrip(-(1 << 24), -(1 << 10), -(1 << 24));

        // Field extremes: x/z are 26-bit signed, y is 12-bit signed.
        roundTrip((1 << 25) - 1, (1 << 11) - 1, (1 << 25) - 1);
        roundTrip(-(1 << 25), -(1 << 11), -(1 << 25));

        // Build-limit corners stay inside the ranges (x/z 26-bit signed, y 12-bit signed).
        roundTrip(33_554_431, 2047, 33_554_431);
        roundTrip(-33_554_432, -2048, -33_554_432);

        // The AIR_KEY collision case from LongSet's contract: pack(-1,-1,-1) == -1, the one
        // key whose +1 form is the empty marker - and it still decodes back correctly.
        RegressionTests.checkEquals(-1L, PackedPos.pack(-1, -1, -1), "pack(-1,-1,-1) must be the AIR_KEY long");
        roundTrip(-1, -1, -1);

        // Every neighbour offset used by the scan must round trip from the vein hot spots.
        for (int i = 0; i < FloodFillCore.NX.length; i++) {
            roundTrip(0 + FloodFillCore.NX[i], 70 + FloodFillCore.NY[i], 0 + FloodFillCore.NZ[i]);
            roundTrip(1_000_000 + FloodFillCore.NX[i], -64 + FloodFillCore.NY[i], -1_000_000 + FloodFillCore.NZ[i]);
        }
    }

    private static void roundTrip(int x, int y, int z) {
        long p = PackedPos.pack(x, y, z);
        RegressionTests.checkEquals(x, PackedPos.unpackX(p), "x round trip at (" + x + "," + y + "," + z + ")");
        RegressionTests.checkEquals(y, PackedPos.unpackY(p), "y round trip at (" + x + "," + y + "," + z + ")");
        RegressionTests.checkEquals(z, PackedPos.unpackZ(p), "z round trip at (" + x + "," + y + "," + z + ")");
    }
}
