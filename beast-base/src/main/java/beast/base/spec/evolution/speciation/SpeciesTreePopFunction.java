package beast.base.spec.evolution.speciation;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.evolution.alignment.TaxonSet;
import beast.base.evolution.tree.TreeDistribution;
import beast.base.inference.State;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.inference.parameter.RealVectorParam;
import beast.base.spec.type.RealVector;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Description("Species tree prior for *BEAST analysis")
public class SpeciesTreePopFunction extends TreeDistribution {

    protected enum TreePopSizeFunction {constant, linear, linear_with_constant_root}

    public final Input<TreePopSizeFunction> popFunctionInput = new Input<>("popFunction", "Population function. " +
            "This can be " + Arrays.toString(TreePopSizeFunction.values()) + " (default 'constant')", TreePopSizeFunction.constant, TreePopSizeFunction.values());

    public final Input<RealVector<PositiveReal>> popSizesBottomInput = new Input<>("bottomPopSize",
            "population size parameter for populations at the bottom of a branch. " +
            "For constant population function, this is the same at the top of the branch.", Validate.REQUIRED);
    public final Input<RealVector<PositiveReal>> popSizesTopInput = new Input<>("topPopSize",
            "population size parameter at the top of a branch. " +
            "Ignored for constant population function, but required for linear population function.");

    /**
     * m_taxonSet is used by GeneTreeForSpeciesTreeDistribution *
     */
    final public Input<TaxonSet> taxonSetInput = new Input<>("taxonset", "set of taxa mapping lineages to species", Validate.REQUIRED);


    TreePopSizeFunction popFunction;
    RealVector<PositiveReal> popSizesBottom;
    RealVector<PositiveReal> popSizesTop;

    @Override
    public void initAndValidate() {
        popFunction = popFunctionInput.get();
        popSizesBottom = popSizesBottomInput.get();
        popSizesTop = popSizesTopInput.get();

        // set up sizes of population functions
        final int speciesCount = treeInput.get().getLeafNodeCount();
        final int nodeCount = treeInput.get().getNodeCount();
        switch (popFunction) {
            case constant:
                ((RealVectorParam<PositiveReal>) popSizesBottom).setDimension(nodeCount);
                break;
            case linear:
                if (popSizesTop == null) {
                    throw new IllegalArgumentException("topPopSize must be specified");
                }
                ((RealVectorParam<PositiveReal>) popSizesBottom).setDimension(speciesCount);
                ((RealVectorParam<PositiveReal>) popSizesTop).setDimension(nodeCount);
                break;
            case linear_with_constant_root:
                if (popSizesTop == null) {
                    throw new IllegalArgumentException("topPopSize must be specified");
                }
                ((RealVectorParam<PositiveReal>) popSizesBottom).setDimension(speciesCount);
                ((RealVectorParam<PositiveReal>) popSizesTop).setDimension(nodeCount - 1);
                break;
        }
    }

    @Override
    public double calculateLogP() {
        logP = 0;
        return logP;
    }

    @Override
    protected boolean requiresRecalculation() {
        return true;
    }

    @Override
    public List<String> getArguments() {
        return null;
    }

    @Override
    public List<String> getConditions() {
        return null;
    }

    @Override
    public void sample(final State state, final Random random) {
    }
}
