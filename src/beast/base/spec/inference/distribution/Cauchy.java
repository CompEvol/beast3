package beast.base.spec.inference.distribution;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.type.RealScalar;
import org.apache.commons.statistics.distribution.CauchyDistribution;
import org.apache.commons.statistics.distribution.ContinuousDistribution;

import java.util.List;


@Description("Cauchy distribution. The probability density function of \\( X \\) is:\n" +
        "\\[ f(x; x_0, \\gamma) = { 1 \\over \\pi \\gamma } \\left[ { \\gamma^2 \\over (x - x_0)^2 + \\gamma^2 } \\right] \\]\n" +
        "for \\( x_0 \\) the location, \\( \\gamma > 0 \\) the scale, and \\( x \\in (-\\infty, \\infty) \\).")
public class Cauchy extends ScalarDistribution<RealScalar<Real>, Double> {

    final public Input<RealScalar<Real>> locationInput = new Input<>("location",
            "center of the peak");
    final public Input<RealScalar<PositiveReal>> scaleInput = new Input<>("scale",
            "the scale parameter, control spread");

    private CauchyDistribution dist = CauchyDistribution.of(0, 1);
    private ContinuousDistribution.Sampler sampler;

    /**
     * Must provide empty constructor for construction by XML.
     * Note that this constructor DOES NOT call initAndValidate();
     */
    public Cauchy() {
    }

    public Cauchy(RealScalar<Real> param, RealScalar<Real> location, RealScalar<PositiveReal> scale) {

        try {
            initByName("param", param, "location", location, "scale", scale);
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
        double location = (locationInput.get() != null) ? locationInput.get().get() : 0.0;
        double scale  = (scaleInput.get()  != null) ? scaleInput.get().get()  : 1.0;

        // Floating point comparison
        if (isNotEqual(dist.getLocation(), location) ||  isNotEqual(dist.getScale(), scale)) {
            dist = CauchyDistribution.of(location, scale);
            sampler = dist.createSampler(rng);
        } else if (sampler == null) {
            // Ensure sampler exists
            sampler = dist.createSampler(rng);
        }
    }

    @Override
    public double calculateLogP() {
        logP = dist.logDensity(param.get() - getOffset()); // no unboxing needed, faster
        return logP;
    }

    @Override
    protected double calcLogP(Double value) {
        return dist.logDensity(value - getOffset()); // scalar
    }

    @Override
    protected List<Double> sample() {
        final double x = sampler.sample() + getOffset();
        return List.of(x); // Returning an immutable result
    }

    @Override
    public Object getApacheDistribution() {
    	return dist;
    }
} // class ChiSquare
