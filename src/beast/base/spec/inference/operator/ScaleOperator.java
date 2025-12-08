/*
 * File ScaleOperator.java
 *
 * Copyright (C) 2010 Remco Bouckaert remco@cs.auckland.ac.nz
 *
 * This file is part of BEAST2.
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
package beast.base.spec.inference.operator;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.inference.Scalable;
import beast.base.inference.operator.kernel.KernelOperator;
import beast.base.spec.inference.parameter.BoolVectorParam;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.inference.parameter.RealVectorParam;
import beast.base.util.Randomizer;

import java.text.DecimalFormat;

@Description("Scale operator that finds scale factor according to a Bactrian distribution (Yang & Rodriguez, 2013), "
        + "which is a mixture of two Gaussians: p(x) = 1/2*N(x;-m,1-m^2) + 1/2*N(x;+m,1-m^2) and more efficient than RealRandomWalkOperator")
public class ScaleOperator extends KernelOperator {

    // RealScalar or RealVector
    public final Input<Scalable> parameterInput = new Input<>(
            "parameter", "the real-valued scalar or vector parameter is scaled");

    public final Input<Boolean> scaleAllInput = new Input<>("scaleAll",
            "if true, all elements of a parameter (not beast.tree) are scaled, " +
                    "otherwise one is randomly selected", false);
    public final Input<Boolean> scaleAllIndependentlyInput = new Input<>("scaleAllIndependently",
            "if true, all elements of a parameter (not beast.tree) are scaled with " +
                    "a different factor, otherwise a single factor is used", false);

    final public Input<Integer> degreesOfFreedomInput = new Input<>("degreesOfFreedom",
            "Degrees of freedom used when scaleAllIndependently=false and scaleAll=true " +
                    "to override default in calculation of Hasting ratio. " +
                    "Ignored when less than 1, default 0.", 0);

    final public Input<BoolVectorParam> indicatorInput = new Input<>("indicator",
            "indicates which of the dimension of the parameters can be scaled. " +
                    "Only used when scaleAllIndependently=false and scaleAll=false. " +
                    "If not specified, it is assumed all dimensions are allowed to be scaled.");


    public final Input<Double> scaleFactorInput = new Input<>("scaleFactor",
            "scaling factor: range from 0 to 1. Close to zero is very large jumps, " +
                    "close to 1.0 is very small jumps.", 0.75);
    final public Input<Double> scaleUpperLimit = new Input<>("upper",
            "Upper Limit of scale factor", 1.0 - 1e-8);
    final public Input<Double> scaleLowerLimit = new Input<>("lower",
            "Lower limit of scale factor", 1e-8);

    final public Input<Boolean> optimiseInput = new Input<>("optimise",
            "flag to indicate that the scale factor is automatically changed in order to " +
                    "achieve a good acceptance rate (default true)", true);

    /**
     * shadows input *
     */
    protected double scaleFactor;
    protected double upper;
    protected double lower;


    @Override
    public void initAndValidate() {
        super.initAndValidate();

        if (scaleUpperLimit.get() == 1 - 1e-8) {
            scaleUpperLimit.setValue(10.0, this);
        }

        scaleFactor = scaleFactorInput.get();
        upper = scaleUpperLimit.get();
        lower = scaleLowerLimit.get();

        final BoolVectorParam indicators = indicatorInput.get();

        Scalable param = parameterInput.get();
        if (param instanceof RealVectorParam<?> realVectorParam) {
            if (indicators != null) {
                final int dataDim = realVectorParam.size();
                final int indsDim = indicators.size();
                if (!(indsDim == dataDim || indsDim + 1 == dataDim)) {
                    throw new IllegalArgumentException("indicator dimension not compatible from parameter dimension");
                }
            }
        } else if (param instanceof RealScalarParam<?>) {
            // do nothing
        } else
            throw new IllegalArgumentException("The parameter for ScaleOperator must be a RealScalarParam or RealVectorParam ! But " + param.getClass());
    }

    @Override
    public double proposal() {
        try {

            double logHR = 0;

            // not a tree scaler, so scale a parameter
            final boolean scaleAll = scaleAllInput.get();
            final int specifiedDoF = degreesOfFreedomInput.get();
            final boolean scaleAllIndependently = scaleAllIndependentlyInput.get();

            Scalable param = parameterInput.get();
            if (param instanceof RealVectorParam<?> realVectorParam) {
                final int dim = realVectorParam.size();

                if (scaleAllIndependently) {
                    // update all dimensions independently.
                    logHR = 0;
                    final BoolVectorParam indicators = indicatorInput.get();
                    if (indicators != null) {
                        final int dimCount = indicators.size();
                        final boolean[] indicator = indicators.getValues();
                        final boolean impliedOne = dimCount == (dim - 1);
                        for (int i = 0; i < dim; i++) {
                            if ((impliedOne && (i == 0 || indicator[i - 1])) || (!impliedOne && indicator[i])) {
                                final double scaleOne = getScaler(i, realVectorParam.get(i));
                                final double newValue = scaleOne * realVectorParam.get(i);

                                logHR += Math.log(scaleOne);

                                if (!realVectorParam.isValid(newValue)) {
                                    return Double.NEGATIVE_INFINITY;
                                }

                                realVectorParam.set(i, newValue);
                            }
                        }
                    } else {

                        for (int i = 0; i < dim; i++) {

                            final double scaleOne = getScaler(i, realVectorParam.get(i));
                            final double newValue = scaleOne * realVectorParam.get(i);

                            logHR += Math.log(scaleOne);

                            if (!realVectorParam.isValid(newValue)) {
                                return Double.NEGATIVE_INFINITY;
                            }

                            realVectorParam.set(i, newValue);
                        }
                    }
                } else if (scaleAll) {
                    // update all dimensions
                    // hasting ratio is dim-2 times of 1dim case. would be nice to have a reference here
                    // for the proof. It is supposed to be somewhere in an Alexei/Nicholes article.

                    // all Values assumed independent!
                    final double scale = getScaler(0, realVectorParam.get(0));
                    final int computedDoF = realVectorParam.scale(scale);
                    final int usedDoF = (specifiedDoF > 0) ? specifiedDoF : computedDoF;
                    logHR = usedDoF * Math.log(scale);
                } else {

                    // which position to scale
                    final int index;
                    final BoolVectorParam indicators = indicatorInput.get();
                    if (indicators != null) {
                        final int dimCount = indicators.size();
                        final boolean[] indicator = indicators.getValues();
                        final boolean impliedOne = dimCount == (dim - 1);

                        // available bit locations. there can be hundreds of them. scan list only once.
                        final int[] loc = new int[dimCount + 1];
                        int locIndex = 0;

                        if (impliedOne) {
                            loc[locIndex] = 0;
                            ++locIndex;
                        }
                        for (int i = 0; i < dimCount; i++) {
                            if (indicator[i]) {
                                loc[locIndex] = i + (impliedOne ? 1 : 0);
                                ++locIndex;
                            }
                        }

                        if (locIndex > 0) {
                            final int rand = Randomizer.nextInt(locIndex);
                            index = loc[rand];
                        } else {
                            return Double.NEGATIVE_INFINITY; // no active indicators
                        }

                    } else {
                        // any is good
                        index = Randomizer.nextInt(dim);
                    }

                    final double oldValue = realVectorParam.get(index);

                    if (oldValue == 0) {
                        // Error: parameter has value 0 and cannot be scaled
                        return Double.NEGATIVE_INFINITY;
                    }

                    final double scale = getScaler(index, oldValue);
                    logHR = Math.log(scale);

                    final double newValue = scale * oldValue;

                    if (!realVectorParam.isValid(newValue)) {
                        // reject out of bounds scales
                        return Double.NEGATIVE_INFINITY;
                    }

                    realVectorParam.set(index, newValue);
                    // provides a hook for subclasses
                    //cleanupOperation(newValue, oldValue);
                }

            } else if (param instanceof RealScalarParam<?> realScalarParam) {

                final double scale = getScaler(0);
                // this set new value
                int dim = realScalarParam.scale(scale);

                if (! realScalarParam.withinBounds(realScalarParam.get()) ) {
                    // reject out of bounds scales
                    return Double.NEGATIVE_INFINITY;
                }
                // hastings ratio
                logHR = Math.log(scale);
            }
            return logHR;

        } catch (Exception e) {
            // whatever went wrong, we want to abort this operation...
            return Double.NEGATIVE_INFINITY;
        }
    }

    protected double getScaler(int i, double value) {
        return kernelDistribution.getScaler(i, value, getCoercableParameterValue());
    }
    protected double getScaler(int i) {
        return getScaler(i, Double.NaN);
    }

    @Override
    public void optimize(double logAlpha) {
        // must be overridden by operator implementation to have an effect
        if (optimiseInput.get()) {
            double delta = calcDelta(logAlpha);
            double scaleFactor = getCoercableParameterValue();
            delta += Math.log(scaleFactor);
            scaleFactor = Math.exp(delta);
            setCoercableParameterValue(scaleFactor);
        }
    }

    @Override
    public double getCoercableParameterValue() {
        return scaleFactor;
    }

    @Override
    public void setCoercableParameterValue(final double value) {
        scaleFactor = Math.max(Math.min(value, upper), lower);
    }

    @Override
    public double getTargetAcceptanceProbability() {
        return 0.3;
    }


    @Override
    public String getPerformanceSuggestion() {
        double prob = m_nNrAccepted / (m_nNrAccepted + m_nNrRejected + 0.0);
        double targetProb = getTargetAcceptanceProbability();

        double ratio = prob / targetProb;
        if (ratio > 2.0) ratio = 2.0;
        if (ratio < 0.5) ratio = 0.5;

        // new scale factor
        double newWindowSize = getCoercableParameterValue() * ratio;

        DecimalFormat formatter = new DecimalFormat("#.###");
        if (prob < 0.10 || prob > 0.40) {
            return "Try setting scale factor to about " + formatter.format(newWindowSize);
        } else return "";
    }

} // class ScaleOperator
