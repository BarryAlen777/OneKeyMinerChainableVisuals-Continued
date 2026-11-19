package cn.blockforge.generated.modad7bbe58.client;

/** Matches preview snapshots to the tick that published them, not to a later moving cursor. */
final class AnchorGate {
    enum Action { SAME, CLEAR, ANCHOR }

    static final int ANY_ORIENTATION = -1;

    private Object beforeTick;
    private boolean armed;
    private boolean anchored;
    private long origin;
    private int orientation;
    private final LongSet covered = new LongSet();

    /** Sample before OneKeyMiner's normal-priority client tick. */
    void begin(Object list) {
        beforeTick = list;
        armed = list != null;
    }

    /** Only a list published between the two samples can belong to this tick's aim. */
    Action finish(Object list, boolean empty) {
        boolean fresh = armed && list != beforeTick;
        beforeTick = null;
        armed = false;
        if (empty) {
            clear();
            return Action.CLEAR;
        }
        return fresh ? Action.ANCHOR : Action.SAME;
    }

    void anchor(long origin, int orientation, long[] keys, int count) {
        this.origin = origin;
        this.orientation = orientation;
        anchored = true;
        covered.reset(count + 1);
        for (int i = 0; i < count; i++) covered.add(keys[i]);
    }

    boolean covers(long aim, int orientation) {
        return anchored && this.orientation == orientation
                && (aim == origin || (orientation == ANY_ORIENTATION && covered.contains(aim)));
    }

    void clear() {
        if (anchored) covered.reset(16);
        anchored = false;
    }

    void reset() {
        beforeTick = null;
        armed = false;
        clear();
    }
}
