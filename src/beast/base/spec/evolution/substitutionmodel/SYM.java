package beast.base.spec.evolution.substitutionmodel;

import java.lang.reflect.InvocationTargetException;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.evolution.datatype.DataType;
import beast.base.evolution.datatype.Nucleotide;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.parameter.RealScalarParam;
import beast.base.spec.type.RealScalar;

@Description("Symmetrical model of nucleotide evolution with equal base frequencies." +
        "Rates that are not specified are assumed to be 1.")
public class SYM extends GeneralSubstitutionModel {
    final public Input<RealScalar<PositiveReal>> rateACInput = new Input<>("rateAC", "substitution rate for A to C (default 1)");
    final public Input<RealScalar<PositiveReal>> rateAGInput = new Input<>("rateAG", "substitution rate for A to G (default 1)");
    final public Input<RealScalar<PositiveReal>> rateATInput = new Input<>("rateAT", "substitution rate for A to T (default 1)");
    final public Input<RealScalar<PositiveReal>> rateCGInput = new Input<>("rateCG", "substitution rate for C to G (default 1)");
    final public Input<RealScalar<PositiveReal>> rateCTInput = new Input<>("rateCT", "substitution rate for C to T (default 1)");
    final public Input<RealScalar<PositiveReal>> rateGTInput = new Input<>("rateGT", "substitution rate for G to T (default 1)");

    RealScalar<PositiveReal> rateAC;
    RealScalar<PositiveReal> rateAG;
    RealScalar<PositiveReal> rateAT;
    RealScalar<PositiveReal> rateCG;
    RealScalar<PositiveReal> rateCT;
    RealScalar<PositiveReal> rateGT;

    // For hardcoding equal base frequencies
    //double[] frequencies;

    public SYM() {
        ratesInput.setRule(Validate.OPTIONAL);
        try {
            ratesInput.setValue(null, this);
        } catch (Exception e) {
            e.printStackTrace();
            // TODO: handle exception
        }

        // Override the superclass SubstitutionModel.Base requirement for input frequencies, since they are equal in SYM
        //frequenciesInput.setRule(Validate.OPTIONAL);
        //try {
        //    initAndValidate();
        //} catch (Exception e) {
        //    e.printStackTrace();
        //    throw new RuntimeException("initAndValidate() call failed when constructing SYM()");
        //}
    }

    @Override
    public void initAndValidate() {
        if (ratesInput.get() != null) {
            throw new IllegalArgumentException("the rates attribute should not be used. Use the individual rates rateAC, rateCG, etc, instead.");
        }

        //if (frequenciesInput.get() != null) {
        //    throw new RuntimeException("Frequencies must not be specified in the SYM model. They are assumed equal.");
        // }

        // Set equal base frequencies
        //frequencies = new double[]{0.25, 0.25, 0.25, 0.25};
        frequencies = frequenciesInput.get();
        updateMatrix = true;
        nrOfStates = frequencies.getFreqs().length;

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
        rateAG = getParameter(rateAGInput);
        rateAT = getParameter(rateATInput);
        rateCG = getParameter(rateCGInput);
        rateCT = getParameter(rateCTInput);
        rateGT = getParameter(rateGTInput);
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
        relativeRates[1] = rateAG.get(); // A->G
        relativeRates[2] = rateAT.get(); // A->T

        relativeRates[3] = rateAC.get(); // C->A
        relativeRates[4] = rateCG.get(); // C->G
        relativeRates[5] = rateCT.get(); // C->T

        relativeRates[6] = rateAG.get(); // G->A
        relativeRates[7] = rateCG.get(); // G->C
        relativeRates[8] = rateGT.get(); // G->T

        relativeRates[9] = rateAT.get(); // T->A
        relativeRates[10] = rateCT.get(); //T->C
        relativeRates[11] = rateGT.get(); //T->G
    }

    @Override
    public boolean canHandleDataType(DataType dataType) {
        return dataType instanceof Nucleotide;
    }
}
