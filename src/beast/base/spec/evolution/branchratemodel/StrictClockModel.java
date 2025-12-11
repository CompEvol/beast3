package beast.base.spec.evolution.branchratemodel;

import beast.base.core.Description;
import beast.base.evolution.tree.Node;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.type.RealScalar;

/**
 * @author Alexei Drummond
 */

@Description("Defines a mean rate for each branch in the beast.tree.")
public class StrictClockModel extends Base {

    //public Input<RealParameter> muParameterInput = new Input<>("clock.rate", "the clock rate (defaults to 1.0)");

	RealScalar<PositiveReal> muParameter;

    @Override
    public void initAndValidate() {
        muParameter = meanRateInput.get();
        if (muParameter != null) {
//        	if (muParameter instanceof RealScalarParam<PositiveReal> mu) {
//        		mu.setBounds(Math.max(0.0, mu.getLower()), mu.getUpper());
//        	}
            mu = muParameter.get();
        }
    }

    @Override
    public double getRateForBranch(final Node node) {
        return mu;
    }

    @Override
    public boolean requiresRecalculation() {
        mu = muParameter.get();
        return true;
    }

    @Override
    protected void restore() {
        mu = muParameter.get();
        super.restore();
    }

    @Override
    protected void store() {
        mu = muParameter.get();
        super.store();
    }

    private double mu = 1.0;
}
