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


import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Log;
import beast.base.evolution.datatype.DataType;
import beast.base.evolution.substitutionmodel.DefaultEigenSystem;
import beast.base.evolution.substitutionmodel.EigenDecomposition;
import beast.base.evolution.substitutionmodel.EigenSystem;
import beast.base.evolution.tree.Node;
import beast.pkgmgmt.BEASTClassLoader;



/**
 * Abstract base for general substitution models that work with any number of states.
 * Provides rate matrix setup, eigendecomposition, and transition probability
 * computation from user-supplied rate parameters and equilibrium frequencies.
 */
@Description("Abstract class for general substituiton models that works for any number of states.")
abstract public class BasicGeneralSubstitutionModel extends Base {

    final public Input<String> eigenSystemClass = new Input<>("eigenSystem", "Name of the class used for creating an EigenSystem", DefaultEigenSystem.class.getName());
    /**
     * a square m_nStates x m_nStates matrix containing current rates  *
     */
    protected double[][] rateMatrix;


    @Override
    public void initAndValidate() {
        super.initAndValidate();
        updateMatrix = true;
        nrOfStates = frequencies.getFreqs().length;

        try {
			eigenSystem = createEigenSystem();
		} catch (SecurityException | ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			throw new IllegalArgumentException(e.getMessage());
		}
        //eigenSystem = new DefaultEigenSystem(m_nStates);

        rateMatrix = new double[nrOfStates][nrOfStates];
        relativeRates = new double[nrOfStates * (nrOfStates - 1)];
        storedRelativeRates = new double[relativeRates.length];
    } // initAndValidate

    /**
     * create an EigenSystem of the class indicated by the eigenSystemClass input 
     * @throws ClassNotFoundException 
     * @throws SecurityException 
     * @throws InvocationTargetException 
     * @throws IllegalArgumentException 
     * @throws IllegalAccessException 
     * @throws InstantiationException *
     */
    protected EigenSystem createEigenSystem() throws SecurityException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Constructor<?>[] ctors = BEASTClassLoader.forName(eigenSystemClass.get()).getDeclaredConstructors();
        Constructor<?> ctor = null;
        for (int i = 0; i < ctors.length; i++) {
            ctor = ctors[i];
            if (ctor.getGenericParameterTypes().length == 1)
                break;
        }
        ctor.setAccessible(true);
        return (EigenSystem) ctor.newInstance(nrOfStates);
    }

    protected double[] relativeRates;
    protected double[] storedRelativeRates;

    protected EigenSystem eigenSystem;

    protected EigenDecomposition eigenDecomposition;
    private EigenDecomposition storedEigenDecomposition;

    protected boolean updateMatrix = true;
    private boolean storedUpdateMatrix = true;

    public void getTransitionProbabilities(Node node, double startTime, double endTime, double rate, double[] matrix, boolean normalized) {
        double distance = (startTime - endTime) * rate;

        int i, j, k;
        double temp;

        // this must be synchronized to avoid being called simultaneously by
        // two different likelihood threads - AJD
        synchronized (this) {
            if (updateMatrix) {
                setupRelativeRates();
                if (normalized) {
                    setupRateMatrix();
                } else {
                    setupRateMatrixUnnormalized();
                }
                eigenDecomposition = eigenSystem.decomposeMatrix(rateMatrix);
                updateMatrix = false;
            }
        }

        // is the following really necessary?
        // implemented a pool of iexp matrices to support multiple threads
        // without creating a new matrix each call. - AJD
        // a quick timing experiment shows no difference - RRB
        double[] iexp = new double[nrOfStates * nrOfStates];
        // Eigen vectors
        double[] Evec = eigenDecomposition.getEigenVectors();
        // inverse Eigen vectors
        double[] Ievc = eigenDecomposition.getInverseEigenVectors();
        // Eigen values
        double[] Eval = eigenDecomposition.getEigenValues();
        for (i = 0; i < nrOfStates; i++) {
            temp = Math.exp(distance * Eval[i]);
            for (j = 0; j < nrOfStates; j++) {
                iexp[i * nrOfStates + j] = Ievc[i * nrOfStates + j] * temp;
            }
        }

        int u = 0;
        for (i = 0; i < nrOfStates; i++) {
            for (j = 0; j < nrOfStates; j++) {
                temp = 0.0;
                for (k = 0; k < nrOfStates; k++) {
                    temp += Evec[i * nrOfStates + k] * iexp[k * nrOfStates + j];
                }

                matrix[u] = Math.abs(temp);
                u++;
            }
        }

    }

    @Override
    public void getTransitionProbabilities(Node node, double startTime, double endTime, double rate, double[] matrix) {
        // get transition probabilities for normalized matrix
        getTransitionProbabilities(node, startTime, endTime, rate, matrix, true);
    } // getTransitionProbabilities

    /**
     * access to (copy of) relative rates *
     */
    public double[] getRelativeRates() {
        return relativeRates.clone();
    }

    /**
     * access to (copy of) rate matrix *
     */
    public double[][] getRateMatrix() {
        return rateMatrix.clone();
    }

    @Override
    public double[] getRateMatrix(Node node) {
        int stateCount = getStateCount();
        double [] rateMatrix = new double[stateCount * stateCount];
        for (int i = 0; i < stateCount; i++) {
        	System.arraycopy(this.rateMatrix[i], 0, rateMatrix, stateCount * i, stateCount);
        }
        return rateMatrix;
    }

    
    abstract public void setupRelativeRates();

    /**
     * sets up rate matrix *
     */
    public void setupRateMatrix() {
        double[] freqs = frequencies.getFreqs();
        for (int i = 0; i < nrOfStates; i++) {
            rateMatrix[i][i] = 0;
            for (int j = 0; j < i; j++) {
                rateMatrix[i][j] = relativeRates[i * (nrOfStates - 1) + j];
            }
            for (int j = i + 1; j < nrOfStates; j++) {
                rateMatrix[i][j] = relativeRates[i * (nrOfStates - 1) + j - 1];
            }
        }
        // bring in frequencies
        for (int i = 0; i < nrOfStates; i++) {
            for (int j = i + 1; j < nrOfStates; j++) {
                rateMatrix[i][j] *= freqs[j];
                rateMatrix[j][i] *= freqs[i];
            }
        }
        // set up diagonal
        for (int i = 0; i < nrOfStates; i++) {
            double sum = 0.0;
            for (int j = 0; j < nrOfStates; j++) {
                if (i != j)
                    sum += rateMatrix[i][j];
            }
            rateMatrix[i][i] = -sum;
        }
        // normalise rate matrix to one expected substitution per unit time
        double subst = 0.0;
        for (int i = 0; i < nrOfStates; i++)
            subst += -rateMatrix[i][i] * freqs[i];

        for (int i = 0; i < nrOfStates; i++) {
            for (int j = 0; j < nrOfStates; j++) {
                rateMatrix[i][j] = rateMatrix[i][j] / subst;
            }
        }
    } // setupRateMatrix

    /**
     * sets up un-normalized rate matrix *
     */
    protected void setupRateMatrixUnnormalized() {}

    /**
     * CalculationNode implementation follows *
     */
    @Override
    public void store() {
        storedUpdateMatrix = updateMatrix;
        if( eigenDecomposition != null ) {
            storedEigenDecomposition = eigenDecomposition.copy();
        }
//        System.arraycopy(relativeRates, 0, storedRelativeRates, 0, relativeRates.length);

        super.store();
    }

    /**
     * Restore the additional stored state
     */
    @Override
    public void restore() {

        updateMatrix = storedUpdateMatrix;

        // To restore all this stuff just swap the pointers...
//        double[] tmp1 = storedRelativeRates;
//        storedRelativeRates = relativeRates;
//        relativeRates = tmp1;
        if( storedEigenDecomposition != null ) {
            EigenDecomposition tmp = storedEigenDecomposition;
            storedEigenDecomposition = eigenDecomposition;
            eigenDecomposition = tmp;
        }
        super.restore();

    }

    @Override
    protected boolean requiresRecalculation() {
        // we only get here if something is dirty
        updateMatrix = true;
        return true;
    }

    
    public void doUpdate() {
    	updateMatrix = true;
    }

    /**
     * This function returns the Eigen vectors.
     *
     * @return the array
     */
    @Override
    public EigenDecomposition getEigenDecomposition(Node node) {
        synchronized (this) {
            if (updateMatrix) {
                setupRelativeRates();
                setupRateMatrix();
                try {
                eigenDecomposition = eigenSystem.decomposeMatrix(rateMatrix);
                }catch(Exception e) {
                	Log.warning(this.getID());
                	Log.warning(this.toString());
                	System.out.print("freqs:\t");
                	for (int i = 0; i < 4; i++) {
                		System.out.print(this.getFrequencies()[i] + "\t");
                	}
                	System.out.println();
                	for (int i = 0; i < 4; i++) {
                		for (int j = 0; j < 4; j++) {
                			System.out.println(rateMatrix[i][j] + "\t"); 
                		}
                		System.out.println();
                	}
                }
                updateMatrix = false;
            }
        }
        return eigenDecomposition;
    }

    @Override
    public boolean canHandleDataType(DataType dataType) {
        return dataType.getStateCount() != Integer.MAX_VALUE;
    }

} // class GeneralSubstitutionModel
