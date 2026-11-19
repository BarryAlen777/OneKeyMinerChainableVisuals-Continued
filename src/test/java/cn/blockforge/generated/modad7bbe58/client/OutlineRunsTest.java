package cn.blockforge.generated.modad7bbe58.client;

import java.util.Arrays;

final class OutlineRunsTest {
    private OutlineRunsTest() { }

    static void run() {
        long rejected = OutlineRuns.edge(0, 6, 2, 3);
        long[] input = {
                OutlineRuns.edge(0, 8, 2, 3), OutlineRuns.edge(0, 5, 2, 3),
                OutlineRuns.edge(0, 7, 2, 3), OutlineRuns.edge(0, 5, 2, 3), rejected,
                OutlineRuns.edge(0, 9, 2, 4), OutlineRuns.edge(1, 4, 7, 6),
                OutlineRuns.edge(1, 4, 6, 6), OutlineRuns.edge(2, 4, 8, 0),
                OutlineRuns.edge(2, 4, 8, 1), OutlineRuns.edge(0, OutlineRuns.MASK, 10, 10),
                OutlineRuns.edge(0, 0, 10, 11)
        };
        float[] expected = {
                5, 2, 3, 6, 2, 3, 7, 2, 3, 9, 2, 3, 9, 2, 4, 10, 2, 4,
                OutlineRuns.MASK, 10, 10, OutlineRuns.MASK + 1, 10, 10,
                0, 10, 11, 1, 10, 11, 4, 6, 6, 4, 8, 6, 4, 8, 0, 4, 8, 2
        };
        RegressionTests.check(Arrays.equals(expected, OutlineRuns.collect(input, input.length, edge -> edge != rejected)),
                "join each axis without crossing gaps, neighboring grid lines or duplicate candidates");
        RegressionTests.check(OutlineRuns.collect(input, input.length, edge -> false).length == 0,
                "an outline with no retained edges is empty");
    }
}
