package beast.base.spec.inference.distribution;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.core.Log;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.UnitInterval;
import beast.base.spec.inference.parameter.SimplexParam;
import beast.base.spec.type.RealVector;
import beast.base.spec.type.Simplex;
import org.apache.commons.math3.special.Gamma;
import org.apache.commons.statistics.distribution.ContinuousDistribution;
import org.apache.commons.statistics.distribution.GammaDistribution;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.DoubleStream;


@Description("Dirichlet distribution.  p(x_1,...,x_n;alpha_1,...,alpha_n) = 1/B(alpha) prod_{i=1}^K x_i^{alpha_i - 1} " +
        "where B() is the beta function B(alpha) = prod_{i=1}^K Gamma(alpha_i)/ Gamma(sum_{i=1}^K alpha_i}. ")
public class Dirichlet extends RealTensorDistribution<Simplex, UnitInterval> {

    final public Input<RealVector<PositiveReal>> alphaInput = new Input<>("alpha",
            "coefficients of the Dirichlet distribution", Validate.REQUIRED);

    private final double expectedSum = 1.0;
    private GammaDistribution[] gammas;

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
    public double logDensity(double[] x) {
        List<Double> alpha = alphaInput.get().getElements();
        if (alpha.size() != x.length)
            throw new IllegalArgumentException("Dimensions of alpha and param should be the same, " +
                    "but dim(alpha)=" + alpha.size() + " and dim(x)=" + x.length);

        double logP = 0;
        for (int i = 0; i < x.length; i++) {
            logP += (alpha.get(i) - 1) * Math.log(x[i]);
            logP -= Gamma.logGamma(alpha.get(i));
        }
        double alphaSum = alpha.stream().mapToDouble(Double::doubleValue).sum();
        logP += Gamma.logGamma(alphaSum);

        // area = sumX^(dim-1)
        double sumX = DoubleStream.of(x).sum();
        if (Math.abs(sumX - expectedSum) > 1e-6) {
            Log.trace("sum of values (" + sumX +") differs significantly from the expected sum of values (" + expectedSum +")");
            return Double.NEGATIVE_INFINITY;
        }
        logP -= (x.length - 1) * Math.log(sumX);

        return logP;
    }

    @Override
    public List<Simplex> sample(final int size) {
        final int dim = alphaInput.get().size();
        List<Simplex> samples = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            double[] dirichletSample = new double[dim];
            double sum = 0.0;
            for (int j = 0; j < dim; j++) {
                dirichletSample[j] = gammas[i].createSampler(null).sample();
                sum += dirichletSample[j];
            }
            // Normalize
            for (int j = 0; j < dim; j++) {
                // if expectedSum != 1, then adjust the sum to it
                dirichletSample[j] = (dirichletSample[j] / sum) * expectedSum;
            }
            samples.add(valueToTensor(dirichletSample));
        }
        return samples;
    }


    @Override
    public ContinuousDistribution getDistribution() {
        throw new UnsupportedOperationException("It is not supported by apache statistics !");
    }

    @Override
    protected Simplex valueToTensor(double[] value) {
        return new SimplexParam(value);
    }

    @Override
    public Double getOffset() {
        throw new UnsupportedOperationException("It is not supported !");
    }


}
