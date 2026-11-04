package beast.base.spec.inference.parameter;

import beast.base.spec.domain.Int;
import beast.base.spec.domain.NonNegativeInt;
import beast.base.spec.inference.distribution.IID;
import beast.base.spec.inference.distribution.IntUniform;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bounds of {@link IntVectorParam}: the effective bound is the tighter of the bound set by
 * setLower/setUpper and the domain bound as tightened by attached priors.
 */
public class IntVectorParamTest {

    private static IntUniform intUniform(int lower, int upper) {
        return new IntUniform(null,
                new IntScalarParam<>(lower, Int.INSTANCE),
                new IntScalarParam<>(upper, Int.INSTANCE));
    }

    @Test
    void testDomainBound() {
        IntVectorParam<NonNegativeInt> param = new IntVectorParam<>(new int[]{0, 0}, NonNegativeInt.INSTANCE);
        // without setLower, the lower bound comes from the domain (it used to be Integer.MIN_VALUE + 1)
        assertEquals(0, param.getLower());
        assertFalse(param.isValid(-1));
        assertTrue(param.isValid(0));
    }

    @Test
    void testExplicitBounds() {
        IntVectorParam<NonNegativeInt> param = new IntVectorParam<>(new int[]{0, 0}, NonNegativeInt.INSTANCE);
        param.setLower(2);
        param.setUpper(6);
        assertEquals(2, param.getLower());
        assertEquals(6, param.getUpper());
    }

    @Test
    void testPriorBounds() {
        IntVectorParam<Int> param = new IntVectorParam<>(new int[]{3, 3}, Int.INSTANCE);
        param.setID("param");
        IntUniform uniform = intUniform(1, 5);
        IID iid = new IID(param, uniform);
        iid.setID("iid");

        assertEquals(uniform.getLowerBoundOfParameter(), param.getLower());
        assertEquals(uniform.getUpperBoundOfParameter(), param.getUpper());
        assertEquals(1, param.getLower());
    }

    @Test
    void testTighterOfExplicitAndPrior() {
        IntVectorParam<Int> param = new IntVectorParam<>(new int[]{3, 3}, Int.INSTANCE);
        param.setID("param");
        IID iid = new IID(param, intUniform(1, 5));
        iid.setID("iid");

        // explicit bounds looser than the prior: the prior wins
        param.setLower(-10);
        param.setUpper(10);
        assertEquals(1, param.getLower());
        assertTrue(param.getUpper() <= 5);

        // explicit bounds tighter than the prior: the explicit bounds win
        param.setLower(2);
        param.setUpper(4);
        assertEquals(2, param.getLower());
        assertEquals(4, param.getUpper());
    }
}
