package beast.base.spec.inference.operator.uniform;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.parameter.RealVectorParam;
import beast.base.util.Randomizer;

@Description("A scale operator that selects a random dimension of the real parameter and scales the value a " +
        "random amount according to a Bactrian distribution such that the parameter remains in its range. "
        + "Supposed to be more efficient than UniformOperator")
public class VectorIntervalOperator extends AbstractInterval {
     final public Input<RealVectorParam<? extends Real>> parameterInput = new Input<>("parameter",
              "the parameter to operate a random walk on.", Validate.REQUIRED);

    @Override
	public void initAndValidate() {
    	super.initAndValidate();
    }

    @Override
    double getPamaLower() {
        return parameterInput.get().getLower();
    }

    @Override
    double getPamaUpper() {
        return parameterInput.get().getUpper();
    }

    @Override
    public double proposal() {

        RealVectorParam<? extends Real> param = parameterInput.get();

        int i = Randomizer.nextInt(param.size());
        double value = param.get(i);
        double scale = getScaler(i, value);

        // transform value
        double y = (upper - value) / (value - lower);
        y *= scale;
        double newValue = (upper + lower * y) / (y + 1.0);

        if (newValue < lower || newValue > upper) {
        	throw new RuntimeException("programmer error: new value proposed outside range");
        }

        // Ensure that the value is not sitting on the limit (due to numerical issues for example)
        if (!inclusive && (newValue == lower || newValue == upper)) return Double.NEGATIVE_INFINITY;

        param.setValue(i, newValue);

        double logHR = Math.log(scale) + 2.0 * Math.log((newValue - lower)/(value - lower));
        return logHR;
    }

} // class VectorIntervalOperator