package beastfx.app.treeannotator.services;


import java.io.IOException;
import java.io.PrintStream;

import beast.base.evolution.tree.Tree;
import beastfx.app.treeannotator.TreeAnnotator;
import beastfx.app.treeannotator.TreeAnnotator.TreeSet;

public class MaxSumCladeCrediblityTopologyService extends MCCTopologyService {

	@Override
	public Tree setTopology(TreeSet treeSet, PrintStream progressStream, TreeAnnotator annotator) throws IOException {
        progressStream.println("Finding maximum sum clade credibility tree...");
		this.treeSet = treeSet;
		this.totalTreesUsed = annotator.getTotalTreesUsed();
		return summarizeTrees(annotator.getCladeSystem(), true, progressStream);
	}
	
	@Override
	public String getServiceName() {
		return "MSCC";
	}
	
	@Override
	public String getDescription() {
		return "Maximum sum of clade credibilities";
	}
}
