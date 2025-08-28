package beast.base.inference.parameter;


import java.io.PrintStream;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.type.Tensor;
import beast.base.type.domain.Domain.DomainType;
import beast.base.type.domain.Real;


/**
 * @author Alexei Drummond
 */

@Description("A real-valued parameter represents a value (or array of values if the dimension is larger than one) " +
        "in the state space that can be changed by operators.")
public class RealParameter extends Parameter.Base<Double> implements Tensor {
    final public Input<Double> lowerValueInput = new Input<>("lower", "lower value for this parameter (default -infinity)");
    final public Input<Double> upperValueInput = new Input<>("upper", "upper value for this parameter (default +infinity)");

    final public Input<TensorType> tensorTypeInput = new Input<>("tensor", "type of tensor, determines what dimensions are possible", TensorType.Scalar, TensorType.values());
    final public Input<DomainType> domainInput = new Input<>("domain", "type of the domain (default DomainType.Real)", DomainType.Real, DomainType.values());
    
    
    public RealParameter() {
    }

    public RealParameter(final Double[] values) {
        super(values);
    }

    /**
     * Constructor used by Input.setValue(String) *
     */
    public RealParameter(final String value) {
        init(0.0, 0.0, TensorType.Scalar, DomainType.Real, value, 1);
    }

    @Override
    public void initAndValidate() {
        if (lowerValueInput.get() != null) {
            m_fLower = lowerValueInput.get();
        } else {
            m_fLower = Double.NEGATIVE_INFINITY;
        }
        if (upperValueInput.get() != null) {
            m_fUpper = upperValueInput.get();
        } else {
            m_fUpper = Double.POSITIVE_INFINITY;
        }
        super.initAndValidate();
    }

    @Override
    Double getMax() {
        return Double.POSITIVE_INFINITY;
    }

    @Override
    Double getMin() {
        return Double.NEGATIVE_INFINITY;
    }
    /** Valuable implementation follows **/

    /**
     * RRB: we need this here, because the base implementation (public T getValue()) fails
     * for some reason. Why?
     */
    @Override
    public Double getValue() {
        return values[0];
    }

    @Override
    public double getArrayValue() {
        return values[0];
    }

    @Override
    public double getArrayValue(final int index) {
        return values[index];
    }

    /**
     * Loggable implementation *
     */
    @Override
    public void log(final long sample, final PrintStream out) {
        final RealParameter var = (RealParameter) getCurrent();
        final int values = var.getDimension();
        for (int value = 0; value < values; value++) {
            out.print(var.getValue(value) + "\t");
        }
    }

    /**
     * StateNode methods *
     */
    @Override
    public int scale(final double scale) {
        int nScaled = 0;

        for (int i = 0; i < values.length; i++) {
            if (values[i] == 0.0)
                continue;

            values[i] *= scale;
            nScaled += 1;

            if (values[i] < m_fLower || values[i] > m_fUpper) {
                throw new IllegalArgumentException("parameter scaled out of range");
            }
        }

        return nScaled;
    }


    @Override
    void fromXML(final int dimension, final String lower, final String upper, final String[] valuesString) {
        setLower(Double.parseDouble(lower));
        setUpper(Double.parseDouble(upper));
        values = new Double[dimension];
        for (int i = 0; i < valuesString.length; i++) {
            values[i] = Double.parseDouble(valuesString[i]);
        }
    }

	@Override
	public int rank() {
		return 1;
	}

	@Override
	public int[] shape() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Double get(int... idx) {
		switch (idx.length) {
		case 0:
			return values[0];
		case 1:
			return values[idx[0]];
		case 2:
			return values[idx[0] * minorDimension + idx[1]];
		default:
			throw new IllegalArgumentException();
		}
	}

	@Override
	public double getDoubleValue(int... idx) {
		switch (idx.length) {
		case 0:
			return values[0];
		case 1:
			return values[idx[0]];
		case 2:
			return values[idx[0] * minorDimension + idx[1]];
		default:
			throw new IllegalArgumentException();
		}
	}

	@Override
	public boolean isValid() {
		// TODO Auto-generated method stub
		return false;
	}


}


