package beast.base.spec.inference.distribution;


import beast.base.core.BEASTInterface;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.core.Log;
import beast.base.inference.State;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.type.RealScalar;
import beast.base.spec.type.RealVector;
import org.apache.commons.math.distribution.GammaDistribution;
import org.apache.commons.math.distribution.GammaDistributionImpl;
import org.apache.commons.statistics.distribution.LogNormalDistribution;

import java.util.List;
import java.util.Random;



/**
 * Initial version Ported from Beast 1.7 ExponentialMarkovModel
 */
@Description("A class that produces a distribution chaining values in a parameter through the Gamma distribution. " +
        "The value of a parameter is assumed to be Gamma distributed with mean as the previous value in the parameter. " +
		"If useLogNormal is set, a log normal distribution is used instead of a Gamma. " +
        "If a Jeffrey's prior is used, the first value is assumed to be distributed as 1/x, otherwise it is assumed to be uniform. " +
        "Handy for population parameters. ")
public class MarkovChainDistribution extends TensorDistribution<RealVector<PositiveReal>, Double> {

    final public Input<Boolean> isJeffreysInput = new Input<>("jeffreys", "use Jeffrey's prior (default false)", false);
    final public Input<Boolean> isReverseInput = new Input<>("reverse", "parameter in reverse (default false)", false);
    final public Input<Boolean> useLogInput = new Input<>("uselog", "use logarithm of parameter values (default false)", false);
    final public Input<Double> shapeInput = new Input<>("shape", "shape parameter of the Gamma distribution (default 1.0 = exponential distribution) " +
    		" or precision parameter if the log normal is used.", 1.0);
//    final public Input<RealVector<PositiveReal>> parameterInput = new Input<>("parameter", "chain parameter to calculate distribution over", Validate.REQUIRED);

    final public Input<RealScalar<PositiveReal>> initialMeanInput = new Input<>("initialMean", "the mean of the prior distribution on the first element. This is an alternative boundary condition to Jeffrey's on the first value.", Validate.OPTIONAL);

    final public Input<Boolean> useLogNormalInput = new Input<>("useLogNormal", "use Log Normal distribution instead of Gamma (default false)", false);
  
    // **************************************************************
    // Private instance variables
    // **************************************************************
    private RealVector<PositiveReal> chainParameter = null;
    private RealScalar<PositiveReal> initialMean = null;
    private boolean jeffreys = false;
    private boolean reverse = false;
    private boolean uselog = false;
    private double shape = 1.0;
    GammaDistribution gamma;
    LogNormalDistribution logNormal;
    boolean useLogNormal;
    private static int warningCount = 0;

    public MarkovChainDistribution() {
		// paramInput.setTipText("chain parameter to calculate distribution over");
	}
    
    
    @Override
    public void initAndValidate() {
        reverse = isReverseInput.get();
        jeffreys = isJeffreysInput.get();
        uselog = useLogInput.get();
        shape = shapeInput.get();
        chainParameter = paramInput.get();
        initialMean = initialMeanInput.get();
        useLogNormal = useLogNormalInput.get();
        gamma = new GammaDistributionImpl(shape, 1);
        logNormal = LogNormalDistribution.of(1.0, 1.0);

        if (jeffreys && initialMean != null) {
            throw new RuntimeException("Must specify either Jeffrey's prior or an initial mean, but not both");
        }
    }


    /**
     * Get the log likelihood.
     *
     * @return the log likelihood.
     */
    @SuppressWarnings("deprecation")
	@Override
    public double calculateLogP() {
        logP = 0.0;
        // jeffreys Prior!
        if (jeffreys) {
            logP += -Math.log(getChainValue(0));
        }
        int first = 1;
        if (initialMean != null) first = 0;

        for (int i = first; i < chainParameter.size(); i++) {
            final double mean = getChainValue(i - 1);
            final double x = getChainValue(i);

            if (useLogNormal) {
	            final double sigma = 1.0 / shape; // shape = precision
	            // convert mean to log space
	            final double M = Math.log(mean) - (0.5 * sigma * sigma);
	            logNormal = LogNormalDistribution.of(M, sigma);
	            logP += logNormal.logDensity(x);
            } else {
                final double scale = mean / shape;
                gamma.setBeta(scale);
                logP += gamma.logDensity(x);
            }
        }
        if (logP == Double.POSITIVE_INFINITY && warningCount == 0) {
        	Log.warning("WARNING: Positive infinity calculated for MarkovChainDistribution (" + getID() + ")");
        	Log.warning("This indicates there may be some numerical instability due to\n"
        			  + "the chain parameter (" + ((BEASTInterface) chainParameter).getID()+ ") escaping\n"
        			  + "to very small values. Consider putting a small lower bound on the\n"
        			  + "chain parameter to prevent this.");
        	warningCount++;
        }
        return logP;
    }
    
    private double getChainValue(int i) {
        if (i == -1) {
            if (initialMean != null){
                return initialMean.get();
            } else {
                throw new IllegalArgumentException("index must be non-negative unless intial value provided.");
            }
        }

        if (uselog) {
            return Math.log(chainParameter.get(index(i)));
        } else {
            return chainParameter.get(index(i));
        }
    }

    private int index(int i) {
        if (reverse)
            return chainParameter.size() - i - 1;
        else
            return i;
    }

    @Override
    public List<String> getArguments() {
		throw new RuntimeException("not implemented yet");
    }

    @Override
    public List<String> getConditions() {
		throw new RuntimeException("not implemented yet");
    }

    @Override
    public void sample(State state, Random random) {
		throw new RuntimeException("not implemented yet");
    }


	@Override
	protected double calcLogP(Double... value) {
		logP = calculateLogP();
		return logP;
	}

    @Override
    public Double getLower() {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    public Double getUpper() {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
	protected List<Double> sample() {
		throw new RuntimeException("not implemented yet");
	}
}

