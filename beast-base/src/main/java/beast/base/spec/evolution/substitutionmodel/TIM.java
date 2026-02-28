package beast.base.spec.evolution.substitutionmodel;

import java.lang.reflect.InvocationTargetException;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.evolution.datatype.DataType;
import beast.base.evolution.datatype.Nucleotide;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.type.RealScalar;

/**
 * Transition model (TIM) of nucleotide evolution with two independent transition
 * rates and two transversion rates. A submodel of GTR.
 */
@Description("Transition model of nucleotide evolution (variable transition rates, two transversion rates). " +
        "Rates that are not specified are assumed to be 1.")
public class TIM extends BasicGeneralSubstitutionModel {

    // Transition rates
    final public Input<RealScalar<PositiveReal>> rateAGInput = new Input<>("rateAG", "substitution rate for A to G (default 1)");
    final public Input<RealScalar<PositiveReal>> rateCTInput = new Input<>("rateCT", "substitution rate for C to T (default 1)");

    // Transversion rates
    final public Input<RealScalar<PositiveReal>> rateTransversions1Input = new Input<>("rateTransversions1", "substitution rate for A<->C and G<->T");
    final public Input<RealScalar<PositiveReal>> rateTransversions2Input = new Input<>("rateTransversions2", "substitution rate for C<->G and A<->T");

    RealScalar<PositiveReal> rateAG;
    RealScalar<PositiveReal> rateCT;
    RealScalar<PositiveReal> rateTransversions1;
    RealScalar<PositiveReal> rateTransversions2;

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

        rateAG = getParameter(rateAGInput);
        rateCT = getParameter(rateCTInput);
        rateTransversions1 = getParameter(rateTransversions1Input);
        rateTransversions2 = getParameter(rateTransversions2Input);
    }

    private RealScalar<PositiveReal> getParameter(Input<RealScalar<PositiveReal>> parameterInput) {
        if (parameterInput.get() != null) {
            return parameterInput.get();
        }
        return new RealScalarParam<PositiveReal>(1.0, PositiveReal.INSTANCE);
    }

    @Override
    public void setupRelativeRates() {
        relativeRates[0] = rateTransversions1.get(); // A->C
        relativeRates[1] = rateAG.get(); // A->G
        relativeRates[2] = rateTransversions2.get(); // A->T

        relativeRates[3] = rateTransversions1.get(); // C->A
        relativeRates[4] = rateTransversions2.get(); // C->G
        relativeRates[5] = rateCT.get(); // C->T

        relativeRates[6] = rateAG.get(); // G->A
        relativeRates[7] = rateTransversions2.get(); // G->C
        relativeRates[8] = rateTransversions1.get(); // G->T

        relativeRates[9] = rateTransversions2.get(); // T->A
        relativeRates[10] = rateCT.get(); //T->C
        relativeRates[11] = rateTransversions1.get(); //T->G
    }

    @Override
    public boolean canHandleDataType(DataType dataType) {
        return dataType instanceof Nucleotide;
    }
}