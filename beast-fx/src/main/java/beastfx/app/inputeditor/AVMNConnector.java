package beastfx.app.inputeditor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import beast.base.core.BEASTInterface;
import beast.base.spec.inference.operator.Transform.LogTransform;
import beast.base.spec.evolution.branchratemodel.Base;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.type.RealScalar;
import beast.base.spec.type.Tensor;


/**
 * custom BEAUti connector that handles mean clock rates being used by the AVNM operator for a partition
 */
public class AVMNConnector {

    public static boolean customConnector(BeautiDoc doc) {
    	// System.err.println("AVMNConnector::customConnector() called");
    	
    	// count how many partitions have the a unique clock
    	List<BEASTInterface> partitions = doc.getPartitions("Partitions");
    	List<BEASTInterface> clockPartitions = doc.getPartitions("ClockModel");
    	Map<String, Integer> partitionsPerClockCount = new HashMap<>();
    	for (BEASTInterface clockModel : clockPartitions) {
    		String id = clockModel.getID();
    		if (!partitionsPerClockCount.containsKey(id)) {
    			partitionsPerClockCount.put(id, 1);
    		} else {
    			partitionsPerClockCount.put(id, 1 + partitionsPerClockCount.get(id));
    		}
    	}
    	
    	// make sure mean clock rate is (dis)connected to/from the correct AVNM operator
    	for (int i = 0; i < partitions.size(); i++) {
    		String clockID = clockPartitions.get(i).getID();
			String partitionID = BeautiDoc.parsePartition(partitions.get(i).getID());
			if (doc.pluginmap.containsKey("AVMNOperator." + partitionID)) {
				BEASTInterface t = doc.pluginmap.get("AVNMLogTransform." + partitionID);
				if (t instanceof LogTransform) {
					LogTransform logtransform = (LogTransform) t;
					BEASTInterface c = clockPartitions.get(i);
					if (c instanceof Base clockmodel) {
						RealScalar<?> f = clockmodel.meanRateInput.get();
						if (f instanceof RealScalarParam<?>) {
							RealScalarParam<?> clockrate = (RealScalarParam<?>) f;
							
				    		if (clockrate.isEstimatedInput.get() && partitionsPerClockCount.get(clockID) == 1) {
				    			// connect mean clock rate to AVMN operator
				    			// if clock rate is estimated and covers only a single partition
				    			connect(clockrate, logtransform);
				    		} else {
				    			// disconnect mean clock rate from AVMN operator
				    			disconnect(clockrate, logtransform);
				    		}							
						}
					}
				}
			}
    	}
    	
    	
    	return true;
    }

	private static void disconnect(RealScalarParam<?> clockrate, LogTransform logtransform) {
		for (Tensor f : logtransform.functionInput.get()) {
			if (f == clockrate) {
				logtransform.functionInput.get().remove(clockrate);
			}
		}
	}

	private static void connect(RealScalarParam<?> clockrate, LogTransform logtransform) {
		for (Tensor f : logtransform.functionInput.get()) {
			if (f == clockrate) {
				return;
			}
		}
		logtransform.functionInput.get().add(clockrate);
	}
}
