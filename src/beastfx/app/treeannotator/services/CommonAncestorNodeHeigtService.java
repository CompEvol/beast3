package beastfx.app.treeannotator.services;

import java.io.IOException;
import java.io.PrintStream;
import java.util.BitSet;

import beast.base.core.Citation;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.TreeUtils;
import beast.base.util.CollectionUtils;
import beastfx.app.treeannotator.CladeSystem;
import beastfx.app.treeannotator.TreeAnnotator;
import beastfx.app.treeannotator.TreeAnnotator.TreeSet;

@Citation(value = "Heled and Bouckaert (2013). BMC Evolutionary Biology.\nLooking for trees in the forest: summary tree from posterior samples.", DOI = "doi.org/10.1186/1471-2148-13-221")
public class CommonAncestorNodeHeigtService implements NodeHeightSettingService {

	private int totalTreesUsed;
	@Override
	public void setNodeHeights(Tree tree, PrintStream progressStream, TreeAnnotator treeAnnotator)  throws IOException {
//        Log.warning.println("Please cite: Heled and Bouckaert: Looking for trees in the forest:\n" +
//                "summary tree from posterior samples. BMC Evolutionary Biology 2013 13:221.");

        this.totalTreesUsed = treeAnnotator.getTotalTreesUsed();
		setTreeHeightsByCA(tree, treeAnnotator.getTreeSet(), progressStream);

	}

	boolean setTreeHeightsByCA(Tree targetTree, TreeSet treeSet, PrintStream progressStream) throws IOException {
		progressStream.println("Setting node heights...");
		progressStream.println("0              25             50             75            100");
		progressStream.println("|--------------|--------------|--------------|--------------|");

		int reportStepSize = totalTreesUsed / 60;
		if (reportStepSize < 1)
			reportStepSize = 1;
		int reported = 0;

// this call increments the clade counts and it shouldn't
// this is remedied with removeClades call after while loop below
		CladeSystem cladeSystem = new CladeSystem(targetTree);
		final int clades = cladeSystem.getCladeMap().size();

// allocate posterior tree nodes order once
		int[] postOrderList = new int[clades];
		BitSet[] ctarget = new BitSet[clades];
		BitSet[] ctree = new BitSet[clades];

		for (int k = 0; k < clades; ++k) {
			ctarget[k] = new BitSet();
			ctree[k] = new BitSet();
		}

		cladeSystem.getTreeCladeCodes(targetTree, ctarget);

// temp collecting heights inside loop allocated once
		double[][] hs = new double[clades][treeSet.totalTrees - treeSet.burninCount];

// heights total sum from posterior trees
		double[] ths = new double[clades];

		int totalTreesUsed = 0;

		int counter = 0;
		treeSet.reset();
		while (treeSet.hasNext()) {
			Tree tree = treeSet.next();
			TreeUtils.preOrderTraversalList(tree, postOrderList);
			cladeSystem.getTreeCladeCodes(tree, ctree);
			for (int k = 0; k < clades; ++k) {
				int j = postOrderList[k];
				for (int i = 0; i < clades; ++i) {
					if (CollectionUtils.isSubSet(ctarget[i], ctree[j])) {
						hs[i][counter] = tree.getNode(j).getHeight();
					}
				}
			}
			for (int k = 0; k < clades; ++k) {
				ths[k] += hs[k][counter];
			}
			totalTreesUsed += 1;
			while (reported < 61 && 1000.0 * reported < 61000.0 * (counter + 1) / this.totalTreesUsed) {
				progressStream.print("*");
				reported++;
				progressStream.flush();
			}
			counter++;

		}

//		if (targetOption != Target.USER_TARGET_TREE)
//			targetTree.initAndValidate();

		cladeSystem.removeClades(targetTree.getRoot(), true);
		for (int k = 0; k < clades; ++k) {
			ths[k] /= totalTreesUsed;
			final Node node = targetTree.getNode(k);
			node.setHeight(ths[k]);
			String attributeName = "CAheight";
			double[] values = hs[k];
			double min = values[0];
			double max = values[0];
			for (double d : values) {
				min = Math.min(d, min);
				max = Math.max(d, max);
			}
			if (Math.abs(min - max) > 1e-10) {
				TreeAnnotator.annotateMeanAttribute(node, attributeName + "_mean", values);
				TreeAnnotator.annotateMedianAttribute(node, attributeName + "_median", values);
				TreeAnnotator.annotateHPDAttribute(node, attributeName + "_95%_HPD", 0.95, values);
				TreeAnnotator.annotateRangeAttribute(node, attributeName + "_range", values);
			}
		}

		assert (totalTreesUsed == this.totalTreesUsed);
		this.totalTreesUsed = totalTreesUsed;
		progressStream.println();
		progressStream.println();

		return true;
	}

	@Override
	public String getServiceName() {
		return "CA";
	}

	@Override
	public String getDescription() {
		return "Common Ancestor heights";
	}

}
