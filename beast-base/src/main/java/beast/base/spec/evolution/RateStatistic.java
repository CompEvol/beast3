/*
 * RateStatistic.java
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

package beast.base.spec.evolution;


import beast.base.core.BEASTObject;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.core.Loggable;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.evolution.branchratemodel.Base;
import beast.base.spec.evolution.likelihood.GenericTreeLikelihood;
import beast.base.spec.type.RealVector;
import beast.base.util.DiscreteStatistics;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


@Description("A statistic that tracks the mean, variance and coefficent of variation of rates. " +
        "It has three dimensions, one for each statistic.")
public class RateStatistic extends BEASTObject implements Loggable, RealVector<PositiveReal> {
	
    final public Input<GenericTreeLikelihood> likelihoodInput = new Input<>("treeLikelihood", "TreeLikelihood containing branch rate model that provides rates for a tree");
    final public Input<Base> branchRateModelInput = new Input<>("branchratemodel", "model that provides rates for a tree", Validate.XOR, likelihoodInput);
    final public Input<Tree> treeInput = new Input<>("tree", "tree for which the rates apply", Validate.REQUIRED);
    final public Input<Boolean> internalInput = new Input<>("internal", "consider internal nodes, default true", true);
    final public Input<Boolean> externalInput = new Input<>("external", "consider external nodes, default true", true);

    private Tree tree = null;
    private Base branchRateModel = null;
    private boolean internal = true;
    private boolean external = true;

    // array index
    final static int MEAN = 0;
    final static int VARIANCE = 1;
    final static int COEFFICIENT_OF_VARIATION = 2;

    @Override
    public void initAndValidate() {
        tree = treeInput.get();
        branchRateModel = branchRateModelInput.get();
        if (branchRateModel == null) {
            branchRateModel = likelihoodInput.get().branchRateModelInput.get();
        }
        this.internal = internalInput.get();
        this.external = externalInput.get();
    }

    /**
     * calculate the three statistics from scratch *
     */
    public double[] calcValues() {
        int length = 0;
        int offset = 0;

        final int nrOfLeafs = tree.getLeafNodeCount();

        if (external) {
            length += nrOfLeafs;
        }
        if (internal) {
            length += tree.getInternalNodeCount() - 1;
        }

        final double[] rates = new double[length];
        // need those only for mean
        final double[] branchLengths = new double[length];

        final Node[] nodes = tree.getNodesAsArray();

        /** handle leaf nodes **/
        if (external) {
            for (int i = 0; i < nrOfLeafs; i++) {
                final Node child = nodes[i];
                final Node parent = child.getParent();
                branchLengths[i] = parent.getHeight() - child.getHeight();
                rates[i] = branchRateModel.getRateForBranch(child);
            }
            offset = nrOfLeafs;
        }

        /** handle internal nodes **/
        if (internal) {
            final int n = tree.getNodeCount();
            int k = offset;
            for (int i = nrOfLeafs; i < n; i++) {
                final Node child = nodes[i];
                if (!child.isRoot()) {
                    final Node parent = child.getParent();
                    branchLengths[k] = parent.getHeight() - child.getHeight();
                    rates[k] = branchRateModel.getRateForBranch(child);
                    k++;
                }
            }
        }

        final double[] values = new double[3];
        double totalWeightedRate = 0.0;
        double totalTreeLength = 0.0;
        for (int i = 0; i < rates.length; i++) {
            totalWeightedRate += rates[i] * branchLengths[i];
            totalTreeLength += branchLengths[i];
        }
        values[MEAN] = totalWeightedRate / totalTreeLength;
        // compute mean/variance once and reuse: DiscreteStatistics.variance(rates) alone
        // would recompute mean(rates) internally, duplicating both passes done here
        final double mean = DiscreteStatistics.mean(rates);
        values[VARIANCE] = DiscreteStatistics.variance(rates, mean);
        values[COEFFICIENT_OF_VARIATION] = Math.sqrt(values[VARIANCE]) / mean;
        return values;
    }


    /**
     * Valuable implementation *
     */

    @Deprecated
    public int getDimension() {
        return size();
    }

    @Deprecated
    public double getArrayValue() {
        return get(0);
    }

    @Deprecated
    public double getArrayValue(final int dim) {
        return get(dim);
    }

    // new API

    @Override
    public int size() {
        return 3;
    }

    @Override
    public double get(int i) {
        if (i < 0 || i >= size()) {
            throw new IllegalArgumentException();
        }
        return calcValues()[i];
    }

    @Override
    public List<Double> getElements() {
        return Arrays.stream(calcValues()).boxed().collect(Collectors.toList());
    }

    @Override
    public PositiveReal getDomain() {
        return PositiveReal.INSTANCE;
    }

    /**
     * Loggable implementation *
     */

    @Override
    public void init(final PrintStream out) {
        String id = getID();
        if (id == null) {
            id = "";
        }
        out.print(id + ".mean\t" + id + ".variance\t" + id + ".coefficientOfVariation\t");
    }


    @Override
    public void log(final long sample, final PrintStream out) {
        final double[] values = calcValues();
        out.print(values[0] + "\t" + values[1] + "\t" + values[2] + "\t");
    }


    @Override
    public void close(final PrintStream out) {
        // nothing to do
    }

}
