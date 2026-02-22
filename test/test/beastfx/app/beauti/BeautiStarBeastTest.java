package test.beastfx.app.beauti;




import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import beastfx.app.beauti.BeautiTabPane;
import beastfx.app.util.Utils;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

import java.io.File;

@ExtendWith(ApplicationExtension.class)
public class BeautiStarBeastTest extends BeautiBase {

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
	
	final static String PREFIX = "../beast2/doc/tutorials/STARBEAST/figures/BEAUti_";

	@Test
	public void simpleStarBeastTest(FxRobot robot) throws Exception {

		String BASE_DIR = PREFIX.substring(0, PREFIX.lastIndexOf('/'));
		File dir = new File(BASE_DIR);
		if (dir.listFiles() != null)
		for (File file : dir.listFiles()) {
			if (file.getAbsolutePath().contains(PREFIX) && file.getName().endsWith(".png")) {
				file.delete();
			}
		}
		


		// create screen-shot showing template menu item
		warning("Select StarBeast template");
//		robot.clickOn("#File").clickOn("Template");
//		screenshot(PREFIX + "selectTemplate.png");
//		
//		robot.type(KeyCode.ESCAPE);
//		robot.type(KeyCode.ESCAPE);
//		robot.type(KeyCode.ESCAPE);
		
		if (!Utils.isMac()) {
			robot.clickOn("#File").clickOn("Template").clickOn("StarBeast");
		} else {
			robot.interact(() -> {
		        	try {
		    			doc.loadNewTemplate(TEMPLATE_DIR +"/StarBeast.xml");
		    			doc.beauti.refreshPanel();
		        	} catch (Exception e) {
						e.printStackTrace();
					}
			});
			Thread.sleep(500);
		}
		
		//beautiFrame.menuItemWithPath("File", "Template").click();
		//JMenuItemFixture templateMenu = beautiFrame.menuItemWithPath("File", "Template", "StarBeast");
		//templateMenu.click();
		// remove menu from screen
		// beautiFrame.menuItemWithPath("File").click();

		//JTabbedPaneFixture f = beautiFrame.tabbedPane();
		robot.clickOn("Partitions");


		// 1. Load gopher data 26.nex, 29.nex, 47.nex
		warning("1. Load gopher data 26.nex, 29.nex, 47.nex");
		importAlignment(NEXUS_DIR, new File("26.nex"), new File("29.nex"), new File("47.nex"));

		screenshot( PREFIX + "DataPartitions.png");
		printBeautiState();

		
		// 2. Define Taxon sets
		warning("2. Define taxon sets");
		robot.clickOn("Taxon sets");
		robot.clickOn("Guess");
		//JOptionPaneFixture dialog = new JOptionPaneFixture(robot());
		//DialogFixture dialog = WindowFinder.findDialog("GuessTaxonSets").using(robot());
		robot.clickOn("split on character");
		robot.clickOn("#splitCombo").clickOn("2");
		robot.doubleClickOn("#SplitChar2").write("_");
		screenshot( PREFIX + "Guess_Taxonsets.png");
		//JButton okButton = dialog.robot.finder().find(JButtonMatcher.withText("OK"));
		//new JButtonFixture(dialog.robot, okButton).click();
		robot.clickOn("OK");
		printBeautiState();

		// 3. Set site model to HKY + empirical frequencies
		warning("3. Set site model to HKY + empirical frequencies");
        robot.clickOn("Site Model");
		final ListView<?> list0 = robot.lookup(".list-view").queryAs(ListView.class);
		robot.interact(()->list0.getSelectionModel().select(0));
		selectFromCombobox(robot, "substModelComboBox", "HKY");
		selectFromCombobox(robot, "frequenciesComboBox", "Empirical");
		robot.interact(()->list0.getSelectionModel().clearAndSelect(1));
		selectFromCombobox(robot, "substModelComboBox", "HKY");
		selectFromCombobox(robot, "frequenciesComboBox", "Empirical");
		robot.interact(()->list0.getSelectionModel().clearAndSelect(2));
		selectFromCombobox(robot, "substModelComboBox", "HKY");
		selectFromCombobox(robot, "frequenciesComboBox", "Empirical");

		//JCheckBoxFixture fixMeanMutationRate = beautiFrame.checkBox("FixMeanMutationRate");
		//fixMeanMutationRate.check();
		screenshot( PREFIX + "Site_Model.png");
		printBeautiState();
		
		// 4. Inspect clock models
		warning("4. Inspect clock models");
		final ListView<?> list = robot.lookup(".list-view").queryAs(ListView.class);
		robot.clickOn("Clock Model");
		robot.interact(()->list.getSelectionModel().select(0));
		screenshot( PREFIX + "ClockModel1.png");
		robot.interact(()->list.getSelectionModel().clearAndSelect(1));
		screenshot( PREFIX + "ClockModel2.png");
		robot.interact(()->list.getSelectionModel().clearAndSelect(2));
		screenshot( PREFIX + "ClockModel3.png");
		
		// 5. Inspect multispecies coalescent
		warning("5. Inspect multispecies coalescent");
		robot.clickOn("Multi Species Coalescent");
		clickOnNodesWithID(robot, "SpeciesTreePopSize.Species.editButton");
		clickOnNodesWithID(robot, "treePrior.t:26.editButton");
		clickOnNodesWithID(robot, "treePrior.t:29.editButton");
		clickOnNodesWithID(robot, "treePrior.t:47.editButton");
		selectFromCombobox(robot, "popFunctionComboBox", "linear_with_constant_root");
		selectFromCombobox(robot, "26ComboBox", "Y or mitochondrial");
		screenshot(PREFIX + "MSP.png");
		
		// 6. Set up MCMC parameters
		warning("6. Set up MCMC parameters");
		robot.clickOn("MCMC");
		robot.doubleClickOn("#chainLength").write("5000000");
		clickOnNodesWithID(robot, "speciesTreeLogger.editButton");
		robot.doubleClickOn("#logEvery").write("1000");
		clickOnNodesWithID(robot, "speciesTreeLogger.editButton");

		clickOnNodesWithID(robot, "screenlog.editButton");
		robot.doubleClickOn("#logEvery").write("10000");
		clickOnNodesWithID(robot, "speciesTreeLogger.editButton");
		screenshot(PREFIX + "MCMC.png");
		
		makeSureXMLParses();

	}


}
