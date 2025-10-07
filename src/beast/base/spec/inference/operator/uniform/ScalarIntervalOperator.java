package beast.base.spec.inference.operator.uniform;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.parameter.RealScalarParam;

@Description("A scale operator that selects a random dimension of the real parameter and scales the value a " +
        "random amount according to a Bactrian distribution such that the parameter remains in its range. "
        + "Supposed to be more efficient than UniformOperator")
public class ScalarIntervalOperator extends AbstractInterval {

     final public Input<RealScalarParam<? extends Real>> parameterInput = new Input<>("parameter",
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

        RealScalarParam<? extends Real> param = parameterInput.get();
        double value = param.getValue();
        // use index 0 for scalar
        double scale = getScaler(0, value);

        // transform value
        double y = (upper - value) / (value - lower);
        y *= scale;
        double newValue = (upper + lower * y) / (y + 1.0);

        if (newValue < lower || newValue > upper) {
        	throw new RuntimeException("programmer error: new value proposed outside range");
        }

        // Ensure that the value is not sitting on the limit (due to numerical issues for example)
        if (!inclusive && (newValue == lower || newValue == upper)) return Double.NEGATIVE_INFINITY;

        param.setValue(newValue);

        double logHR = Math.log(scale) + 2.0 * Math.log((newValue - lower)/(value - lower));
        return logHR;
    }

} // class BactrianIntervalOperator