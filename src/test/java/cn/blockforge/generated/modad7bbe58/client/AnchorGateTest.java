package cn.blockforge.generated.modad7bbe58.client;

final class AnchorGateTest {
    private static final long A = PackedPos.pack(10, 64, 10);
    private static final long B = PackedPos.pack(11, 64, 10);
    private static final long OTHER = PackedPos.pack(40, 70, 40);

    private AnchorGateTest() { }

    static void run() {
        AnchorGate gate = new AnchorGate();
        Object oldList = new Object();
        Object newList = new Object();
        gate.begin(oldList);
        RegressionTests.check(gate.finish(oldList, false) == AnchorGate.Action.SAME,
                "an old list must not be attached to a new crosshair target");
        gate.begin(oldList);
        RegressionTests.check(gate.finish(newList, false) == AnchorGate.Action.ANCHOR,
                "a snapshot from this tick is accepted immediately, without settle ticks");
        gate.anchor(A, AnchorGate.ANY_ORIENTATION, new long[]{B}, 1);
        RegressionTests.check(gate.covers(A, AnchorGate.ANY_ORIENTATION)
                        && gate.covers(B, AnchorGate.ANY_ORIENTATION),
                "an amorphous group stays visible while aiming inside it");
        RegressionTests.check(!gate.covers(OTHER, AnchorGate.ANY_ORIENTATION),
                "aiming at another group hides the previous group before the next tick");
        gate.begin(newList);
        RegressionTests.check(gate.finish(newList, false) == AnchorGate.Action.SAME,
                "aiming elsewhere never reuses the snapshot as a new group");
        gate.anchor(A, 4, new long[]{B}, 1);
        RegressionTests.check(gate.covers(A, 4) && !gate.covers(A, 5) && !gate.covers(B, 4),
                "directional and unknown shapes require both their own origin and direction");
        RegressionTests.check(gate.finish(new Object(), true) == AnchorGate.Action.CLEAR && !gate.covers(A, 4),
                "an empty report clears old geometry immediately");
        gate.begin(oldList);
        gate.reset();
        RegressionTests.check(gate.finish(newList, false) == AnchorGate.Action.SAME,
                "a world or mode change invalidates the earlier sample");
        gate.begin(null);
        RegressionTests.check(gate.finish(newList, false) == AnchorGate.Action.SAME,
                "a failed first read cannot certify an older snapshot");
    }
}
