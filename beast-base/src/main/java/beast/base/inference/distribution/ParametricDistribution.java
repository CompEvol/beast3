/*
 * ParametricDistributionModel.java
 *
 * Copyright (C) 2002-2006 Alexei Drummond and Andrew Rambaut
 *
 * This file is part of BEAST.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership and licensing.
 *
 * BEAST is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 *  BEAST is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with BEAST; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA  02110-1301  USA
 */

package beast.base.inference.distribution;

import beast.base.core.Description;
import beast.base.core.Function;
import beast.base.core.Input;
import beast.base.inference.CalculationNode;
import beast.base.spec.type.RealScalar;
import beast.base.spec.type.RealVector;
import beast.base.util.Randomizer;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.statistics.distribution.ContinuousDistribution;
import org.apache.commons.statistics.distribution.DiscreteDistribution;

/**
 * A class that describes a parametric distribution
 *
 * * (FIXME) cumulative functions disregard offset. Serious bug if they are used.
 *
 * @author Alexei Drummond
 */

/**
 * @deprecated replaced by {@link beast.base.spec.inference.distribution.TensorDistribution}
 */
@Deprecated
@Description("A class that describes a parametric distribution, that is, a distribution that takes some " +
        "parameters/valuables as inputs and can produce (cumulative) densities and inverse " +
        "cumulative densities.")
public abstract class ParametricDistribution extends CalculationNode implements ContinuousDistribution {
    public final Input<Double> offsetInput = new Input<>("offset", "offset of origin (defaults to 0)", 0.0);

    abstract public Object getDistribution();

    /**
     * Calculate log probability of a valuable x for this distribution.
     * If x is multidimensional, the components of x are assumed to be independent,
     * so the sum of log probabilities of all elements of x is returned as the prior.
     */
    public double calcLogP(final Function fun) {
        final double offset = offsetInput.get();
        double logP = 0;
        for (int i = 0; i < fun.getDimension(); i++) {
            final double x = fun.getArrayValue(i);
            logP += logDensity(x, offset);
        }
        return logP;
    }

    public double calcLogP(final RealScalar<?> fun) {
        final double offset = offsetInput.get();
        double logP = 0;
        final double x = fun.get();
        logP += logDensity(x, offset);
        return logP;
    }

    public double calcLogP(final RealVector<?> fun) {
        final double offset = offsetInput.get();
        double logP = 0;
        for (int i = 0; i < fun.size(); i++) {
            final double x = fun.get(i);
            logP += logDensity(x, offset);
        }
        return logP;
    }
    /*
     * This implementation is only suitable for univariate distributions.
     * Must be overwritten for multivariate ones.
     * @size sample size = number of samples to produce
     */
    public Double[][] sample(final int size) {
        final Double[][] sample = new Double[size][];
        for (int i = 0; i < sample.length; i++) {
            final double p = Randomizer.nextDouble();
            sample[i] = new Double[]{inverseCumulativeProbability(p)};
        }
        return sample;

    }

    /**
     * For this distribution, X, this method returns x such that {@code P(X < x) = p}.
     *
     * @param p the cumulative probability.
     * @return x.
     */
    @Override
	public double inverseCumulativeProbability(final double p) {
        final Object dist = getDistribution();
        double offset = getOffset();
        if (dist instanceof ContinuousDistribution) {
            return offset + ((ContinuousDistribution) dist).inverseCumulativeProbability(p);
        } else if (dist instanceof DiscreteDistribution) {
            return offset + ((DiscreteDistribution)dist).inverseCumulativeProbability(p);
        }
        return 0.0;
    }

    /**
     * Return the probability density for a particular point.
     * NB this does not take offset in account
     *
     * @param x The point at which the density should be computed.
     * @return The pdf at point x.
     */
    @Override
	public double density(double x) {
        final double offset = getOffset();
 //       if( x >= offset ) {
            x -= offset;
            final Object dist = getDistribution();
            if (dist instanceof ContinuousDistribution) {
                return ((ContinuousDistribution) dist).density(x);
            } else if (dist instanceof DiscreteDistribution) {
                return ((DiscreteDistribution) dist).probability((int) x);
            }
   //     }
        return 0.0;
    }

    private double logDensity(double x, final double offset) {
   //     if( x >= offset ) {
            x -= offset;
            final Object dist = getDistribution();
            if (dist instanceof ContinuousDistribution) {
                return ((ContinuousDistribution) dist).logDensity(x);
            } else if (dist instanceof DiscreteDistribution) {
                final double probability = ((DiscreteDistribution) dist).probability((int) x);
                if( probability > 0 ) {
                    return Math.log(probability);
                }
            }
  //      }
        return Double.NEGATIVE_INFINITY;
    }

    @Override
	public double logDensity(final double x) {
        return logDensity(x, getOffset());
    }

    /**
     * For a random variable X whose values are distributed according
     * to this distribution, this method returns P(X &le; x).  In other words,
     * this method represents the  (cumulative) distribution function, or
     * CDF, for this distribution.
     *
     * @param x the value at which the distribution function is evaluated.
     * @return the probability that a random variable with this
     *         distribution takes a value less than or equal to <code>x</code>
     */
    @Override
	public double cumulativeProbability(final double x) {
        final Object dist = getDistribution();
        if (dist instanceof ContinuousDistribution) {
            return ((ContinuousDistribution) dist).cumulativeProbability(x);
        } else if (dist instanceof DiscreteDistribution) {
            return ((DiscreteDistribution) dist).cumulativeProbability((int) x);
        }
        return 0.0;
    }

    /**
     * For a random variable X whose values are distributed according
     * to this distribution, this method returns P(x0 &le; X &le; x1).
     *
     * @param x0 the (inclusive) lower bound
     * @param x1 the (inclusive) upper bound
     * @return the probability that a random variable with this distribution
     *         will take a value between <code>x0</code> and <code>x1</code>,
     *         including the endpoints
     * @throws IllegalArgumentException if {@code x0 > x1}
     */
	public double cumulativeProbability(final double x0, final double x1) {
        return cumulativeProbability(x1) - cumulativeProbability(x0);
    }

    /**
     * @return  offset of distribution.
     */
    public double getOffset() {
        return offsetInput.get();
    }

    protected double getMeanWithoutOffset() {
        throw new RuntimeException("Not implemented yet");
    }

    /** returns mean of distribution, if implemented **/
    @Override
    public double getMean() {
        return getMeanWithoutOffset() + getOffset();
    }

    @Override
    public double getVariance() {
        final Object dist = getDistribution();
        if (dist instanceof ContinuousDistribution) {
            return ((ContinuousDistribution) dist).getVariance();
        }
        return Double.NaN;
    }

    @Override
    public double getSupportLowerBound() {
        final Object dist = getDistribution();
        if (dist instanceof ContinuousDistribution) {
            return ((ContinuousDistribution) dist).getSupportLowerBound();
        }
        return Double.NEGATIVE_INFINITY;
    }

    @Override
    public double getSupportUpperBound() {
        final Object dist = getDistribution();
        if (dist instanceof ContinuousDistribution) {
            return ((ContinuousDistribution) dist).getSupportUpperBound();
        }
        return Double.POSITIVE_INFINITY;
    }

    @Override
    public ContinuousDistribution.Sampler createSampler(UniformRandomProvider rng) {
        final Object dist = getDistribution();
        if (dist instanceof ContinuousDistribution) {
            return ((ContinuousDistribution) dist).createSampler(rng);
        }
        return () -> inverseCumulativeProbability(rng.nextDouble());
    }

    /**
     * @return true if the distribution is an integer distribution
     */
    public boolean isIntegerDistribution() {
        return getDistribution() instanceof DiscreteDistribution;
    }
}
