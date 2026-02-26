package test.beastfx.app.beauti;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import beast.base.inference.operator.DeltaExchangeOperator;
import beast.base.inference.operator.kernel.BactrianDeltaExchangeOperator;
import beastfx.app.beauti.BeautiTabPane;
import javafx.stage.Stage;

/** test how the FixedMeanRate flag interact with link/unlink **/
@ExtendWith(ApplicationExtension.class)
public class FixedMeanRateTest extends BeautiBase {

	@Start
    public void start(Stage stage) {
    	try {
    		System.setProperty("beast.is.junit.testing", "true");
    		BeautiTabPane tabPane = BeautiTabPane.main2(new String[] {}, stage);
    		this.doc = tabPane.doc;
            stage.show();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

	@Test
	public void testFixedMeanRate(FxRobot robot) throws Exception {		
		importAlignment(NEXUS_DIR, new File("26.nex"), new File("29.nex"));
		
		robot.clickOn("#Mode").clickOn("#autoUpdateFixMeanSubstRate");
		// beautiFrame.menuItemWithPath("Mode", "Automatic set fix mean substitution rate flag").click();

		
		warning("Setting fixed mean rates");
		//JTabbedPaneFixture f = beautiFrame.tabbedPane();
		selectTab(robot, "Site Model");		
        clickOnNodesWithID(robot, "mutationRate.isEstimated");
		robot.clickOn("#FixMeanMutationRate");

		warning("link/unlink site models");
		// f.selectTab("Partitions");
		// beautiFrame.table().selectCells(TableCell.row(0).column(0), TableCell.row(1).column(0));
		selectPartitions(robot, 0, 1);
		clickOnButtonWithText(robot, "Link Site Models");
		clickOnButtonWithText(robot, "Unlink Site Models");

		//saveFile("/Users/remcobouckaert/tmp", "x.xml");
		makeSureXMLParses();
		

		
		BactrianDeltaExchangeOperator operator = (BactrianDeltaExchangeOperator) doc.pluginmap.get("FixMeanMutationRatesOperator");
		int nrOfParameters = operator.parameterInput.get().size();
		if(nrOfParameters != 2) {
			throw new IllegalArgumentException("Expected 2 parameters for deltaExchangeOperator, not " + nrOfParameters);
		}
	}
	
	@Test
	public void testFixedMeanRateSharedSiteModel(FxRobot robot) throws Exception {

		importAlignment(NEXUS_DIR, new File("26.nex"), new File("29.nex"), new File("47.nex"));
		
		warning("Setting fixed mean rates");
		selectTab(robot, "Site Model");		
		clickOnNodesWithID(robot, "mutationRate.isEstimated");
		//beautiFrame.checkBox("FixMeanMutationRate").check();

		warning("link/unlink site models");
		selectTab(robot, "Partitions");
		selectPartitions(robot, 0, 1, 2);
		clickOnButtonWithText(robot, "Link Site Models");
		clickOnButtonWithText(robot, "Unlink Site Models");
		
		BactrianDeltaExchangeOperator operator = (BactrianDeltaExchangeOperator) doc.pluginmap.get("FixMeanMutationRatesOperator");
		int nrOfParameters = operator.parameterInput.get().size();
		if (nrOfParameters != 3) {
			throw new IllegalArgumentException("Expected 3 parameters for deltaExchangeOperator, not " + nrOfParameters);
		}

		List<Integer> weights = operator.parameterWeightsInput.get().valuesInput.get();
		assertEquals(weights.size(), 3);
		assertEquals(weights.get(0), (Integer)614);
		assertEquals(weights.get(1), (Integer)601);
		assertEquals(weights.get(2), (Integer)819);

		selectPartitions(robot, 0, 2);
		// beautiFrame.table().selectCells(TableCell.row(0).column(1), TableCell.row(2).column(1));
		clickOnButtonWithText(robot, "Link Site Models");
		operator = (BactrianDeltaExchangeOperator) doc.pluginmap.get("FixMeanMutationRatesOperator");
		nrOfParameters = operator.parameterInput.get().size();
		
		//SiteModelInputEditor.customConnector(doc);
		
		if (nrOfParameters != 2) {
			throw new IllegalArgumentException("Expected 2 parameters for deltaExchangeOperator, not " + nrOfParameters);
		}
		weights = operator.parameterWeightsInput.get().valuesInput.get();
		assertEquals(weights.size(), 2);
		assertEquals(weights.get(0), (Integer)(614 + 819));
		assertEquals(weights.get(1), (Integer)601);

		makeSureXMLParses();
	}

}
