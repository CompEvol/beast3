package beast.base.spec.inference.distribution;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.spec.domain.Int;
import beast.base.spec.domain.NonNegativeInt;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.inference.parameter.IntScalarParam;
import beast.base.spec.inference.parameter.IntVectorParam;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.type.IntScalar;
import beast.base.spec.type.IntVector;
import beast.base.spec.type.RealScalar;
import org.apache.commons.math3.distribution.IntegerDistribution;
import org.apache.commons.math3.distribution.PoissonDistribution;


@Description("Poisson distribution, used as prior  f(k; lambda)=\\frac{lambda^k e^{-lambda}}{k!}  " +
        "If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
        "separate independent component.")
public class Poisson<D extends NonNegativeInt> extends AbstractDiscreteDistribution<D> {
    final public Input<RealScalar<? extends PositiveReal>> lambdaInput = new Input<>("lambda", "rate parameter, defaults to 1");

    private PoissonDistribution dist = new PoissonDistribution(1);


    // Must provide empty constructor for construction by XML. Note that this constructor DOES NOT call initAndValidate();
    public Poisson() {
    }

    public Poisson(RealScalar<? extends PositiveReal> lambda) {

        try {
            initByName("lambda", lambda);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initByName lambda parameter when constructing Poisson instance.");
        }
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
        double lambda;
        if (lambdaInput.get() == null) {
            lambda = 1;
        } else {
            lambda = lambdaInput.get().get();
            if (lambda < 0) {
                lambda = 1;
            }
        }
        // math3 PoissonDistribution is immutable
        // only update if not same
        if (lambda != dist.getMean())
            dist = new PoissonDistribution(lambda);
    }

//TODO    @Override
    public double calcLogP(IntScalar<D> scalar) {
        return super.calcLogP(scalar);
    }

//TODO    @Override
    public double calcLogP(IntVector<D> vector) {
        return super.calcLogP(vector);
    }

    @Override
    public IntegerDistribution getDistribution() {
        refresh();
        return dist;
    }
    
    @Override
    public double getMeanWithoutOffset() {
    	return lambdaInput.get().get();
    }

    @Override
    public double getMean() {
        throw  new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double getVariance() {
        throw  new UnsupportedOperationException("Not supported yet.");
    }

    public static void main(String[] args) {
        RealScalar<PositiveReal> lambda = new RealScalarParam<>(5.0, PositiveReal.INSTANCE);
        Poisson poisson = new Poisson(lambda);

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
