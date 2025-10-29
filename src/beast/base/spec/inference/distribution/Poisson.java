package beast.base.spec.inference.distribution;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Log;
import beast.base.spec.domain.Int;
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
public class Poisson extends IntTensorDistribution<NonNegativeInt> {

    // allow 0
    final public Input<RealScalar<NonNegativeReal>> lambdaInput = new Input<>(
            "lambda", "rate parameter, defaults to 1");

    private PoissonDistribution dist = PoissonDistribution.of(1.0);

    /**
     * Must provide empty constructor for construction by XML.
     * Note that this constructor DOES NOT call initAndValidate();
     */
    public Poisson() {
    }

    public Poisson(IntScalar<NonNegativeInt> tensor, RealScalar<NonNegativeReal> lambda) {

        try {
            initByName("tensor", tensor, "lambda", lambda);
        } catch (Exception e) {
            throw new RuntimeException( "Failed to initialize " + getClass().getSimpleName() +
                    " via initByName in constructor.", e );
        }
    }

    @Override
    PoissonDistribution getDistribution() {
        refresh();
        return dist;
    }

    @Override
    public void initAndValidate() {
        refresh();
    }

    /**
     * make sure internal state is up to date *
     */
    @SuppressWarnings("deprecation")
	void refresh() {
        double lambda = 1;
        if (lambdaInput.get() != null) {
            lambda = lambdaInput.get().get();
            if (lambda < 0) {
                Log.err.println("Poisson::lambda should be positive not " + lambda + ". Assign it to the default value.");
                lambda = 1;
            }
        }
        // PoissonDistribution is immutable
        // only update if not same
        if (lambda != dist.getMean())
            dist = PoissonDistribution.of(lambda);
    }

    public static void main(String[] args) {
        // in
        RealScalar<NonNegativeReal> lambda = new RealScalarParam<>(5.0, NonNegativeReal.INSTANCE);
        // out
        IntScalar<NonNegativeInt> tensor = new IntScalarParam<>(0, NonNegativeInt.INSTANCE);
        // include initAndValidate
        Poisson poisson = new Poisson(tensor, lambda);

        System.out.println("tensor = " + tensor + ", logP =" + poisson.calculateLogP());

        /*
k	P(X=k)	log P(X=k)
0	0.006737947	-5.0
1	0.0336897	-3.391
2	0.084224	-2.476
3	0.140374	-1.964
Sum of logP for [0,1,2,3] ≈ -12.831
         */

        for (int i = 0; i < 4; i++) {
            System.out.println("i = " + i + ", logP =" + poisson.calcLogP(new IntScalarParam(i, Int.INSTANCE)));
        }

        System.out.println(poisson.calcLogP(new IntVectorParam(new int[]{0, 1, 2, 3}, Int.INSTANCE)));
    }

} // class Poisson
