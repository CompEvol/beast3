package beastfx.app.treeannotator.services;


import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import beast.base.core.Log;
import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.TreeParser;
import beast.base.parser.NexusParser;
import beast.base.util.FileUtils;
import beastfx.app.treeannotator.TreeAnnotator;
import beastfx.app.treeannotator.TreeAnnotator.TreeSet;

public class UserTargetTreeTopologyService implements TopologySettingService {

	@Override
	public Tree setTopology(TreeSet treeSet, PrintStream progressStream, TreeAnnotator annotator) throws IOException {
		String targetTreeFileName = annotator.targetInput.get();
        if (targetTreeFileName != null) {
            progressStream.println("Reading user specified target tree, " + targetTreeFileName);
            
            String tree = FileUtils.load(targetTreeFileName);
            
            if (tree.trim().startsWith("#NEXUS")) {
            	NexusParser parser2 = new NexusParser();
            	parser2.parseFile(new File(targetTreeFileName));
            	Tree targetTree = parser2.trees.get(0);
            	return targetTree;
            } else {
                try {
                    TreeParser parser2 = new TreeParser();
                    parser2.initByName("IsLabelledNewick", true, "newick", tree);
                    Tree targetTree = parser2;
                	return targetTree;
                } catch (Exception e) {
                    Log.err.println("Error Parsing Target Tree: " + e.getMessage());
                    return null;
                }
            }
        } else {
            Log.err.println("No user target tree specified.");
            return null;
        }
	}

	
	final static public String SERVICE_NAME = "target";
	
	@Override
	public String getServiceName() {
		return SERVICE_NAME;
	}

	@Override
	public String getDescription() {
		return "User target tree";
	}
}
