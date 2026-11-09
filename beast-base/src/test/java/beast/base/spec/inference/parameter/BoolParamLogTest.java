package beast.base.spec.inference.parameter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.Test;

import beast.base.core.Loggable;

/**
 * Boolean parameters log as 1/0, not true/false, so that Tracer reads the columns as
 * numbers (beast3#221), as BEAST 2's BooleanParameter does.
 */
public class BoolParamLogTest {

    static String logged(Loggable loggable) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(bytes);
        loggable.log(0, out);
        out.flush();
        return bytes.toString();
    }

    @Test
    public void vectorLogsOneZero() {
        BoolVectorParam p = new BoolVectorParam(new boolean[]{true, false, false, true});
        assertEquals("1\t0\t0\t1\t", logged(p));
    }

    @Test
    public void scalarLogsOneZero() {
        assertEquals("1\t", logged(new BoolScalarParam(true)));
        assertEquals("0\t", logged(new BoolScalarParam(false)));
    }
}
