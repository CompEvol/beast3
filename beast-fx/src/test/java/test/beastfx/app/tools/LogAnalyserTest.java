package test.beastfx.app.tools;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

import beastfx.app.tools.LogAnalyser;

public class LogAnalyserTest {

    /** writes a trace log with the given rows under the header Sample, x, y, z */
    private static File log(String... rows) throws IOException {
        File file = File.createTempFile("LogAnalyserTest", ".log");
        file.deleteOnExit();
        StringBuilder b = new StringBuilder("Sample\tx\ty\tz\n");
        for (String row : rows) {
            b.append(row).append('\n');
        }
        Files.writeString(file.toPath(), b.toString());
        return file;
    }

    /**
     * Boolean columns must read as true = 1 and false = 0, whichever value appears first.
     */
    @Test
    void testBooleansAreOneAndZero() throws IOException {
        LogAnalyser analyser = new LogAnalyser(log(
                "0\ttrue\tfalse\tA",
                "1\tfalse\ttrue\tB",
                "2\tfalse\ttrue\tA",
                "3\tfalse\ttrue\tA").getPath(), 0, true, true);

        // starts with true
        assertArrayEquals(new Double[] {1.0, 0.0, 0.0, 0.0}, analyser.getTrace("x"));
        assertEquals(0.25, analyser.getMean("x"), 1e-12);
        // starts with false
        assertArrayEquals(new Double[] {0.0, 1.0, 1.0, 1.0}, analyser.getTrace("y"));
        assertEquals(0.75, analyser.getMean("y"), 1e-12);
        // other nominal values are still numbered in order of first appearance
        assertArrayEquals(new Double[] {0.0, 1.0, 0.0, 0.0}, analyser.getTrace("z"));
    }

    /** the same holds when burn-in removes the first rows */
    @Test
    void testBooleansAfterBurnIn() throws IOException {
        LogAnalyser analyser = new LogAnalyser(log(
                "0\tfalse\tfalse\tA",
                "1\ttrue\tfalse\tA",
                "2\tfalse\ttrue\tA",
                "3\ttrue\ttrue\tA",
                "4\ttrue\ttrue\tA",
                "5\tfalse\ttrue\tA",
                "6\ttrue\ttrue\tA",
                "7\ttrue\ttrue\tA",
                "8\ttrue\ttrue\tA",
                "9\ttrue\ttrue\tA").getPath(), 10, true, true);

        // burn-in 10% drops row 0; x now starts with true
        assertArrayEquals(new Double[] {1.0, 0.0, 1.0, 1.0, 0.0, 1.0, 1.0, 1.0, 1.0}, analyser.getTrace("x"));
        // y: one false, then eight true
        assertEquals(8.0 / 9.0, analyser.getMean("y"), 1e-12);
    }
}
