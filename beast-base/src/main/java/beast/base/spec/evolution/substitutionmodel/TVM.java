package beast.base.spec.evolution.substitutionmodel;

import java.lang.reflect.InvocationTargetException;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.evolution.datatype.DataType;
import beast.base.evolution.datatype.Nucleotide;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.type.RealScalar;

/**
 * Transversion model (TVM) of nucleotide evolution with four independent
 * transversion rates and equal transition rates. A submodel of GTR.
 */
@Description("Transversion model of nucleotide evolution (variable transversion rates, equal transition rates)." +
        "Rates that are not specified are assumed to be 1.")
public class TVM extends BasicGeneralSubstitutionModel {

    // Transversion rates
    final public Input<RealScalar<PositiveReal>> rateACInput = new Input<>("rateAC", "substitution rate for A to C (default 1)");
    final public Input<RealScalar<PositiveReal>> rateATInput = new Input<>("rateAT", "substitution rate for A to T (default 1)");
    final public Input<RealScalar<PositiveReal>> rateCGInput = new Input<>("rateCG", "substitution rate for C to G (default 1)");
    final public Input<RealScalar<PositiveReal>> rateGTInput = new Input<>("rateGT", "substitution rate for G to T (default 1)");

    // Transition rates
    final public Input<RealScalar<PositiveReal>> rateTransitionsInput = new Input<>("rateTransitions", "substitution rate for A<->G and C<->T");

    RealScalar<PositiveReal> rateAC;
    RealScalar<PositiveReal> rateGT;
    RealScalar<PositiveReal> rateAT;
    RealScalar<PositiveReal> rateCG;
    RealScalar<PositiveReal> rateTransitions;

    @Override
    public void initAndValidate() {
        frequencies = frequenciesInput.get();
        updateMatrix = true;
        nrOfStates = frequencies.getFreqs().length;
        if (nrOfStates != 4) {
            throw new IllegalArgumentException("Frequencies has wrong size. Expected 4, but got " + nrOfStates);
        }

        try {
			eigenSystem = createEigenSystem();
		} catch (SecurityException | ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			throw new IllegalArgumentException(e.getMessage());
		}
        rateMatrix = new double[nrOfStates][nrOfStates];
        relativeRates = new double[nrOfStates * (nrOfStates - 1)];
        storedRelativeRates = new double[nrOfStates * (nrOfStates - 1)];

        rateAC = getParameter(rateACInput);
        rateAT = getParameter(rateATInput);
        rateCG = getParameter(rateCGInput);
        rateGT = getParameter(rateGTInput);

        rateTransitions = getParameter(rateTransitionsInput);
    }

    private RealScalar<PositiveReal> getParameter(Input<RealScalar<PositiveReal>> parameterInput) {
        if (parameterInput.get() != null) {
            return parameterInput.get();
        }
        return new RealScalarParam<PositiveReal>(1.0, PositiveReal.INSTANCE);
    }

    @Override
    public void setupRelativeRates() {
        relativeRates[0] = rateAC.get(); // A->C
        relativeRates[1] = rateTransitions.get(); // A->G
        relativeRates[2] = rateAT.get(); // A->T

        relativeRates[3] = rateAC.get(); // C->A
        relativeRates[4] = rateCG.get(); // C->G
        relativeRates[5] = rateTransitions.get(); // C->T

        relativeRates[6] = rateTransitions.get(); // G->A
        relativeRates[7] = rateCG.get(); // G->C
        relativeRates[8] = rateGT.get(); // G->T

        relativeRates[9] = rateAT.get(); // T->A
        relativeRates[10] = rateTransitions.get(); //T->C
        relativeRates[11] = rateGT.get(); //T->G
    }

    @Override
    public boolean canHandleDataType(DataType dataType) {
        return dataType instanceof Nucleotide;
    }
}
