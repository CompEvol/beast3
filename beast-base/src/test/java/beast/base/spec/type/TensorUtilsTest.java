package beast.base.spec.type;

import beast.base.spec.domain.Domain;
import beast.base.spec.domain.Real;

import org.junit.Test;

import static org.junit.Assert.*;

import java.util.List;

/**
 * Test class for TensorUtils using RealScalar
 */
public class TensorUtilsTest {

    // Example RealScalar implementation for testing purposes
    private static class TestRealScalar implements RealScalar<Real> {
        private final double value;
        private final Real domain;

        public TestRealScalar(double value) {
            this.value = value;
            this.domain = Real.INSTANCE;
        }

        @Override
        public Real getDomain() {
            return domain;
        }

        @Override
        public double get() {
            return value;
        }
    }

    // Another example implementation for RealVector
    private static class TestRealVector implements Vector<Real, Double> {
        private final double[] values;

        public TestRealVector(double[] values) {
            this.values = values;
        }

        @Override
        public int size() {
            return values.length;
        }

		@Override
		public Double get(int... idx) {
            return values[idx[0]];
		}

		@Override
		public Real getDomain() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean isValid(Double value) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public List<Double> getElements() {
			// TODO Auto-generated method stub
			return null;
		}
    }

    @Test
    public void testValuesToObjectArrayWithScalar() {
        RealScalar<Real> scalar = new TestRealScalar(42.0);
        Object[] result = TensorUtils.valuesToObjectArray(scalar);

        assertEquals(1, result.length);
        assertEquals(42.0, result[0]);
    }

    @Test
    public void testValuesToObjectArrayWithVector() {
        Vector<Real, Double> vector = new TestRealVector(new double[]{1.0, 2.0, 3.0});
        Object[] result = TensorUtils.valuesToObjectArray(vector);

        assertEquals(3, result.length);
        assertArrayEquals(new Object[]{1.0, 2.0, 3.0}, result);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testValuesToObjectArrayWithUnsupportedTensor() {
        Tensor unsupportedTensor = new Tensor() {
            @Override
            public int size() {
                return 1;
            }

			@Override
			public Object get(int... idx) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Domain getDomain() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public int rank() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public int[] shape() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public boolean isValid(Object value) {
				// TODO Auto-generated method stub
				return false;
			}
        };

        TensorUtils.valuesToObjectArray(unsupportedTensor);
    }

    @Test
    public void testValuesToDoubleArrayWithScalar() {
        RealScalar<Real> scalar = new TestRealScalar(42.0);
        double[] result = TensorUtils.valuesToDoubleArray(scalar);

        assertEquals(1, result.length);
        assertEquals(42.0, result[0], 1e-10);
    }

    @Test
    public void testValuesToDoubleArrayWithVector() {
        Vector<Real, Double> vector = new TestRealVector(new double[]{1.0, 2.0, 3.0});
        double[] result = TensorUtils.valuesToDoubleArray(vector);

        assertEquals(3, result.length);
        assertArrayEquals(new double[]{1.0, 2.0, 3.0}, result, 1e-10);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testValuesToDoubleArrayWithUnsupportedTensor() {
        Tensor unsupportedTensor = new Tensor() {
            @Override
            public int size() {
                return 1;
            }

			@Override
			public Object get(int... idx) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Domain getDomain() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public int rank() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public int[] shape() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public boolean isValid(Object value) {
				// TODO Auto-generated method stub
				return false;
			}
        };

        TensorUtils.valuesToDoubleArray(unsupportedTensor);
    }
}