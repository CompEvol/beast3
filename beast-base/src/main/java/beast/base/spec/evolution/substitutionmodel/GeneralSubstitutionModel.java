/*
* File GeneralSubstitutionModel.java
*
* Copyright (C) 2010 Remco Bouckaert remco@cs.auckland.ac.nz
*
* This file is not copyright Remco! It is copied from BEAST 1.
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
package beast.base.spec.evolution.substitutionmodel;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.spec.domain.NonNegativeReal;
import beast.base.spec.type.RealVector;

import java.lang.reflect.InvocationTargetException;



/**
 * General substitution model with user-specified rate matrix.
 * Extends {@link BasicGeneralSubstitutionModel} to handle eigendecomposition
 * and transition probability calculations for any number of states.
 */
@Description("Specifies transition probability matrix with no restrictions on the rates other " +
        "than that one of the is equal to one and the others are specified relative to " +
        "this unit rate. Works for any number of states.")
public class GeneralSubstitutionModel extends BasicGeneralSubstitutionModel {
    // GeneralSubstModel should allow zero rates -- for example, the stochastic variable selection model in DTA has mostly zero rates.
    final public Input<RealVector<NonNegativeReal>> ratesInput =
            new Input<>("rates", "Rate parameter which defines the transition rate matrix. " +
                    "Only the off-diagonal entries need to be specified (diagonal makes row sum to zero in a " +
                    "rate matrix). Entry i specifies the rate from floor(i/(n-1)) to i%(n-1)+delta where " +
                    "n is the number of states and delta=1 if floor(i/(n-1)) <= i%(n-1) and 0 otherwise.", Validate.REQUIRED);

    @Override
    public void initAndValidate() {
        super.initAndValidate();
        updateMatrix = true;
        nrOfStates = frequencies.getFreqs().length;
        if (ratesInput.get().size() != nrOfStates * (nrOfStates - 1)) {
            throw new IllegalArgumentException("Dimension of input 'rates' is " + ratesInput.get().size() + " but a " +
                    "rate matrix of dimension " + nrOfStates + "x" + (nrOfStates - 1) + "=" + nrOfStates * (nrOfStates - 1) + " was " +
                    "expected");
        }

        try {
			eigenSystem = createEigenSystem();
		} catch (SecurityException | ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			throw new IllegalArgumentException(e.getMessage());
		}
        //eigenSystem = new DefaultEigenSystem(m_nStates);

        rateMatrix = new double[nrOfStates][nrOfStates];
        relativeRates = new double[ratesInput.get().size()];
        storedRelativeRates = new double[ratesInput.get().size()];
    } // initAndValidate



    @Override
    public void setupRelativeRates() {
    	RealVector<NonNegativeReal> rates = this.ratesInput.get();
        for (int i = 0; i < rates.size(); i++) {
            relativeRates[i] = rates.get(i);
        }
    }

} // class GeneralSubstitutionModel
