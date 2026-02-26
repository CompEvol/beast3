package beast.base.spec.inference.distribution;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.type.RealScalar;
import org.apache.commons.statistics.distribution.ContinuousDistribution;
import org.apache.commons.statistics.distribution.LaplaceDistribution;

import java.util.List;

@Description("Laplace distribution.    f(x|\\mu,b) = \\frac{1}{2b} \\exp \\left( -\\frac{|x-\\mu|}{b} \\right)" +
        "The probability density function of the Laplace distribution is also reminiscent of the normal distribution; " +
        "however, whereas the normal distribution is expressed in terms of the squared difference from the mean ?, " +
        "the Laplace density is expressed in terms of the absolute difference from the mean. Consequently the Laplace " +
        "distribution has fatter tails than the normal distribution.")
public class Laplace extends ScalarDistribution<RealScalar<Real>, Double> {

    final public Input<RealScalar<Real>> muInput = new Input<>("mu",
            "location parameter, defaults to 0");
    final public Input<RealScalar<PositiveReal>> scaleInput = new Input<>("scale",
            "scale parameter, defaults to 1");

    private LaplaceDistribution dist = LaplaceDistribution.of(0, 1);
    private ContinuousDistribution.Sampler sampler;

    /**
     * Must provide empty constructor for construction by XML.
     * Note that this constructor DOES NOT call initAndValidate();
     */
    public Laplace() {}

    public Laplace(RealScalar<Real> param,
                   RealScalar<Real> mu, RealScalar<PositiveReal> scale) {

        try {
            initByName("param", param, "mu", mu, "scale", scale);
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
     * make sure internal state is up to date *
     */
    @Override
    public void refresh() {
        double mu = (muInput.get() != null) ? muInput.get().get() : 0.0;
        double scale  = (scaleInput.get()  != null) ? scaleInput.get().get()  : 1.0;

        // Floating point comparison
        if (isNotEqual(dist.getLocation(), mu) ||  isNotEqual(dist.getScale(), scale)) {
            dist = LaplaceDistribution.of(mu, scale);
        }
    }

    @Override
    public double calculateLogP() {
        logP = getApacheDistribution().logDensity(param.get()); // no unboxing needed, faster
        return logP;
    }

    @Override
	public List<Double> sample() {
        if (sampler == null) {
            // Ensure sampler exists
            sampler = dist.createSampler(rng);
        }
        final double x = sampler.sample();
        return List.of(x); // Returning an immutable result
    }

    @Override
	protected LaplaceDistribution getApacheDistribution() {
        refresh(); // this make sure distribution parameters are updated if they are sampled during MCMC
        return dist;
    }

} // class
