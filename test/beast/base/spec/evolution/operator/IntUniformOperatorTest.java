package beast.base.spec.evolution.operator;

import beast.base.inference.State;
import beast.base.spec.domain.Int;
import beast.base.spec.inference.distribution.IntUniform;
import beast.base.spec.inference.operator.uniform.IntUniformOperator;
import beast.base.spec.inference.parameter.IntScalarParam;
import beast.base.spec.inference.parameter.IntVectorParam;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class IntUniformOperatorTest {

    /**
     * IntUniformOperator's move includes bounds
     */
    @Test
    public void testIntScalarBound() {
        try {
            IntScalarParam<Int> parameter = new IntScalarParam<>(1, Int.INSTANCE);
            // set bounds
            IntUniform uniform = new IntUniform(parameter,
                    new IntScalarParam<>(0, Int.INSTANCE),
                    new IntScalarParam<>(3, Int.INSTANCE));

            // check bounds
            assertEquals(0, parameter.getLower(), 1e-10);
            assertEquals(3, parameter.getUpper(), 1e-10);

            State state = new State();
            state.initByName("stateNode", parameter);
            state.initialise();

            IntUniformOperator intUniformOperator = new IntUniformOperator();
            intUniformOperator.initByName("parameter", parameter, "weight", 1.0);

            System.out.println("IntUniformOperator on IntScalarParam : value includes bounds, " +
                    "where lower = 0 and upper = 3 ");
            int[] count = new int[4]; // 4 values {0, 1, 2, 3}
            for (int i = 0; i < 400; i++) {
                intUniformOperator.proposal();
                int value = parameter.get();

                assertTrue(value >= 0 && value <= 3);

                count[value] += 1;
            }
            System.out.println("count = " + Arrays.toString(count));
            assertTrue((count[0] > 0) && (count[1] > 0) && (count[2] > 0) && (count[3] > 0),
                    "IntUniformOperator on IntScalarParam, expecting count[0-3] > 0");

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }


    /**
     * IntUniformOperator's move includes bounds
     */
    @Test
    public void testIntegerVectorBound() {
    	try {

	        IntVectorParam<Int> parameter = new IntVectorParam<>(new int[]{1, 0, 3}, Int.INSTANCE);

            //TODO
            parameter.setLower(0);
	        parameter.setUpper(3);

    		State state = new State();
    		state.initByName("stateNode", parameter);
    		state.initialise();

            IntUniformOperator intUniformOperator = new IntUniformOperator();
	        intUniformOperator.initByName("parameter", parameter, "howMany", 3, "weight", 1.0);

            int[][] count = new int[parameter.size()][4]; // 4 values {0, 1, 2, 3}
	        for (int i = 0; i < 400; i++) {
	            intUniformOperator.proposal();
	            int [] values = parameter.getValues();
	            for (int k = 0; k < values.length; k++) {
	                int j = values[k];
	                count[k][j] += 1; 
	            }
	        }

	        System.out.println("Integer distributions lower = 0, upper = 3");
	        for (int j = 0; j < count.length; j++) {
		        System.out.println("x[" +j + "] = " + Arrays.toString(count[j]));
			}
	
	        assertTrue(count[0][0] > 0 && count[0][1] > 0 && count[0][2] > 0 && count[0][3] > 0, "Expected count[0][0-3] > 0");
	        assertTrue(count[1][0] > 0 && count[1][1] > 0 && count[1][2] > 0 && count[1][3] > 0, "Expected count[1][0-3] > 0");
	        assertTrue(count[2][0] > 0 && count[2][1] > 0 && count[2][2] > 0 && count[2][3] > 0, "Expected count[2][0-3] > 0");
    	} catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

}
