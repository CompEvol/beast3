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
package beast.base.inference.operator;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.inference.util.InputUtil;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.parameter.RealScalarParam;


@Description("Scales a parameter or a complete beast.tree (depending on which of the two is specified.")
public class ScaleScalarOperator extends AbstractScaleOp {

    //TODO 1. validate in compile time ?
    // 2. what about if parameter has 0?

//    public final Input<? extends NonNegativeRealScalarParam> parameterInput = new Input<>(
//            "parameter", "if specified, this parameter is scaled");
    public final Input<RealScalarParam<? extends PositiveReal>> parameterInput = new Input<>(
        "parameter", "if specified, this parameter is scaled");

    //    final public Input<Integer> degreesOfFreedomInput = new Input<>("degreesOfFreedom", "Degrees of freedom used when " +
//            "scaleAllIndependently=false and scaleAll=true to override default in calculation of Hasting ratio. " +
//            "Ignored when less than 1, default 0.", 0);
//    final public Input<BooleanParameter> indicatorInput = new Input<>("indicator", "indicates which of the dimension " +
//            "of the parameters can be scaled. Only used when scaleAllIndependently=false and scaleAll=false. If not specified " +
//            "it is assumed all dimensions are allowed to be scaled.");


    //    protected boolean isTreeScaler() {
//        return m_bIsTreeScaler;
//    }


    @Override
    public void initAndValidate() {
        super.initAndValidate();
    }

    /**
     * override this for proposals,
     *
     * @return log of Hastings Ratio, or Double.NEGATIVE_INFINITY if proposal should not be accepted *
     */
    @Override
    public double proposal() {

        try {

            double hastingsRatio;
            final double scale = getScaler();

//            if (m_bIsTreeScaler) {
//
//                final Tree tree = (Tree) InputUtil.get(treeInput, this);
//                if (rootOnlyInput.get()) {
//                    final Node root = tree.getRoot();
//                    final double newHeight = root.getHeight() * scale;
//
//                    if (newHeight < Math.max(root.getLeft().getHeight(), root.getRight().getHeight())) {
//                        return Double.NEGATIVE_INFINITY;
//                    }
//                    root.setHeight(newHeight);
//                    return -Math.log(scale);
//                } else {
//                    // scale the beast.tree
//                    final int internalNodes = tree.scale(scale);
//                    return Math.log(scale) * (internalNodes - 2);
//                }
//            }

            // not a tree scaler, so scale a parameter
//            final boolean scaleAll = scaleAllInput.get();
//            final int specifiedDoF = degreesOfFreedomInput.get();
//            final boolean scaleAllIndependently = scaleAllIndependentlyInput.get();

            // TODO use Bounded interface to validate
            final RealScalarParam param = (RealScalarParam) InputUtil.get(parameterInput, this);

            assert param.getLower() != null && param.getUpper() != null;

//            final int dim = param.getDimension();

//            if (scaleAllIndependently) {
//                // update all dimensions independently.
//                hastingsRatio = 0;
//                final BooleanParameter indicators = indicatorInput.get();
//                if (indicators != null) {
//                    final int dimCount = indicators.getDimension();
//                    final Boolean[] indicator = indicators.getValues();
//                    final boolean impliedOne = dimCount == (dim - 1);
//                    for (int i = 0; i < dim; i++) {
//                        if( (impliedOne && (i == 0 || indicator[i-1])) || (!impliedOne && indicator[i]) )  {
//                            final double scaleOne = getScaler();
//                            final double newValue = scaleOne * param.getValue(i);
//
//                            hastingsRatio -= Math.log(scaleOne);
//
//                            if (outsideBounds(newValue, param)) {
//                                return Double.NEGATIVE_INFINITY;
//                            }
//
//                            param.setValue(i, newValue);
//                        }
//                    }
//                }  else {
//
//                    for (int i = 0; i < dim; i++) {
//
//                        final double scaleOne = getScaler();
//                        final double newValue = scaleOne * param.getValue(i);
//
//                        hastingsRatio -= Math.log(scaleOne);
//
//                        if( outsideBounds(newValue, param) ) {
//                            return Double.NEGATIVE_INFINITY;
//                        }
//
//                        param.setValue(i, newValue);
//                    }
//                }
//            } else if (scaleAll) {
//                // update all dimensions
//                // hasting ratio is dim-2 times of 1dim case. would be nice to have a reference here
//                // for the proof. It is supposed to be somewhere in an Alexei/Nicholes article.
//
//                // all Values assumed independent!
//                final int computedDoF = param.scale(scale);
//                final int usedDoF = (specifiedDoF > 0) ? specifiedDoF : computedDoF ;
//                hastingsRatio = (usedDoF - 2) * Math.log(scale);
//            } else {
                hastingsRatio = -Math.log(scale);

                // which position to scale
//                final int index;
//                final BooleanParameter indicators = indicatorInput.get();
//                if (indicators != null) {
//                    final int dimCount = indicators.getDimension();
//                    final Boolean[] indicator = indicators.getValues();
//                    final boolean impliedOne = dimCount == (dim - 1);
//
//                    // available bit locations. there can be hundreds of them. scan list only once.
//                    final int[] loc = new int[dimCount + 1];
//                    int locIndex = 0;
//
//                    if (impliedOne) {
//                        loc[locIndex] = 0;
//                        ++locIndex;
//                    }
//                    for (int i = 0; i < dimCount; i++) {
//                        if (indicator[i]) {
//                            loc[locIndex] = i + (impliedOne ? 1 : 0);
//                            ++locIndex;
//                        }
//                    }
//
//                    if (locIndex > 0) {
//                        final int rand = Randomizer.nextInt(locIndex);
//                        index = loc[rand];
//                    } else {
//                        return Double.NEGATIVE_INFINITY; // no active indicators
//                    }
//
//                } else {
//                    // any is good
//                    index = Randomizer.nextInt(dim);
//                }

//                final double oldValue = param.getValue(index);
            final double oldValue = param.getValue();

                if (oldValue == 0) {
                    // Error: parameter has value 0 and cannot be scaled
                    return Double.NEGATIVE_INFINITY;
                }

                final double newValue = scale * oldValue;

                if (outsideBounds(newValue, param)) {
                    // reject out of bounds scales
                    return Double.NEGATIVE_INFINITY;
                }

//            param.setValue(index, newValue);
                param.setValue(newValue); // TODO scalar, no index
                // provides a hook for subclasses
                //cleanupOperation(newValue, oldValue);
//            }

            return hastingsRatio;

        } catch (Exception e) {
            // whatever went wrong, we want to abort this operation...
            return Double.NEGATIVE_INFINITY;
        }
    }


} // class ScaleOperator
