package beast.base.spec.inference.distribution;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.UnitInterval;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.type.RealScalar;
import org.apache.commons.statistics.distribution.ContinuousDistribution;
import org.apache.commons.statistics.distribution.GammaDistribution;

import java.util.List;

// TODO need to test
@Description("Inverse Gamma distribution, used as prior.    for x>0  f(x; alpha, beta) = \frac{beta^alpha}{Gamma(alpha)} (1/x)^{alpha + 1}exp(-beta/x) " +
        "If the input x is a multidimensional parameter, each of the dimensions is considered as a " +
        "separate independent component.")
public class InverseGamma extends TensorDistribution<RealScalar<PositiveReal>, PositiveReal, Double> {

    final public Input<RealScalar<PositiveReal>> alphaInput = new Input<>("alpha",
            "shape parameter, defaults to 1");
    final public Input<RealScalar<PositiveReal>> betaInput = new Input<>("beta",
            "scale parameter, defaults to 1");

    private GammaDistribution gamma = GammaDistribution.of(1, 1);

    /**
     * Must provide empty constructor for construction by XML.
     * Note that this constructor DOES NOT call initAndValidate();
     */
    public InverseGamma() {}

    public InverseGamma(RealScalar<UnitInterval> param,
                RealScalar<PositiveReal> alpha, RealScalar<PositiveReal> beta) {

        try {
            initByName("param", param, "alpha", alpha, "beta", beta);
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

    /**
     * ensure internal state is up to date *
     */
    void refresh() {
        double alpha = (alphaInput.get() != null) ? alphaInput.get().get() : 1.0;
        double beta  = (betaInput.get()  != null) ? betaInput.get().get()  : 1.0;

        // Floating point comparison
        if (Math.abs(gamma.getShape() - alpha) > EPS ||  Math.abs(gamma.getScale() - 1.0 / beta) > EPS)
            gamma = GammaDistribution.of(alpha, 1.0 / beta);

    }

    private double density(double x) {
//        double logP = logDensity(x);
//        return Math.exp(logP);
//        x -= getOffset();
        // TODO check : This uses the change-of-variable formula for PDFs.
        return (x > 0) ? gamma.density(1.0 / x) / (x * x) : 0.0;
    }

    @Override
    protected double calcLogP(Double... value) {
        // If x is positive zero or negative zero, then the result is negative infinity.
        return Math.log(this.density(value[0])); // scalar
    }

    @Override
    protected List<RealScalar<PositiveReal>> sample() {
        ContinuousDistribution.Sampler sampler = gamma.createSampler(rng);
        final double y = sampler.sample();  // sample from Gamma
        final double x = 1.0 / y; // sample from Gamma
        RealScalarParam<PositiveReal> param = new RealScalarParam<>(x, PositiveReal.INSTANCE);
        return List.of(param);
    }

    //        @Override
//        public double density(double x) {
//            double logP = logDensity(x);
//            return Math.exp(logP);
//        }
//
//  C = m_fAlpha * Math.log(m_fBeta) - org.apache.commons.math.special.Gamma.logGamma(m_fAlpha);
//        @Override
//        public double logDensity(double x) {
//            double logP = -(m_fAlpha + 1.0) * Math.log(x) - (m_fBeta / x) + C;
//            return logP;
//        }

//    public class InverseGammaDistribution implements ContinuousDistribution {
//        private final double alpha; // shape
//        private final double beta;  // scale
//        private final GammaDistribution gamma;
//
//        private InverseGammaDistribution(double alpha, double beta) {
//            this.alpha = alpha;
//            this.beta = beta;
//            // Gamma uses shape = alpha, scale = 1/beta
//            this.gamma = GammaDistribution.of(alpha, 1.0 / beta);
//        }
//
//        public static InverseGammaDistribution of(double alpha, double beta) {
//            return new InverseGammaDistribution(alpha, beta);
//        }
//
//
//
//        public void setAlphaBeta(double alpha, double beta) {
//            m_fAlpha = alpha;
//            m_fBeta = beta;
//            C = m_fAlpha * Math.log(m_fBeta) - org.apache.commons.math.special.Gamma.logGamma(m_fAlpha);
//        }
//
//        @Override
//        public double cumulativeProbability(double x) throws MathException {
//            throw new MathException("Not implemented yet");
//        }
//
//        @Override
//        public double survivalProbability(double x) {
//            return ContinuousDistribution.super.survivalProbability(x);
//        }
//
//        @Override
//        public double cumulativeProbability(double x0, double x1) throws MathException {
//            throw new UnsupportedOperationException("Not implemented yet");
//        }
//
//        @Override
//        public double inverseCumulativeProbability(double p) throws MathException {
//            throw new MathException("Not implemented yet");
//        }
//
//        @Override
//        public double inverseSurvivalProbability(double p) {
//            return ContinuousDistribution.super.inverseSurvivalProbability(p);
//        }
//
//        @Override
//        public double getMean() {
//            return 0;
//        }
//
//        @Override
//        public double getVariance() {
//            return 0;
//        }
//
//        @Override
//        public double getSupportLowerBound() {
//            return 0;
//        }
//
//        @Override
//        public double getSupportUpperBound() {
//            return 0;
//        }
//
//        @Override
//        public Sampler createSampler(UniformRandomProvider rng) {
//            return null;
//        }
//
//        @Override
//        public double density(double x) {
//            double logP = logDensity(x);
//            return Math.exp(logP);
//        }
//
//        @Override
//        public double probability(double x0, double x1) {
//            return ContinuousDistribution.super.probability(x0, x1);
//        }
//
//        @Override
//        public double logDensity(double x) {
//            double logP = -(m_fAlpha + 1.0) * Math.log(x) - (m_fBeta / x) + C;
//            return logP;
//        }
//    } // class InverseGammaImpl



} // class InverseGamma
