package test.beastfx.app.beauti;




//import beast.app.util.Utils;
import beastfx.app.beauti.BeautiTabPane;
import javafx.stage.Stage;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Arrays;



@ExtendWith(ApplicationExtension.class)
public class BeautiRateTutorialTest extends BeautiBase {

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
	// file used to store, then reload xml
	final static String XML_FILE = "rsv.xml";
	final static String PREFIX = "../beast2/doc/tutorials/MEPs/figures/generated/BEAUti_";

	@Test
	public void MEPTutorial(FxRobot robot) throws Exception {
		long t0 = System.currentTimeMillis();
		//ScreenshotTaker screenshotTaker = new ScreenshotTaker();
		//beauti.frame.setSize(1024, 640);
		
		File dir = new File(PREFIX.substring(0, PREFIX.lastIndexOf('/')));
		if (!dir.exists()) {
			dir.mkdir();
		}
		if (dir.listFiles() != null)
		for (File file : dir.listFiles()) {
			file.delete();
		}
		
		// 0. Load primate-mtDNA.nex
		warning("// 0. Load RSV2.nex");
		importAlignment(NEXUS_DIR, new File("RSV2.nex"));

        robot.clickOn("#Mode").clickOn("Automatic set fix mean substitution rate flag");

		// load anolis.nex
		// JTabbedPaneFixture f = beautiFrame.tabbedPane();
		BeautiTabPane pane = robot.lookup("#BeautiTabPane").queryAs(BeautiTabPane.class);
		assertThat(pane.isVisible());
		String[] titles = tabTitles(pane);
		assertArrayEquals(titles,"[Partitions, Tip Dates, Site Model, Clock Model, Priors, MCMC]");
		System.err.println(Arrays.toString(titles));

		
		selectTab(robot, "Partitions");
		selectPartitions(robot, 0);
		
				
		//0. Split partition... 
		warning("0. Split partition...");
		clickOnButtonWithText(robot, "Split");
		//JOptionPaneFixture dialog = new JOptionPaneFixture(robot());
		selectFromCombobox(robot, "1 + 2 + 3", "1 + 2 + 3 frame 3");
		
		//robot.clickOn(".combo-box").clickOn("1 + 2 + 3 frame 3");
		robot.clickOn("OK");
		// check table
		printTableContents(robot);
		checkTableContents(robot, 
				"[RSV2_1, RSV2, 129, 209, nucleotide, RSV2_1, RSV2_1, RSV2_1, false]*" +
				"[RSV2_2, RSV2, 129, 210, nucleotide, RSV2_2, RSV2_2, RSV2_1, false]*" +
				"[RSV2_3, RSV2, 129, 210, nucleotide, RSV2_3, RSV2_3, RSV2_1, false]"
			);
		printBeautiState();
//		assertStateEquals("Tree.t:RSV2_2", "clockRate.c:RSV2_2", "birthRate.t:RSV2_2", "Tree.t:RSV2_3", "clockRate.c:RSV2_3", "birthRate.t:RSV2_3", "Tree.t:RSV2_1", "birthRate.t:RSV2_1");
//		assertOperatorsEqual("StrictClockRateScaler.c:RSV2_2", "YuleBirthRateScaler.t:RSV2_2", "YuleModelBICEPSEpochTop.t:RSV2_2", "YuleModelBICEPSEpochAll.t:RSV2_2", "YuleModelBICEPSTreeFlex.t:RSV2_2", "YuleModelTreeRootScaler.t:RSV2_2", "YuleModelUniformOperator.t:RSV2_2", "YuleModelSubtreeSlide.t:RSV2_2", "YuleModelNarrow.t:RSV2_2", "YuleModelWide.t:RSV2_2", "YuleModelWilsonBalding.t:RSV2_2", "strictClockUpDownOperator.c:RSV2_2", "StrictClockRateScaler.c:RSV2_3", "YuleBirthRateScaler.t:RSV2_3", "YuleModelBICEPSEpochTop.t:RSV2_3", "YuleModelBICEPSEpochAll.t:RSV2_3", "YuleModelBICEPSTreeFlex.t:RSV2_3", "YuleModelTreeRootScaler.t:RSV2_3", "YuleModelUniformOperator.t:RSV2_3", "YuleModelSubtreeSlide.t:RSV2_3", "YuleModelNarrow.t:RSV2_3", "YuleModelWide.t:RSV2_3", "YuleModelWilsonBalding.t:RSV2_3", "strictClockUpDownOperator.c:RSV2_3", "YuleBirthRateScaler.t:RSV2_1", "YuleModelBICEPSEpochTop.t:RSV2_1", "YuleModelBICEPSEpochAll.t:RSV2_1", "YuleModelBICEPSTreeFlex.t:RSV2_1", "YuleModelTreeRootScaler.t:RSV2_1", "YuleModelUniformOperator.t:RSV2_1", "YuleModelSubtreeSlide.t:RSV2_1", "YuleModelNarrow.t:RSV2_1", "YuleModelWide.t:RSV2_1", "YuleModelWilsonBalding.t:RSV2_1");
//		assertPriorsEqual("YuleModel.t:RSV2_1", "YuleModel.t:RSV2_2", "YuleModel.t:RSV2_3", "ClockPrior.c:RSV2_2", "YuleBirthRatePrior.t:RSV2_2", "ClockPrior.c:RSV2_3", "YuleBirthRatePrior.t:RSV2_3", "YuleBirthRatePrior.t:RSV2_1");
//		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.RSV2_2", "TreeHeight.t:RSV2_2", "clockRate.c:RSV2_2", "YuleModel.t:RSV2_2", "birthRate.t:RSV2_2", "treeLikelihood.RSV2_3", "TreeHeight.t:RSV2_3", "clockRate.c:RSV2_3", "YuleModel.t:RSV2_3", "birthRate.t:RSV2_3", "treeLikelihood.RSV2_1", "TreeHeight.t:RSV2_1", "YuleModel.t:RSV2_1", "birthRate.t:RSV2_1");

		assertStateEquals("Tree.t:RSV2_1", "clockRate.c:RSV2_2", "birthRate.t:RSV2_1", "clockRate.c:RSV2_3");
		assertOperatorsEqual("StrictClockRateScaler.c:RSV2_2", "YuleBirthRateScaler.t:RSV2_1", "strictClockUpDownOperator.c:RSV2_2", "YuleModelBICEPSEpochTop.t:RSV2_1", "YuleModelBICEPSEpochAll.t:RSV2_1", "YuleModelBICEPSTreeFlex.t:RSV2_1", "YuleModelTreeRootScaler.t:RSV2_1", "YuleModelUniformOperator.t:RSV2_1", "YuleModelSubtreeSlide.t:RSV2_1", "YuleModelNarrow.t:RSV2_1", "YuleModelWide.t:RSV2_1", "YuleModelWilsonBalding.t:RSV2_1", "StrictClockRateScaler.c:RSV2_3", "strictClockUpDownOperator.c:RSV2_3");
		assertPriorsEqual("YuleModel.t:RSV2_1", "ClockPrior.c:RSV2_2", "YuleBirthRatePrior.t:RSV2_1", "ClockPrior.c:RSV2_3");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.RSV2_2", "treeLikelihood.RSV2_3", "TreeHeight.t:RSV2_1", "clockRate.c:RSV2_2", "YuleModel.t:RSV2_1", "birthRate.t:RSV2_1", "treeLikelihood.RSV2_1", "clockRate.c:RSV2_3");
	
		//1a. Link trees... 
		warning("1a. Link trees...");
		selectTab(robot, "Partitions");
		selectPartitions(robot, 0, 1, 2);
//		t = beautiFrame.table();
//		t.selectCells(TableCell.row(0).column(2), TableCell.row(1).column(2), TableCell.row(2).column(2));
		clickOnButtonWithText(robot, "Link Trees");
		printBeautiState();

		//1b. ...and call the tree "tree"
		warning("1b. ...and call the tree \"tree\"");
		selectTab(robot, "Partitions");
        setPartitionTableCell(robot, 7, "tree");
//		JTableCellFixture cell = beautiFrame.table().cell(TableCell.row(0).column(7));
//		Component editor = cell.editor();
//		JComboBoxFixture comboBox = new JComboBoxFixture(robot(), (JComboBox<?>) editor);
//		cell.startEditing();
//		comboBox.selectAllText();
//		comboBox.enterText("tree");
//		//comboBox.pressAndReleaseKeys(KeyEvent.VK_ENTER);
//		cell.stopEditing();
		printBeautiState();
		assertStateEquals("clockRate.c:RSV2_2", "clockRate.c:RSV2_3", "Tree.t:tree", "birthRate.t:tree");
		assertOperatorsEqual("StrictClockRateScaler.c:RSV2_2", "StrictClockRateScaler.c:RSV2_3", "YuleBirthRateScaler.t:tree", "YuleModelBICEPSEpochTop.t:tree", "YuleModelBICEPSEpochAll.t:tree", "YuleModelBICEPSTreeFlex.t:tree", "YuleModelTreeRootScaler.t:tree", "YuleModelUniformOperator.t:tree", "YuleModelSubtreeSlide.t:tree", "YuleModelNarrow.t:tree", "YuleModelWide.t:tree", "YuleModelWilsonBalding.t:tree", "strictClockUpDownOperator.c:RSV2_3", "strictClockUpDownOperator.c:RSV2_2");
		assertPriorsEqual("YuleModel.t:tree", "ClockPrior.c:RSV2_2", "ClockPrior.c:RSV2_3", "YuleBirthRatePrior.t:tree");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.RSV2_2", "clockRate.c:RSV2_2", "treeLikelihood.RSV2_3", "clockRate.c:RSV2_3", "treeLikelihood.RSV2_1", "TreeHeight.t:tree", "YuleModel.t:tree", "birthRate.t:tree");

		
		//2a. Link clocks 
		warning("2a. Link clocks");
		selectTab(robot, "Partitions");
		selectPartitions(robot, 0, 1, 2);
		clickOnButtonWithText(robot, "Link Clock Models");

		//2b. and call the uncorrelated relaxed molecular clock "clock"
		warning("2b. and call the uncorrelated relaxed molecular clock \"clock\"");
        setPartitionTableCell(robot, 6, "clock");
//		cell = beautiFrame.table().cell(TableCell.row(0).column(6));
//		editor = cell.editor();
//		comboBox = new JComboBoxFixture(robot(), (JComboBox<?>) editor);
//		cell.startEditing();
//		comboBox.selectAllText();
//		comboBox.enterText("clock\n");
//		//comboBox.pressAndReleaseKeys(KeyEvent.VK_ENTER);
//		cell.stopEditing();
		printBeautiState();
		assertStateEquals("Tree.t:tree", "birthRate.t:tree");
		assertOperatorsEqual("YuleBirthRateScaler.t:tree", "YuleModelBICEPSEpochTop.t:tree", "YuleModelBICEPSEpochAll.t:tree", "YuleModelBICEPSTreeFlex.t:tree", "YuleModelTreeRootScaler.t:tree", "YuleModelUniformOperator.t:tree", "YuleModelSubtreeSlide.t:tree", "YuleModelNarrow.t:tree", "YuleModelWide.t:tree", "YuleModelWilsonBalding.t:tree");
		assertPriorsEqual("YuleModel.t:tree", "YuleBirthRatePrior.t:tree");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.RSV2_2", "treeLikelihood.RSV2_3", "treeLikelihood.RSV2_1", "TreeHeight.t:tree", "YuleModel.t:tree", "birthRate.t:tree");

		
		//3a. Link site models
		warning("3a. link site models");
		selectTab(robot, "Partitions");
		selectPartitions(robot, 0, 1, 2);
		clickOnButtonWithText(robot, "Link Site Models");
		
		//3b. Set the site model to HKY (empirical)
		warning("3b. Set the site model to HKY (empirical)");
		selectTab(robot, "Site Model");
		robot.clickOn("#substModelComboBox").clickOn("HKY");
		robot.clickOn("#frequenciesComboBox").clickOn("Empirical");
        clickOnNodesWithID(robot, "mutationRate.isEstimated");
        clickOnNodesWithID(robot, "FixMeanMutationRate");
        setCheckBox(robot, "#FixMeanMutationRate");
		screenshot(PREFIX + "Site_Model.png");
		printBeautiState();
		
		//3c. Unlink site models
		warning("3c. unlink site models");
		selectTab(robot, "Partitions");
		selectPartitions(robot, 0, 1, 2);
		clickOnButtonWithText(robot, "Unlink Site Models");
		printBeautiState();
		assertStateEquals("Tree.t:tree", "birthRate.t:tree", "kappa.s:RSV2_1", "mutationRate.s:RSV2_1", "kappa.s:RSV2_2", "mutationRate.s:RSV2_2", "kappa.s:RSV2_3", "mutationRate.s:RSV2_3");
		assertOperatorsEqual("YuleBirthRateScaler.t:tree", "YuleModelBICEPSEpochTop.t:tree", "YuleModelBICEPSEpochAll.t:tree", "YuleModelBICEPSTreeFlex.t:tree", "YuleModelTreeRootScaler.t:tree", "YuleModelUniformOperator.t:tree", "YuleModelSubtreeSlide.t:tree", "YuleModelNarrow.t:tree", "YuleModelWide.t:tree", "YuleModelWilsonBalding.t:tree", "KappaScaler.s:RSV2_1", "KappaScaler.s:RSV2_2", "KappaScaler.s:RSV2_3", "FixMeanMutationRatesOperator");
		assertPriorsEqual("YuleModel.t:tree", "YuleBirthRatePrior.t:tree", "KappaPrior.s:RSV2_1", "KappaPrior.s:RSV2_2", "KappaPrior.s:RSV2_3");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.RSV2_2", "treeLikelihood.RSV2_3", "treeLikelihood.RSV2_1", "TreeHeight.t:tree", "YuleModel.t:tree", "birthRate.t:tree", "kappa.s:RSV2_1", "mutationRate.s:RSV2_1", "kappa.s:RSV2_2", "mutationRate.s:RSV2_2", "kappa.s:RSV2_3", "mutationRate.s:RSV2_3");
		
		screenshot(PREFIX + "partition.png");
		
		//4. set up tip dates
		selectTab(robot, "Tip Dates");
		warning("4. Setting up tip dates");
		setCheckBox(robot, "useTipDates");
		clickOnButtonWithText(robot, "Auto-configure");
		
		robot.doubleClickOn("#SplitChar").write("s");
		screenshot(PREFIX + "GuessDates.png");
		robot.clickOn("#delimiterCombo").clickOn("after last");		
		robot.clickOn("OK");
		screenshot(PREFIX + "dates.png");
		printBeautiState();
		assertStateEquals("Tree.t:tree", "birthRate.t:tree", "kappa.s:RSV2_1", "mutationRate.s:RSV2_1", "kappa.s:RSV2_2", "mutationRate.s:RSV2_2", "kappa.s:RSV2_3", "mutationRate.s:RSV2_3", "clockRate.c:clock");
		assertOperatorsEqual("YuleBirthRateScaler.t:tree", "YuleModelBICEPSEpochTop.t:tree", "YuleModelBICEPSEpochAll.t:tree", "YuleModelBICEPSTreeFlex.t:tree", "YuleModelTreeRootScaler.t:tree", "YuleModelUniformOperator.t:tree", "YuleModelSubtreeSlide.t:tree", "YuleModelNarrow.t:tree", "YuleModelWide.t:tree", "YuleModelWilsonBalding.t:tree", "KappaScaler.s:RSV2_1", "KappaScaler.s:RSV2_2", "KappaScaler.s:RSV2_3", "FixMeanMutationRatesOperator", "StrictClockRateScaler.c:clock", "strictClockUpDownOperator.c:clock");
		assertPriorsEqual("YuleModel.t:tree", "YuleBirthRatePrior.t:tree", "KappaPrior.s:RSV2_1", "KappaPrior.s:RSV2_2", "KappaPrior.s:RSV2_3", "ClockPrior.c:clock");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.RSV2_2", "treeLikelihood.RSV2_3", "treeLikelihood.RSV2_1", "TreeHeight.t:tree", "YuleModel.t:tree", "birthRate.t:tree", "kappa.s:RSV2_1", "mutationRate.s:RSV2_1", "kappa.s:RSV2_2", "mutationRate.s:RSV2_2", "kappa.s:RSV2_3", "mutationRate.s:RSV2_3", "clockRate.c:clock");
	
		//5. Change tree prior to Coalescent with constant pop size 
		warning("5a. Change tree prior to Coalescent with constant pop size");
		selectTab(robot, "Priors");
		robot.clickOn("#TreeDistribution").clickOn("Coalescent Constant Population");
		
		warning("5b. Change clock prior to Log Normal with M = -5, S = 1.25");
		selectFromCombobox(robot, "clockRate.c:clock.distr", "Log Normal");
        clickOnNodesWithID(robot, "ClockPrior.c:clock.editButton");
		robot.doubleClickOn("#M").write("-5");
		robot.doubleClickOn("#S").write("1.25");
		screenshot( PREFIX + "priors.png");
		printBeautiState();
		assertStateEquals("Tree.t:tree", "kappa.s:RSV2_1", "mutationRate.s:RSV2_1", "kappa.s:RSV2_2", "mutationRate.s:RSV2_2", "kappa.s:RSV2_3", "mutationRate.s:RSV2_3", "clockRate.c:clock", "popSize.t:tree");
		assertOperatorsEqual("CoalescentConstantBICEPSEpochTop.t:tree", "CoalescentConstantBICEPSEpochAll.t:tree", "CoalescentConstantBICEPSTreeFlex.t:tree", "CoalescentConstantTreeRootScaler.t:tree", "CoalescentConstantUniformOperator.t:tree", "CoalescentConstantSubtreeSlide.t:tree", "CoalescentConstantNarrow.t:tree", "CoalescentConstantWide.t:tree", "CoalescentConstantWilsonBalding.t:tree", "KappaScaler.s:RSV2_1", "KappaScaler.s:RSV2_2", "KappaScaler.s:RSV2_3", "FixMeanMutationRatesOperator", "StrictClockRateScaler.c:clock", "strictClockUpDownOperator.c:clock", "PopSizeScaler.t:tree");
		assertPriorsEqual("CoalescentConstant.t:tree", "ClockPrior.c:clock", "KappaPrior.s:RSV2_1", "KappaPrior.s:RSV2_2", "KappaPrior.s:RSV2_3", "PopSizePrior.t:tree");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.RSV2_2", "treeLikelihood.RSV2_3", "treeLikelihood.RSV2_1", "TreeHeight.t:tree", "kappa.s:RSV2_1", "mutationRate.s:RSV2_1", "kappa.s:RSV2_2", "mutationRate.s:RSV2_2", "kappa.s:RSV2_3", "mutationRate.s:RSV2_3", "clockRate.c:clock", "popSize.t:tree", "CoalescentConstant.t:tree");


		//6. Run MCMC and look at results in Tracer, TreeAnnotator->FigTree
		warning("6. Setting up MCMC parameters");
		selectTab(robot, "MCMC");
		robot.doubleClickOn("#chainLength").write("2000000");
		clickOnNodesWithID(robot, "tracelog.editButton");
		robot.doubleClickOn("#logEvery").write("400");
		clickOnNodesWithID(robot, "tracelog.editButton");
		

		clickOnNodesWithID(robot, "treelog.t:tree.editButton");
		robot.doubleClickOn("#logEvery").write("400");
		screenshot(PREFIX + "mcmc.png");
		clickOnNodesWithID(robot, "treelog.t:tree.editButton");
		printBeautiState();
		assertStateEquals("Tree.t:tree", "kappa.s:RSV2_1", "mutationRate.s:RSV2_1", "kappa.s:RSV2_2", "mutationRate.s:RSV2_2", "kappa.s:RSV2_3", "mutationRate.s:RSV2_3", "clockRate.c:clock", "popSize.t:tree");
		assertOperatorsEqual("CoalescentConstantBICEPSEpochTop.t:tree", "CoalescentConstantBICEPSEpochAll.t:tree", "CoalescentConstantBICEPSTreeFlex.t:tree", "CoalescentConstantTreeRootScaler.t:tree", "CoalescentConstantUniformOperator.t:tree", "CoalescentConstantSubtreeSlide.t:tree", "CoalescentConstantNarrow.t:tree", "CoalescentConstantWide.t:tree", "CoalescentConstantWilsonBalding.t:tree", "KappaScaler.s:RSV2_1", "KappaScaler.s:RSV2_2", "KappaScaler.s:RSV2_3", "FixMeanMutationRatesOperator", "StrictClockRateScaler.c:clock", "strictClockUpDownOperator.c:clock", "PopSizeScaler.t:tree");
		assertPriorsEqual("CoalescentConstant.t:tree", "ClockPrior.c:clock", "KappaPrior.s:RSV2_1", "KappaPrior.s:RSV2_2", "KappaPrior.s:RSV2_3", "PopSizePrior.t:tree");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.RSV2_2", "treeLikelihood.RSV2_3", "treeLikelihood.RSV2_1", "TreeHeight.t:tree", "kappa.s:RSV2_1", "mutationRate.s:RSV2_1", "kappa.s:RSV2_2", "mutationRate.s:RSV2_2", "kappa.s:RSV2_3", "mutationRate.s:RSV2_3", "clockRate.c:clock", "popSize.t:tree", "CoalescentConstant.t:tree");
		
		
		//7. Run MCMC and look at results in Tracer, TreeAnnotator->FigTree
		warning("7. Run MCMC and look at results in Tracer, TreeAnnotator->FigTree");
		makeSureXMLParses();

		long t1 = System.currentTimeMillis();
		System.err.println("total time: " + (t1 - t0)/1000 + " seconds");
		
	}


	

	@Test
	public void MEPBSPTutorial(FxRobot robot) throws InterruptedException {
		if (true) {return;}
		try {
		long t0 = System.currentTimeMillis();
		//ScreenshotTaker screenshotTaker = new ScreenshotTaker();
		//beauti.frame.setSize(1024, 640);

		// 1. reaload XML file
		warning("1. reload rsv.xml");
		String dir = "" + org.assertj.core.util.Files.temporaryFolder();
		String file = XML_FILE;
		
//		if (!Utils.isMac()) {
//			robot.clickOn("#File").clickOn("Load");
//			JFileChooserFixture fileChooser = findFileChooser().using(robot());
//			fileChooser.setCurrentDirectory(new File(dir));
//			fileChooser.selectFile(new File(file)).approve();
//		} else {
			File _file = new File(dir + "/" + file);
//			execute(new GuiTask() {
//		        @Override
//				protected void executeInEDT() {
	                doc.newAnalysis();
	                doc.setFileName(_file.getAbsolutePath());
	                try {
		                doc.loadXML(new File(doc.getFileName()));
		        	} catch (Exception e) {
						e.printStackTrace();
					}
//		        }
//		    });							
//		}
	                
		// JTabbedPaneFixture f = beautiFrame.tabbedPane();
		printBeautiState();

		// 2. change tree prior to BSP
		warning("2. change tree prior to BSP");
		selectTab(robot, "Priors");
		robot.clickOn("TreeDistribution").clickOn("Coalescent Bayesian Skyline");
		screenshot(PREFIX + "priors2.png");
		printBeautiState();
		
		// 3. change tree prior to BSP
		warning("3. change group and population size parameters");
		robot.clickOn("#View").clickOn("Show Initialization panel");
		
		robot.clickOn("#isPopSizes.t:tree.editButton");
		robot.clickOn("#dimension"); robot.write("3");
		robot.clickOn("#isPopSizes.t:tree.editButton");
		
		robot.clickOn("#isGroupSizes.t:tree.editButton");
		robot.clickOn("#dimension"); robot.write("3");
		screenshot( PREFIX + "init.png");
		printBeautiState();
		
		// 4. set chain-length to 10M, log every 10K
		warning("4. set chain-length to 10M, log every 10K");
		selectTab(robot, "MCMC");
		robot.clickOn("#chainLength"); robot.write("10000000");
		robot.clickOn("#tracelog.editButton");
		robot.clickOn("#logEvery"); robot.write("10000");
		robot.clickOn("#tracelog.editButton");
		

		robot.clickOn("#treelog.t:tree.editButton");
		robot.clickOn("#logEvery"); robot.write("10000");
		robot.clickOn("#treelog.t:tree.editButton");
		printBeautiState();
		
		// 5. save XML file
		warning("5. save XML file");
		File fout = new File(org.assertj.core.util.Files.temporaryFolder() + "/" + XML_FILE);
		if (fout.exists()) {
			fout.delete();
		}
		saveFile(""+org.assertj.core.util.Files.temporaryFolder(), XML_FILE);

		//4. Run MCMC and look at results in Tracer, TreeAnnotator->FigTree
		makeSureXMLParses();
		long t1 = System.currentTimeMillis();
		System.err.println("total time: " + (t1 - t0)/1000 + " seconds");
		} catch (Exception e) {
			e.printStackTrace();
		
		}
	}
	

}

