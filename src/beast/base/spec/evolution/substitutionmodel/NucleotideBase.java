package beast.base.spec.evolution.substitutionmodel;

import beast.base.core.Description;

/**
 * basic implementation of a SubstitutionModel bringing together relevant super class*
 */
@Description(value = "Base implementation of a nucleotide substitution model.", isInheritable = false)
public abstract class NucleotideBase extends Base {

    public double freqA, freqC, freqG, freqT,
    // A+G
    freqR,
    // C+T
    freqY;


    @Override
    public int getStateCount() {
        assert nrOfStates == 4;
        return nrOfStates;
    }

    protected void calculateFreqRY() {
        double[] freqs = frequencies.getFreqs();
        freqA = freqs[0];
        freqC = freqs[1];
        freqG = freqs[2];
        freqT = freqs[3];
        freqR = freqA + freqG;
        freqY = freqC + freqT;
    }


} // class NucleotideBase
