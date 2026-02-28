package beastfx.app.treeannotator.services;


import java.io.IOException;
import java.io.PrintStream;

import beast.base.evolution.tree.Tree;
import beastfx.app.treeannotator.CladeSystem;
import beastfx.app.treeannotator.TreeAnnotator;
import beastfx.app.treeannotator.TreeAnnotator.TreeSet;

public class MCCTopologyService implements TopologySettingService {

	
	protected TreeSet treeSet;
	protected int totalTreesUsed;
	
	@Override
	public Tree setTopology(TreeSet treeSet, PrintStream progressStream, TreeAnnotator annotator) throws IOException {
        progressStream.println("Finding maximum credibility tree...");
		this.treeSet = treeSet;
		this.totalTreesUsed = annotator.getTotalTreesUsed();
		return summarizeTrees(annotator.getCladeSystem(), false, progressStream);
	}

    protected Tree summarizeTrees(CladeSystem cladeSystem, boolean useSumCladeCredibility, PrintStream progressStream) throws IOException  {

        Tree bestTree = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        progressStream.println("Analyzing " + totalTreesUsed + " trees...");
        progressStream.println("0              25             50             75            100");
        progressStream.println("|--------------|--------------|--------------|--------------|");

        int stepSize = Math.max(totalTreesUsed / 60, 1);
        int reported = 0;

        int counter = 0;
        treeSet.reset();
        while (treeSet.hasNext()) {
        	Tree tree = treeSet.next();
            double score = scoreTree(tree, cladeSystem, useSumCladeCredibility);
          if (score > bestScore) {
              bestTree = tree;
              bestScore = score;
          }
		  while (reported < 61 && 1000.0 * reported < 61000.0 * (counter + 1) / totalTreesUsed) {
              progressStream.print("*");
              reported++;
              progressStream.flush();
    	  }
          counter++;
        }
        progressStream.println();
        progressStream.println();
        if (useSumCladeCredibility) {
            progressStream.println("Highest Sum Clade Credibility: " + bestScore);
        } else {
            progressStream.println("Highest Log Clade Credibility: " + bestScore);
        }

        bestTree.initAndValidate();
        return bestTree;
    }

    public double scoreTree(Tree tree, CladeSystem cladeSystem, boolean useSumCladeCredibility) {
        if (useSumCladeCredibility) {
            return cladeSystem.getSumCladeCredibility(tree.getRoot(), null);
        } else {
            return cladeSystem.getLogCladeCredibility(tree.getRoot(), null);
        }
    }

	@Override
	public String getServiceName() {
		return "MCC";
	}

	@Override
	public String getDescription() {
		return "Maximum clade credibility tree";
	}
}
