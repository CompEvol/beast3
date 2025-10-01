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
package beast.base.spec.inference.operator.scale;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.inference.parameter.RealScalarParam;


@Description("Scales a parameter or a complete beast.tree (depending on which of the two is specified.")
public class ScaleScalarOperator extends AbstractScale {

    public final Input<RealScalarParam<? extends PositiveReal>> parameterInput = new Input<>(
            "parameter", "if specified, this parameter is scaled");

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

            // TODO seem not to require InputUtil.get(...)
            final RealScalarParam param = parameterInput.get();

            // TODO this would not happened after merge Parameter with Bounded interface
            assert param.getLower() != null && param.getUpper() != null;

            final double oldValue = param.get();
            // TODO not sure if still required, when validation is working
            if (oldValue == 0) {
                // Error: parameter has value 0 and cannot be scaled
                return Double.NEGATIVE_INFINITY;
            }

            final double scale = getScaler();
            final double newValue = scale * oldValue;

            // TODO all validations for a value should be in one place eventually
            if (! param.isValid(newValue)) {
                // reject out of bounds scales
                return Double.NEGATIVE_INFINITY;
            }

            // scalar, no index
            param.set(newValue);
            // provides a hook for subclasses
            //cleanupOperation(newValue, oldValue);

            // Hastings Ratio
            return -Math.log(scale);

        } catch (Exception e) {
            // whatever went wrong, we want to abort this operation...
            return Double.NEGATIVE_INFINITY;
        }
    }

} // class ScaleOperator
