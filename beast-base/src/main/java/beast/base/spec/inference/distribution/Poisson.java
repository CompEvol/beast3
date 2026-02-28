package beast.base.spec.inference.distribution;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.spec.domain.NonNegativeInt;
import beast.base.spec.domain.NonNegativeReal;
import beast.base.spec.inference.parameter.IntScalarParam;
import beast.base.spec.inference.parameter.IntVectorParam;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.type.IntScalar;
import beast.base.spec.type.RealScalar;
import org.apache.commons.statistics.distribution.DiscreteDistribution;
import org.apache.commons.statistics.distribution.PoissonDistribution;

import java.util.List;


/**
 * Poisson distribution parameterised by its mean (lambda).
 * Supports both integer and real-valued parameters. When applied to a
 * multidimensional parameter, each dimension is treated as an independent component.
 */
@Description("Poisson distribution, used as prior  f(k; lambda)=\\frac{lambda^k e^{-lambda}}{k!}  " +
        "If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
        "separate independent component.")
public class Poisson extends ScalarDistribution<IntScalar<NonNegativeInt>, Integer> {

    // allow 0
    final public Input<RealScalar<NonNegativeReal>> lambdaInput = new Input<>(
            "lambda", "rate parameter, defaults to 1");

    private PoissonDistribution dist = PoissonDistribution.of(1.0); // mean = lambda
    private DiscreteDistribution.Sampler sampler;

    /**
     * Must provide empty constructor for construction by XML.
     * Note that this constructor DOES NOT call initAndValidate();
     */
    public Poisson() {
    }

    public Poisson(IntScalar<NonNegativeInt> param, RealScalar<NonNegativeReal> lambda) {

        try {
            initByName("param", param, "lambda", lambda);
        } catch (Exception e) {
            throw new RuntimeException( "Failed to initialize " + getClass().getSimpleName() +
                    " via initByName in constructor.", e );
        }
    }

    @Override
    public void initAndValidate() {
        refresh();
        super.initAndValidate();
    }

    @Override
    public double calculateLogP() {
        int y = param.get();
        logP = getApacheDistribution().logProbability(y); // no unboxing needed, faster
        return logP;
    }

    @Override
	public List<Integer> sample() {
        if (sampler == null) {
            // Ensure sampler exists
            sampler = dist.createSampler(rng);
        }
        final int x = sampler.sample();
        return List.of(x); // Returning an immutable result
    }

    /**
     * make sure internal state is up to date *
     */
    @Override
    public void refresh() {
        double lambda = (lambdaInput.get() != null) ? lambdaInput.get().get() : 1.0;

        // Floating point comparison:
        if (isNotEqual(dist.getMean(), lambda)) {
            // The expected number of events (E[X]) in Poisson equals lambda
            dist = PoissonDistribution.of(lambda);
        }
    }

    @Override
    public boolean isIntegerDistribution() {
        return true;
    }

    @Override
    protected PoissonDistribution getApacheDistribution() {
        refresh(); // this make sure distribution parameters are updated if they are sampled during MCMC
        return dist;
    }

    public static void main(String[] args) {
        // model param is immutable
        RealScalar<NonNegativeReal> lambda = new RealScalarParam<>(5.0, NonNegativeReal.INSTANCE);
        // this is a state node, its value can be changed
        IntScalarParam<NonNegativeInt> param = new IntScalarParam<>(0, NonNegativeInt.INSTANCE);
        // include initAndValidate
        Poisson poisson = new Poisson(param, lambda);

        System.out.println("param = " + param + ", logP =" + poisson.calculateLogP());


        /** R code
         * x <- c(0, 1, 2, 3)
         * lambda <- 5
         * logP <- dpois(x, lambda, log = TRUE)
         * sum(logP)  # total log-likelihood across observations
         */

        /*
k	log P(X=k)
0	-5.0
1	-3.390562
2	-2.474271
3	-1.963446
Sum of logP for [0,1,2,3] ≈ -12.82828
         */

        for (int i = 0; i < 4; i++) {
            param.set(i);
            poisson = new Poisson(param, lambda);
            System.out.println("i = " + i + ", logP =" + poisson.calculateLogP());
        }

        // IID
        IID iid = new IID(
                new IntVectorParam(new int[]{0, 1, 2, 3}, NonNegativeInt.INSTANCE),
                poisson);

        System.out.println("param = " + iid.param + ", logP =" + iid.calculateLogP());

    }

} // class Poisson
