package cn.blockforge.generated.modad7bbe58.client;

import java.util.Arrays;
import java.util.function.LongPredicate;

/** Packs grid edges so a sort puts unit edges along the same line next to each other. */
final class OutlineRuns {
    static final int BITS = 20;
    static final int MASK = (1 << BITS) - 1;
    private static final int AXIS_SHIFT = 3 * BITS;

    private OutlineRuns() { }

    static long edge(int axis, int x, int y, int z) {
        return ((long) axis << AXIS_SHIFT) | switch (axis) {
            case 0 -> coordinates(y, z, x);
            case 1 -> coordinates(x, z, y);
            default -> coordinates(x, y, z);
        };
    }

    static int axis(long edge) { return (int) (edge >>> AXIS_SHIFT); }
    static int along(long edge) { return (int) (edge & MASK); }
    private static int first(long edge) { return (int) ((edge >>> (BITS * 2)) & MASK); }
    private static int second(long edge) { return (int) ((edge >>> BITS) & MASK); }
    static int x(long edge) { return axis(edge) == 0 ? along(edge) : first(edge); }
    static int y(long edge) {
        return switch (axis(edge)) {
            case 0 -> first(edge);
            case 1 -> along(edge);
            default -> second(edge);
        };
    }
    static int z(long edge) { return axis(edge) == 2 ? along(edge) : second(edge); }

    private static long coordinates(int first, int second, int along) {
        return ((long) first << (BITS * 2)) | ((long) second << BITS) | along;
    }

    static float[] collect(long[] edges, int count, LongPredicate keep) {
        Arrays.sort(edges, 0, count);
        float[] result = new float[64];
        int written = 0;
        long start = -1, end = -1, previous = -1;
        for (int i = 0; i < count; i++) {
            long edge = edges[i];
            if (edge == previous) continue;
            previous = edge;
            if (!keep.test(edge)) continue;
            if (start >= 0 && ((edge ^ end) >>> BITS) == 0 && edge == end + 1) {
                end = edge;
                continue;
            }
            if (start >= 0) {
                if (written + 6 > result.length) result = Arrays.copyOf(result, result.length * 2);
                written = append(result, written, start, end);
            }
            start = end = edge;
        }
        if (start >= 0) {
            if (written + 6 > result.length) result = Arrays.copyOf(result, result.length * 2);
            written = append(result, written, start, end);
        }
        return Arrays.copyOf(result, written);
    }

    private static int append(float[] result, int index, long start, long end) {
        int axis = axis(start);
        result[index++] = x(start);
        result[index++] = y(start);
        result[index++] = z(start);
        result[index++] = x(end) + (axis == 0 ? 1 : 0);
        result[index++] = y(end) + (axis == 1 ? 1 : 0);
        result[index++] = z(end) + (axis == 2 ? 1 : 0);
        return index;
    }
}
