package beast.base.spec.evolution.branchratemodel;

import beast.base.core.Input;
import beast.base.evolution.branchratemodel.BranchRateModel;
import beast.base.inference.CalculationNode;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.type.RealScalar;

/**
 * Abstract base for branch rate models in the spec type system.
 * Provides the shared {@code clock.rate} input for the mean clock rate.
 */
public abstract class Base extends CalculationNode implements BranchRateModel {
    final public Input<RealScalar<PositiveReal>> meanRateInput = new Input<>("clock.rate", "mean clock rate (defaults to 1.0)");

    // empty at the moment but brings together the required interfaces
}
