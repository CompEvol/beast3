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
import org.apache.commons.statistics.distribution.PoissonDistribution;


@Description("Poisson distribution, used as prior  f(k; lambda)=\\frac{lambda^k e^{-lambda}}{k!}  " +
        "If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
        "separate independent component.")
public class Poisson extends IntTensorDistribution<IntScalar<NonNegativeInt>, NonNegativeInt> {

    // allow 0
    final public Input<RealScalar<NonNegativeReal>> lambdaInput = new Input<>(
            "lambda", "rate parameter, defaults to 1");

    protected PoissonDistribution dist = PoissonDistribution.of(1.0); // mean = lambda

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
    protected PoissonDistribution getDistribution() {
        refresh();
        return dist;
    }

    @Override
    protected IntScalar<NonNegativeInt> valueToTensor(int value) {
        return new IntScalarParam<>(value, NonNegativeInt.INSTANCE);
    }

    @Override
    public void initAndValidate() {
        refresh();
    }

    /**
     * make sure internal state is up to date *
     */
	void refresh() {
        double lambda = (lambdaInput.get() != null) ? lambdaInput.get().get() : 1.0;

        // Floating point comparison:
        if (Math.abs(dist.getMean() - lambda) > EPS)
            // The expected number of events (E[X]) in Poisson equals lambda
            dist = PoissonDistribution.of(lambda);
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
            System.out.println("i = " + i + ", logP =" + poisson.calcLogP(poisson.valueToTensor(i)));
        }

        // TODO calculateLogP() allows to sum the logP of each scalar,
        //  this is interesting, but cannot do through Input and constructor

        System.out.println(poisson.calcLogP(new IntVectorParam(new int[]{0, 1, 2, 3}, NonNegativeInt.INSTANCE)));
    }

} // class Poisson
