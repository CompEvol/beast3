package beast.base.spec.evolution.branchratemodel;

import beast.base.core.Citation;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Log;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.inference.parameter.BoolVectorParam;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.inference.parameter.RealVectorParam;
import beast.base.spec.type.BoolVector;
import beast.base.spec.type.RealScalar;
import beast.base.spec.type.RealVector;

/**
 * @author Alexei Drummond
 */
@Description("Random Local Clock Model.")
@Citation(value =
        "Drummond AJ, Suchard MA (2010) Bayesian random local clocks, or one rate to rule them all. BMC biology 8, 114.",
        DOI = "10.1186/1741-7007-8-114",
        year = 2010, firstAuthorSurname = "drummond")
public class RandomLocalClockModel extends Base {

    final public Input<BoolVector> indicatorParamInput =
            new Input<>("indicators",
                    "the indicators associated with nodes in the tree for sampling of individual rate changes among branches.",
                    Input.Validate.REQUIRED);
    final public Input<RealVector<PositiveReal>> rateParamInput =
            new Input<>("rates",
                    "the rate parameters associated with nodes in the tree for sampling of individual rates among branches.",
                    Input.Validate.REQUIRED);

    final public Input<Tree> treeInput =
            new Input<>("tree", "the tree this relaxed clock is associated with.", Input.Validate.REQUIRED);

    final public Input<Boolean> ratesAreMultipliersInput =
            new Input<>("ratesAreMultipliers", "true if the rates should be treated as multipliers (default false).",
                    false);

    final public Input<Boolean> scalingInput =
            new Input<>("scaling", "if false, then ignore meanRate input and leave rates unscaled.",
                    true, Input.Validate.OPTIONAL);

    final public Input<Boolean> includeRootInput =
            new Input<>("includeRoot", "if true, then the root can take on an arbitrary rate, otherwise the root branch has rate 1.0.",
                    false, Input.Validate.OPTIONAL);

    Tree tree;
    RealScalar<PositiveReal> meanRate;
    boolean scaling = true;

    @Override
    public void initAndValidate() {
        tree = treeInput.get();

        scaling = scalingInput.get();

        BoolVector indicators = indicatorParamInput.get();

        int rateSize = tree.getNodeCount();
        int indicatorSize = tree.getNodeCount() - 1;

        if (!includeRootInput.get()) rateSize -= 1;

        if (indicators.size() != indicatorSize) {
        	if (indicators instanceof BoolVectorParam indicatorsParam) {
        		Log.warning("RandomLocalClockModel::Setting dimension of indicators to " + indicatorSize);
        		indicatorsParam.setDimension(indicatorSize);
        	} else {
        		throw new IllegalArgumentException("Indicators has dimension " + indicators.size() + " but expected " + indicatorSize);
        	}
        }

        unscaledBranchRates = new double[tree.getNodeCount()];

        RealVector<PositiveReal> rates = rateParamInput.get();
        if (rates instanceof RealVectorParam<PositiveReal> ratesParam) {
            // setBounds are deprecated, and PositiveReal provides this domain range
//	        if (ratesParam.lowerValueInput.get() == null || ratesParam.lowerValueInput.get() < 0.0) {
//	        	ratesParam.setLower(0.0);
//	        }
//	        if (ratesParam.upperValueInput.get() == null || ratesParam.upperValueInput.get() < 0.0) {
//	        	ratesParam.setUpper(Double.MAX_VALUE);
//	        }
	        if (ratesParam.size() != rateSize) {
	        	Log.warning("RandomLocalClockModel::Setting dimension of rates to " + rateSize);
	        	ratesParam.setDimension(rateSize);
	        }
        }

        ratesAreMultipliers = ratesAreMultipliersInput.get();

        meanRate = meanRateInput.get();
        if (meanRate == null) {
            meanRate = new RealScalarParam<PositiveReal>(1.0, PositiveReal.INSTANCE);
        }
    }

    /**
     * This is a recursive function that does the work of
     * calculating the unscaled branch rates across the tree
     * taking into account the indicator variables.
     *
     * @param node the node
     * @param rate the rate of the parent node
     */
    private void calculateUnscaledBranchRates(Node node, double rate, BoolVector indicators, RealVector<PositiveReal> rates) {

        int nodeNumber = getNr(node);

        if (!node.isRoot()) {
            if (indicators.get(nodeNumber)) {
                if (ratesAreMultipliers) {
                    rate *= rates.get(nodeNumber);
                } else {
                    rate = rates.get(nodeNumber);
                }
            }
        }
        unscaledBranchRates[nodeNumber] = rate;

        if (!node.isLeaf()) {
            calculateUnscaledBranchRates(node.getLeft(), rate, indicators, rates);
            calculateUnscaledBranchRates(node.getRight(), rate, indicators, rates);
        }
    }

    private void recalculateScaleFactor() {
    	BoolVector indicators = indicatorParamInput.get();
        RealVector<PositiveReal> rates = rateParamInput.get();

        double rootRate = 1.0;
        if (includeRootInput.get()) rootRate = rates.get(tree.getRoot().getNr());

        calculateUnscaledBranchRates(tree.getRoot(), rootRate, indicators, rates);

        if (scaling) {

            double timeTotal = 0.0;
            double branchTotal = 0.0;

            for (int i = 0; i < tree.getNodeCount(); i++) {
                Node node = tree.getNode(i);
                if (!node.isRoot()) {

                    double branchInTime = node.getParent().getHeight() - node.getHeight();

                    double branchLength = branchInTime * unscaledBranchRates[node.getNr()];

                    timeTotal += branchInTime;
                    branchTotal += branchLength;
                }
            }

            scaleFactor = timeTotal / branchTotal;

            scaleFactor *= meanRate.get();
        } else {
            scaleFactor = 1.0;
        }
    }

    @Override
    public double getRateForBranch(Node node) {
        // this must be synchronized to avoid being called simultaneously by
        // two different likelihood threads
    	synchronized (this) {
    		if (recompute) {
                recalculateScaleFactor();
                recompute = false;
			}
        }

        return unscaledBranchRates[getNr(node)] * scaleFactor;
    }

    private int getNr(Node node) {
        int nodeNr = node.getNr();
        if (nodeNr > tree.getRoot().getNr()) {
            nodeNr--;
        }
        return nodeNr;
    }

    @Override
    protected boolean requiresRecalculation() {
        // this is only called if any of its inputs is dirty, hence we need to recompute
        recompute = true;
        return true;
    }

    @Override
    protected void store() {
        recompute = true;
        super.store();
    }

    @Override
    protected void restore() {
        recompute = true;
        super.restore();
    }

    private boolean recompute = true;
    double[] unscaledBranchRates;
    double scaleFactor = 1.0;
    boolean ratesAreMultipliers = false;
}
