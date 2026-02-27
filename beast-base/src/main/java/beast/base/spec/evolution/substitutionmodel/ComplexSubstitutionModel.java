/*
 * ComplexSubstitutionModel.java
 *
 * Copyright (c) 2002-2015 Alexei Drummond, Andrew Rambaut and Marc Suchard
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


import java.util.Arrays;

import beast.base.core.Citation;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.evolution.datatype.DataType;
import beast.base.evolution.substitutionmodel.ComplexColtEigenSystem;
import beast.base.evolution.substitutionmodel.EigenSystem;
import beast.base.evolution.tree.Node;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.type.RealVector;

/**
 * <b>A general irreversible class for any
 * data type; allows complex eigenstructures.</b>
 * Complex-diagonalizable, irreversible substitution model.
 * Extends {@link BasicComplexSubstitutionModel} to use complex eigendecomposition
 * for non-reversible rate matrices where eigenvalues may be complex.
 *
 * @author Marc Suchard
 */

@Description("Complex-diagonalizable, irreversible substitution model")
@Citation(value = "Edwards, C. J., Suchard, M. A., Lemey, P., ... & Valdiosera, C. E. (2011).\n" +
        "Ancient hybridization and an Irish origin for the modern polar bear matriline.\n" +
        "Current Biology, 21(15), 1251-1258.",
        year = 2011, firstAuthorSurname = "Edwards", DOI="10.1016/j.cub.2011.05.058")
public class ComplexSubstitutionModel extends BasicComplexSubstitutionModel {
    final public Input<RealVector<PositiveReal>> ratesInput =
            new Input<>("rates", "Rate parameter which defines the transition rate matrix. " +
                    "Only the off-diagonal entries need to be specified (diagonal makes row sum to zero in a " +
                    "rate matrix). Entry i specifies the rate from floor(i/(n-1)) to i%(n-1)+delta where " +
                    "n is the number of states and delta=1 if floor(i/(n-1)) <= i%(n-1) and 0 otherwise.", Validate.REQUIRED);
	
	@Override
	public void initAndValidate() {
        updateMatrix = true;
        frequencies = frequenciesInput.get();
        nrOfStates = frequencies.getFreqs().length;
        
        try {
			eigenSystem = createEigenSystem();
		} catch (SecurityException e) {
			throw new IllegalArgumentException(e.getMessage());
		}

        rateMatrix = new double[nrOfStates][nrOfStates];
        relativeRates = new double[ratesInput.get().size()];
        storedRelativeRates = new double[ratesInput.get().size()];
	}
	

	@Override
	public void setupRelativeRates() {
        for (int i = 0; i < relativeRates.length; i++)
            relativeRates[i] = ratesInput.get().get(i);
	}

}