package beast.base.spec.evolution.substitutionmodel;

import java.lang.reflect.InvocationTargetException;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.evolution.datatype.DataType;
import beast.base.evolution.datatype.Nucleotide;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.type.RealScalar;
import beast.base.spec.type.Simplex;

/**
 * General Time Reversible (GTR) model of nucleotide evolution.
 * The most general neutral, independent, finite-sites, time-reversible model,
 * with six substitution rate parameters and four equilibrium base frequencies.
 * <p>
 * The rates are given in one of two ways:
 * <ul>
 * <li>{@code rates}: the six rates as one vector of proportions that sum to 1, in the order
 * AC, AG, AT, CG, CT, GT. This is the form to use with a Dirichlet prior on the rates.</li>
 * <li>{@code rateAC}, {@code rateAG}, ... : six separate rates. Rates that are not specified
 * default to 1. One of them is usually fixed (by convention {@code rateCT}) to make the others
 * identifiable.</li>
 * </ul>
 * The rate matrix is normalised to one expected substitution per unit of time in both cases,
 * so only the ratios of the rates matter and the two forms describe the same model.
 */
@Description("General Time Reversible model of nucleotide evolution. " +
        "The rates are given either as one vector 'rates' of six proportions that sum to 1 " +
        "(in the order AC, AG, AT, CG, CT, GT), or as six separate rates rateAC, rateAG, ..., " +
        "where rates that are not specified are assumed to be 1. ")
public class GTR extends BasicGeneralSubstitutionModel {
    final public Input<Simplex> ratesInput = new Input<>("rates", "the six substitution rates as proportions that sum to 1, "
            + "in the order AC, AG, AT, CG, CT, GT. Cannot be combined with the separate rate inputs.");
    final public Input<RealScalar<PositiveReal>> rateACInput = new Input<>("rateAC", "substitution rate for A to C (default 1)");
    final public Input<RealScalar<PositiveReal>> rateAGInput = new Input<>("rateAG", "substitution rate for A to G (default 1)");
    final public Input<RealScalar<PositiveReal>> rateATInput = new Input<>("rateAT", "substitution rate for A to T (default 1)");
    final public Input<RealScalar<PositiveReal>> rateCGInput = new Input<>("rateCG", "substitution rate for C to G (default 1)");
    final public Input<RealScalar<PositiveReal>> rateCTInput = new Input<>("rateCT", "substitution rate for C to T (default 1)");
    final public Input<RealScalar<PositiveReal>> rateGTInput = new Input<>("rateGT", "substitution rate for G to T (default 1)");

    /** the order of the rates in the rates input */
    private static final int AC = 0, AG = 1, AT = 2, CG = 3, CT = 4, GT = 5;

    Simplex rates;
    RealScalar<PositiveReal> rateAC;
    RealScalar<PositiveReal> rateAG;
    RealScalar<PositiveReal> rateAT;
    RealScalar<PositiveReal> rateCG;
    RealScalar<PositiveReal> rateCT;
    RealScalar<PositiveReal> rateGT;

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

        rates = ratesInput.get();
        if (rates != null) {
            if (rates.size() != 6) {
                throw new IllegalArgumentException("Input 'rates' must have 6 elements (AC, AG, AT, CG, CT, GT), but has "
                        + rates.size());
            }
            for (Input<?> rateInput : new Input<?>[] {rateACInput, rateAGInput, rateATInput, rateCGInput, rateCTInput,
                    rateGTInput}) {
                if (rateInput.get() != null) {
                    throw new IllegalArgumentException("Specify either the input 'rates' or the separate rates "
                            + "(rateAC, rateAG, ...), not both: found 'rates' and '" + rateInput.getName() + "'");
                }
            }
            return;
        }

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
        final double ac, ag, at, cg, ct, gt;
        if (rates != null) {
            ac = rates.get(AC);
            ag = rates.get(AG);
            at = rates.get(AT);
            cg = rates.get(CG);
            ct = rates.get(CT);
            gt = rates.get(GT);
        } else {
            ac = rateAC.get();
            ag = rateAG.get();
            at = rateAT.get();
            cg = rateCG.get();
            ct = rateCT.get();
            gt = rateGT.get();
        }
        relativeRates[0] = ac; // A->C
        relativeRates[1] = ag; // A->G
        relativeRates[2] = at; // A->T

        relativeRates[3] = ac; // C->A
        relativeRates[4] = cg; // C->G
        relativeRates[5] = ct; // C->T

        relativeRates[6] = ag; // G->A
        relativeRates[7] = cg; // G->C
        relativeRates[8] = gt; // G->T

        relativeRates[9] = at; // T->A
        relativeRates[10] = ct; //T->C
        relativeRates[11] = gt; //T->G
    }

    @Override
    public boolean canHandleDataType(DataType dataType) {
        return dataType instanceof Nucleotide;
    }
}
