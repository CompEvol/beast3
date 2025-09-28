/*
* File SubstitutionModel.java
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
package beast.base.spec.evolution.substitutionmodel;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.evolution.substitutionmodel.SubstitutionModel;
import beast.base.evolution.tree.Node;
import beast.base.inference.CalculationNode;


    /**
     * basic implementation of a SubstitutionModel bringing together relevant super class*
     */
    @Description(value = "Base implementation of a substitution model.", isInheritable = false)
    public abstract class Base extends CalculationNode implements SubstitutionModel {
        final public Input<Frequencies> frequenciesInput =
                new Input<>("frequencies", "substitution model equilibrium state frequencies", Validate.REQUIRED);

        /**
         * shadows frequencies, or can be set by subst model *
         */
        protected Frequencies frequencies;

        /**
         * number of states *
         */
        protected int nrOfStates;

        @Override
        public void initAndValidate() {
            frequencies = frequenciesInput.get();
        }
        
        @Override
        public double[] getFrequencies() {
            return frequencies.getFreqs();
        }

        @Override
        public int getStateCount() {
            return nrOfStates;
        }


        @Override
        public boolean canReturnComplexDiagonalization() {
            return false;
        }

        @Override
        public double[] getRateMatrix(Node node) {
            return null;
        }

    } // class Base


