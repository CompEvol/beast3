/*
* File BitFlipOperator.java
*
* Copyright (C) 2010 Joseph Heled jheled@gmail.com
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

package beast.base.spec.inference.operator;

import beast.base.core.BEASTInterface;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.inference.CalculationNode;
import beast.base.inference.Operator;
import beast.base.spec.domain.Bool;
import beast.base.spec.inference.distribution.IndicatorPrior;
import beast.base.spec.inference.parameter.BoolScalarParam;
import beast.base.spec.inference.parameter.BoolVectorParam;
import beast.base.spec.type.Tensor;
import beast.base.util.Randomizer;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * @author Joseph Heled
 */

/**
 * Operator that flips a single randomly chosen bit in a boolean vector parameter.
 * The Hastings ratio ensures correct sampling when combined with an indicator
 * variable that controls which subset of parameters is active.
 */
@Description("Flip one bit in an array of boolean bits. The hastings ratio is designed so that all subsets of vectors with the" +
        " same number of 'on' bits are equiprobable.")
public class BitFlipOperator extends Operator {
    final public Input<Boolean> uniformInput = new Input<>("uniform", "when on, total probability of combinations with k" +
            " 'on' bits is equal. Otherwise uniform on all combinations. Default: true, unless the parameter has" +
            " a prior that is the whole prior on the indicators, such as CountPrior, which already makes all" +
            " placements of k 'on' bits equally probable; then false, and true is an error");

    final public Input<Tensor<? extends Bool,Boolean>> parameterInput = new Input<>("parameter", "the parameter to operate a flip on.", Validate.REQUIRED);

    private boolean usesPriorOnSum = true;

    /** whether usesPriorOnSum has been worked out from the parameter's priors */
    private boolean resolved = false;

    @Override
	public void initAndValidate() {
        if (!(parameterInput.get() instanceof BoolVectorParam) && !(parameterInput.get() instanceof BoolScalarParam)) {
        	throw new IllegalArgumentException("The parameter must be a BoolVectorParam or BoolScalarParam");
        }
        // from XML, the priors of the parameter are known now, so a conflict is reported at
        // load; from Java they may be added later, so this is done again on the first proposal
        resolved = false;
        resolveUniform();
        resolved = false;
    }

    /**
     * Works out whether the Hastings ratio makes placements of the same number of 'on' bits
     * equally probable. An {@link IndicatorPrior}, e.g. CountPrior, is the whole prior on the
     * indicators, so that term would be applied twice (#231).
     */
    private void resolveUniform() {
        IndicatorPrior prior = indicatorPrior((BEASTInterface) parameterInput.get());
        Boolean uniform = uniformInput.get();
        if (uniform == null) {
            usesPriorOnSum = prior == null;
        } else if (uniform && prior != null) {
            throw new IllegalArgumentException("BitFlipOperator " + getID() + " has uniform=\"true\", but "
                    + "its parameter " + ((BEASTInterface) parameterInput.get()).getID() + " has the prior "
                    + prior.getClass().getSimpleName() + " " + ((BEASTInterface) prior).getID()
                    + ", which already makes all placements of the same number of indicators "
                    + "equally probable. Set uniform=\"false\" on the operator, or leave it out.");
        } else {
            usesPriorOnSum = uniform;
        }
        resolved = true;
    }

    /**
     * The indicator prior of a parameter: among its outputs, or the outputs of a view of it
     * (another tensor computed from it), or null if it has none.
     */
    private static IndicatorPrior indicatorPrior(BEASTInterface parameter) {
        Set<BEASTInterface> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        Deque<BEASTInterface> todo = new ArrayDeque<>();
        todo.add(parameter);
        while (!todo.isEmpty()) {
            BEASTInterface o = todo.poll();
            if (!seen.add(o)) {
                continue;
            }
            for (BEASTInterface output : o.getOutputs()) {
                if (output instanceof IndicatorPrior prior) {
                    return prior;
                }
                if (output instanceof Tensor<?, ?> && output instanceof CalculationNode) {
                    todo.add(output);
                }
            }
        }
        return null;
    }

    /**
     * Change the parameter and return the hastings ratio.
     * Flip (Switch a 0 to 1 or 1 to 0) for a random bit in a bit vector.
     * Return the hastings ratio which makes all subsets of vectors with the same number of 1 bits
     * equiprobable, unless !usesPriorOnSum , then all configurations are equiprobable
     */

    @Override
    public double proposal() {

    	if (!resolved) {
    		resolveUniform();
    	}
    	Tensor<?,Boolean> param = parameterInput.get();

        final int dim = param.size();

        double sum = 0.0;
        if (usesPriorOnSum) {
            for (int i = 0; i < dim; i++) {
                if (param.get(i)) sum += 1;
            }
        }

        final int pos = Randomizer.nextInt(dim);

        final boolean value = param.get(pos);

        double logq = 0.0;
        if (!value) {
        	setValue(pos, true);

            if (usesPriorOnSum) {
                logq = -Math.log((dim - sum) / (sum + 1));
            }

        } else {
            //assert value;

        	setValue(pos, false);
            if (usesPriorOnSum) {
                logq = -Math.log(sum / (dim - sum + 1));
            }
        }
        return logq;
    }
    
	private void setValue(int i, boolean value) {
  	 if (parameterInput.get() instanceof BoolVectorParam) {
  		 final BoolVectorParam p = (BoolVectorParam)parameterInput.get();
  		 p.set(i, value);
  	 }else {
  		 final BoolScalarParam p = (BoolScalarParam)parameterInput.get();
  		 p.set(value);
  	 }
  }
}

