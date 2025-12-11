package beast.base.spec.inference.distribution;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.core.Log;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.type.RealVector;
import beast.base.spec.type.Simplex;
import org.apache.commons.math3.special.Gamma;
import org.apache.commons.statistics.distribution.GammaDistribution;

import java.util.Arrays;
import java.util.List;


@Description("Dirichlet distribution.  p(x_1,...,x_n;alpha_1,...,alpha_n) = 1/B(alpha) prod_{i=1}^K x_i^{alpha_i - 1} " +
        "where B() is the beta function B(alpha) = prod_{i=1}^K Gamma(alpha_i)/ Gamma(sum_{i=1}^K alpha_i}. ")
public class Dirichlet extends TensorDistribution<Simplex, Double> {

    final public Input<RealVector<PositiveReal>> alphaInput = new Input<>("alpha",
            "coefficients of the Dirichlet distribution", Validate.REQUIRED);

    private GammaDistribution[] gammas;

    /**
     * Must provide empty constructor for construction by XML.
     * Note that this constructor DOES NOT call initAndValidate();
     */
    public Dirichlet() {}

    public Dirichlet(Simplex param, RealVector<PositiveReal> alpha) {

        try {
            initByName("param", param, "alpha", alpha);
        } catch (Exception e) {
            throw new RuntimeException( "Failed to initialize " + getClass().getSimpleName() +
                    " via initByName in constructor.", e );
        }
    }

    @Override
    public void initAndValidate() {
        refresh();
        super.initAndValidate(); // load param here
        if (alphaInput.get().size() != dimension())
            throw new IllegalArgumentException("Dimensions of alpha and param should be the same, " +
                    "but dim(alpha)=" + alphaInput.get().size() + " and dim(x)=" + dimension());
    }

    /**
     * ensure internal state is up to date *
     */
    void refresh() {
        List<Double> alpha = alphaInput.get().getElements();
        gammas = new GammaDistribution[alpha.size()];

        for (int i = 0; i < alpha.size(); i++) {
            if (gammas[i] == null)
                gammas[i] = GammaDistribution.of(alpha.get(i), 1);

            // Floating point comparison
            if (Math.abs(gammas[i].getShape() - alpha.get(i)) > EPS)
                gammas[i] = GammaDistribution.of(alpha.get(i), 1.0);
        }
    }

    @Override
    protected double calcLogP(Double... value) {
        return this.calcLogP(Arrays.asList(value));
    }

    @Override
    public double calculateLogP() {
        // Avoid unnecessary conversions, use List<> directly for better performance
        logP = this.calcLogP(param.getElements());
        return logP;
    }

    private double calcLogP(List<Double> value) {
        List<Double> alpha = alphaInput.get().getElements();
        if (alpha.size() != value.size())
            throw new IllegalArgumentException("Dimensions of alpha and param should be the same, " +
                    "but dim(alpha)=" + alpha.size() + " and dim(x)=" + value.size());

        double logP = 0;
        for (int i = 0; i < value.size(); i++) {
            logP += (alpha.get(i) - 1) * Math.log(value.get(i));
            logP -= Gamma.logGamma(alpha.get(i));
        }
        double alphaSum = alpha.stream().mapToDouble(Double::doubleValue).sum();
        logP += Gamma.logGamma(alphaSum);

        // area = sumX^(dim-1)
        double sumX = value.stream()              // Stream<Double>
                .mapToDouble(Double::doubleValue) // unbox to double
                .sum();
        final double expectedSum = 1.0;
        if (Math.abs(sumX - expectedSum) > 1e-6) {
            Log.trace("sum of values (" + sumX +") differs significantly from the expected sum of values (" + expectedSum +")");
            return Double.NEGATIVE_INFINITY;
        }
        logP -= (value.size() - 1) * Math.log(sumX);

        return logP;
    }

    @Override
    public List<Double> sample() {
        Double[] dirichletSample = new Double[dimension()];
        double sum = 0.0;
        for (int i = 0; i < dimension(); i++) {
            dirichletSample[i] = gammas[i].createSampler(null).sample();
            sum += dirichletSample[i]; // use primitive double for speed
        }
        // Normalize
        for (int i = 0; i < dimension(); i++) {
            // if expectedSum != 1, then adjust the sum to it
            dirichletSample[i] = (dirichletSample[i] / sum);
        }
        // Returning an immutable result
        return List.of(dirichletSample);
    }

    @Override
    public Double getLower() {
        // all gammas have the same bounds
        return gammas[0].getSupportLowerBound();
    }

    @Override
    public Double getUpper() {
        return gammas[0].getSupportUpperBound();
    }
}
