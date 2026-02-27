package beast.base.spec.evolution.likelihood;


import java.util.List;
import java.util.Random;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.evolution.alignment.Alignment;
import beast.base.spec.evolution.branchratemodel.Base;
import beast.base.evolution.sitemodel.SiteModelInterface;
import beast.base.evolution.tree.TreeInterface;
import beast.base.inference.Distribution;
import beast.base.inference.State;





/**
 * Base class for tree likelihood calculations.
 * Combines an alignment, a tree, a site model (with substitution model),
 * and an optional branch rate model to compute the likelihood of
 * sequence data given a phylogenetic tree.
 *
 * <p>Subclass this and override {@code calculateLogP()} to implement
 * non-standard tree likelihood computations.
 */
@Description("Generic tree likelihood for an alignment given a generic SiteModel, " +
		"a beast tree and a branch rate model")
public class GenericTreeLikelihood extends Distribution {
    
    final public Input<Alignment> dataInput = new Input<>("data", "sequence data for the beast.tree", Validate.REQUIRED);

    final public Input<TreeInterface> treeInput = new Input<>("tree", "phylogenetic beast.tree with sequence data in the leafs", Validate.REQUIRED);

    final public Input<SiteModelInterface> siteModelInput = new Input<>("siteModel", "site model for leafs in the beast.tree", Validate.REQUIRED);
    
    final public Input<Base> branchRateModelInput = new Input<>("branchRateModel",
            "A model describing the rates on the branches of the beast.tree.");

    
    
	@Override
	public List<String> getArguments() {return null;}

	@Override
	public List<String> getConditions() {return null;}

	@Override
	public void sample(State state, Random random) {}

}
