package beast.base.evolution.operator.kernel;

/*
 * Moved from BactrianSubtreeSlide.java, which carries:
 *
 * Copyright (C) 2010 Remco Bouckaert remco@cs.auckland.ac.nz
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

import java.util.ArrayList;
import java.util.List;

import beast.base.core.Description;
import beast.base.evolution.operator.TreeOperator;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.inference.util.InputUtil;
import beast.base.util.Randomizer;

/**
 * The subtree slide move, apart from how the moved node's new height is proposed.
 *
 * <p>A random non-root node {@code i} is chosen; its parent {@code p} is moved to a new height,
 * carrying the subtree rooted at {@code i}. If {@code p} moves above its parent it is reattached
 * higher up (possibly as the new root); if it moves below its other child {@code CiP} it is
 * reattached to an edge of {@code CiP}'s subtree, chosen uniformly among those at the new height.
 * Subclasses propose the new height in {@link #proposeHeight(Node, Node)}; the Hastings ratio is
 * that proposal's log-Jacobian plus the reattachment term ({@code -log(#sources)} up,
 * {@code +log(#destinations)} down, {@code 0} without a topology change).</p>
 *
 * <p>Not for sampled-ancestor trees: if {@code CiP} is a zero-length sampled-ancestor tip,
 * {@code p} can still move and turn the sampled ancestor into an ordinary tip.</p>
 */
@Description("Subtree slide move, apart from how the new height of the moved node is proposed.")
public abstract class AbstractSubtreeSlide extends TreeOperator {

    /**
     * A proposed new height for the moved node, with the log-Jacobian of the move that produced it.
     */
    protected record HeightProposal(double height, double logJacobian) {
    }

    /**
     * Proposes a new height for {@code p}, which carries the subtree rooted at its child {@code i}.
     *
     * @return the proposal, or {@code null} to reject the move
     */
    protected abstract HeightProposal proposeHeight(Node i, Node p);

    /**
     * Do a probabilistic subtree slide move.
     *
     * @return log of Hastings Ratio, or Double.NEGATIVE_INFINITY if proposal should not be accepted
     */
    @Override
    public double proposal() {
        final Tree tree = (Tree) InputUtil.get(treeInput, this);
        final boolean markClades = markCladesInput.get();

        // 1. choose a random node avoiding root
        final int nodeCount = tree.getNodeCount();
        Node i;
        do {
            i = tree.getNode(Randomizer.nextInt(nodeCount));
        } while (i.isRoot());

        final Node p = i.getParent();
        final Node CiP = getOtherChild(p, i);
        final Node PiP = p.getParent();

        // 2. propose the new height
        final HeightProposal proposal = proposeHeight(i, p);
        if (proposal == null) {
            return Double.NEGATIVE_INFINITY;
        }
        final double oldHeight = p.getHeight();
        final double newHeight = proposal.height();
        double logq = proposal.logJacobian();

        // 3. if the move is up
        if (newHeight > oldHeight) {

            // 3.1 if the topology will change
            if (PiP != null && PiP.getHeight() < newHeight) {
                // find new parent
                Node newParent = PiP;
                Node newChild = p;
                while (newParent.getHeight() < newHeight) {
                    newChild = newParent;
                    if (markClades) newParent.makeDirty(Tree.IS_FILTHY); // JH
                    newParent = newParent.getParent();
                    if (newParent == null) break;
                }
                // the moved node 'p' would become a child of 'newParent'

                // 3.1.1 if creating a new root
                if (newChild.isRoot()) {
                    replace(p, CiP, newChild);
                    replace(PiP, p, CiP);

                    p.setParent(null);
                    tree.setRoot(p);
                }
                // 3.1.2 no new root
                else {
                    replace(p, CiP, newChild);
                    replace(PiP, p, CiP);
                    replace(newParent, newChild, p);
                }

                p.setHeight(newHeight);

                // 3.1.3 count the hypothetical sources of this destination.
                final int possibleSources = intersectingEdges(newChild, oldHeight, null);
                logq -= Math.log(possibleSources);

            } else {
                // just change the node height
                p.setHeight(newHeight);
            }
        }
        // 4 if we are sliding the subtree down.
        else {

            // 4.0 is it a valid move?
            if (i.getHeight() > newHeight) {
                return Double.NEGATIVE_INFINITY;
            }

            // 4.1 will the move change the topology
            if (CiP.getHeight() > newHeight) {

                final List<Node> newChildren = new ArrayList<>();
                final int possibleDestinations = intersectingEdges(CiP, newHeight, newChildren);

                // if no valid destinations then return a failure
                if (newChildren.size() == 0) {
                    return Double.NEGATIVE_INFINITY;
                }

                // pick a random parent/child destination edge uniformly from options
                final int childIndex = Randomizer.nextInt(newChildren.size());
                final Node newChild = newChildren.get(childIndex);
                final Node newParent = newChild.getParent();

                // 4.1.1 if p was root
                if (p.isRoot()) {
                    // new root is CiP
                    replace(p, CiP, newChild);
                    replace(newParent, newChild, p);

                    CiP.setParent(null);
                    tree.setRoot(CiP);

                } else {
                    replace(p, CiP, newChild);
                    replace(PiP, p, CiP);
                    replace(newParent, newChild, p);
                }

                p.setHeight(newHeight);
                if (markClades) {
                    // make dirty the path from the (down) moved node back up to former parent.
                    Node n = p;
                    while (n != CiP) {
                        n.makeDirty(Tree.IS_FILTHY); // JH
                        n = n.getParent();
                    }
                }

                logq += Math.log(possibleDestinations);
            } else {
                p.setHeight(newHeight);
            }
        }
        return logq;
    }

    protected int intersectingEdges(Node node, double height, List<Node> directChildren) {
        final Node parent = node.getParent();

        if (parent.getHeight() < height) return 0;

        if (node.getHeight() < height) {
            if (directChildren != null) directChildren.add(node);
            return 1;
        }

        if (node.isLeaf()) {
            // TODO: verify that this makes sense
            return 0;
        } else {
            return intersectingEdges(node.getLeft(), height, directChildren) +
                    intersectingEdges(node.getRight(), height, directChildren);
        }
    }
}
