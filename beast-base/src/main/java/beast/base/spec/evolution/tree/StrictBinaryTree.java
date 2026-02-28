/*
* File Node.java
*
* Copyright (C) 2010 Remco Bouckaert remco@cs.auckland.ac.nz
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
package beast.base.spec.evolution.tree;

import beast.base.core.Description;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.TreeParser;

/**
 * A phylogenetic tree restricted to strict binary topology: every internal
 * node has exactly two children, with no sampled ancestors or polytomies.
 */
@Description("A strict binary tree (no sampled ancestors or polytomies).")
public class StrictBinaryTree extends Tree {

    public StrictBinaryTree() {
    }

    /**
     * This constructor wraps a new root node in a <code>StrictBinaryTree</code>
     * object, which does not copy the tree. To copy a
     * <code>StrictBinaryTree</code>, either {@link StrictBinaryTree#copy()
     * StrictBinaryTree#copy} or {@link Node#copy() Node#copy} can be used instead, which
     * performs true deep copy. For example,
     * <code>new StrictBinaryTree(oldTree.getRoot().copy())</code> which is also
     * equivalent to <code>oldTree.copy()</code>.
     *
     * @param rootNode root <code>Node</code>
     */
    public StrictBinaryTree(final Node rootNode) {
        super(rootNode);
    }

    /**
     * Construct a tree from newick string -- will not automatically adjust tips to
     * zero.
     */
    public StrictBinaryTree(final String newick) {
        super(new TreeParser(newick).getRoot());
    }

    /**
     * Check whether the object represents a valid StrictBinaryTree.
     */
    public boolean isValid() {
        for (Node n : root.getAllChildNodesAndSelf()) {
            if (!n.isRoot() && (n.getLength() == 0.0))
                // Zero branch length indicates a sampled ancestor or polytomy, which are
                // disallowed in a StrictBinaryTree.
                return false;
        }
        return true;
    }

} // class StrictBinaryTree