package beast.base.spec.inference.distribution;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.spec.domain.NonNegativeInt;
import beast.base.spec.domain.NonNegativeReal;
import beast.base.spec.inference.parameter.IntScalarParam;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.type.IntScalar;
import beast.base.spec.type.RealScalar;
import org.apache.commons.statistics.distribution.DiscreteDistribution;
import org.apache.commons.statistics.distribution.PoissonDistribution;

import java.util.List;


@Description("Poisson distribution, used as prior  f(k; lambda)=\\frac{lambda^k e^{-lambda}}{k!}  " +
        "If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
        "separate independent component.")
public class Poisson extends TensorDistribution<IntScalar<NonNegativeInt>, NonNegativeInt, Integer> {

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
    protected double calcLogP(List<Integer> value) {
        return dist.logProbability(value.getFirst()); // scalar
    }

    @Override
    protected List<Integer> sample() {
        final int x = sampler.sample();
        return List.of(x);
    }

    /**
     * make sure internal state is up to date *
     */
	void refresh() {
        double lambda = (lambdaInput.get() != null) ? lambdaInput.get().get() : 1.0;

        // Floating point comparison:
        if (isNotEqual(dist.getMean(), lambda)) {
            // The expected number of events (E[X]) in Poisson equals lambda
            dist = PoissonDistribution.of(lambda);
            sampler = dist.createSampler(rng);
        } else if (sampler == null) {
            // Ensure sampler exists
            sampler = dist.createSampler(rng);
        }
    }

    public static void main(String[] args) {
        // in
        RealScalar<NonNegativeReal> lambda = new RealScalarParam<>(5.0, NonNegativeReal.INSTANCE);
        // out
        IntScalar<NonNegativeInt> param = new IntScalarParam<>(0, NonNegativeInt.INSTANCE);
        // include initAndValidate
        Poisson poisson = new Poisson(param, lambda);

        System.out.println("param = " + param + ", logP =" + poisson.calculateLogP());

        /*
k	P(X=k)	log P(X=k)
0	0.006737947	-5.0
1	0.0336897	-3.391
2	0.084224	-2.476
3	0.140374	-1.964
Sum of logP for [0,1,2,3] ≈ -12.831
         */

        for (int i = 0; i < 4; i++) {
            System.out.println("i = " + i + ", logP =" + poisson.calcLogP(List.of(i)));
        }

        // TODO IID of 0, 1, 2, 3

//Not working now :   System.out.println(poisson.calcLogP(List.of(0, 1, 2, 3)));
    }

} // class Poisson
