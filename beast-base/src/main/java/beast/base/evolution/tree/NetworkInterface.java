package beast.base.evolution.tree;

import java.util.List;


public interface NetworkInterface {
    String getID();

    int getLeafNodeCount();
	int getInternalNodeCount();
	int getNodeCount();

    List<Node> getExternalNodes();
    List<Node> getInternalNodes();
    
	boolean somethingIsDirty();
    
}
