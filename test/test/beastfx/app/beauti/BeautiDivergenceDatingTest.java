package test.beastfx.app.beauti;



import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.service.query.NodeQuery;

import beastfx.app.beauti.BeautiTabPane;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(ApplicationExtension.class)
public class BeautiDivergenceDatingTest extends BeautiBase {

	@Start
    public void start(Stage stage) {
		System.setProperty("beast.is.junit.testing", "true");
    	try {
    		BeautiTabPane tabPane = BeautiTabPane.main2(new String[] {}, stage);
    		this.doc = tabPane.doc;
            stage.show();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

    final static String PREFIX = "../beast2/doc/tutorials/DivergenceDating/figures/BEAUti_";

    @Test
    public void DivergenceDatingTutorial(FxRobot robot) throws Exception {
        long t0 = System.currentTimeMillis();

        // 0. Load primate-mtDNA.nex
        warning("// 0. Load primate-mtDNA.nex");
        importAlignment(NEXUS_DIR, new File("primate-mtDNA.nex"));

        robot.clickOn("#Mode").clickOn("Automatic set fix mean substitution rate flag");

		BeautiTabPane pane = robot.lookup("#BeautiTabPane").queryAs(BeautiTabPane.class);
		assertThat(pane.isVisible());
		String[] titles = tabTitles(pane);
        assertArrayEquals(titles, "[Partitions, Tip Dates, Site Model, Clock Model, Priors, MCMC]");
        System.err.println(Arrays.toString(titles));
        selectTab(robot, "Partitions");

        // check table
        printTableContents(robot);
        checkTableContents(robot, "[coding, primate-mtDNA, 12, 693, nucleotide, coding, coding, coding, false]*" +
                "[noncoding, primate-mtDNA, 12, 205, nucleotide, noncoding, noncoding, noncoding, false]*" +
                "[1stpos, primate-mtDNA, 12, 231, nucleotide, 1stpos, 1stpos, 1stpos, false]*" +
                "[2ndpos, primate-mtDNA, 12, 231, nucleotide, 2ndpos, 2ndpos, 2ndpos, false]*" +
                "[3rdpos, primate-mtDNA, 12, 231, nucleotide, 3rdpos, 3rdpos, 3rdpos, false]");

		ScrollPane scrollPane = robot.lookup(".scroll-pane").queryAs(ScrollPane.class);
		robot.interact(()-> {
			scrollPane.setVvalue(scrollPane.getVmax());
		});

        
        // assertThat(f).isNotNull();
        printBeautiState();
        assertStateEquals("Tree.t:noncoding", "birthRate.t:noncoding", "Tree.t:2ndpos", "birthRate.t:2ndpos", "Tree.t:1stpos", "birthRate.t:1stpos", "Tree.t:coding", "birthRate.t:coding", "Tree.t:3rdpos", "birthRate.t:3rdpos");
        assertOperatorsEqual("YuleBirthRateScaler.t:2ndpos", "YuleModelTreeRootScaler.t:2ndpos", "YuleModelUniformOperator.t:2ndpos", "YuleModelSubtreeSlide.t:2ndpos", "YuleModelNarrow.t:2ndpos", "YuleModelWide.t:2ndpos", "YuleModelWilsonBalding.t:2ndpos", "YuleModelBICEPSEpochTop.t:2ndpos", "YuleModelBICEPSEpochAll.t:2ndpos", "YuleModelBICEPSTreeFlex.t:2ndpos", "YuleBirthRateScaler.t:1stpos", "YuleModelTreeRootScaler.t:1stpos", "YuleModelUniformOperator.t:1stpos", "YuleModelSubtreeSlide.t:1stpos", "YuleModelNarrow.t:1stpos", "YuleModelWide.t:1stpos", "YuleModelWilsonBalding.t:1stpos", "YuleModelBICEPSEpochTop.t:1stpos", "YuleModelBICEPSEpochAll.t:1stpos", "YuleModelBICEPSTreeFlex.t:1stpos", "YuleBirthRateScaler.t:noncoding", "YuleModelTreeRootScaler.t:noncoding", "YuleModelUniformOperator.t:noncoding", "YuleModelSubtreeSlide.t:noncoding", "YuleModelNarrow.t:noncoding", "YuleModelWide.t:noncoding", "YuleModelWilsonBalding.t:noncoding", "YuleModelBICEPSEpochTop.t:noncoding", "YuleModelBICEPSEpochAll.t:noncoding", "YuleModelBICEPSTreeFlex.t:noncoding", "YuleBirthRateScaler.t:coding", "YuleModelTreeRootScaler.t:coding", "YuleModelUniformOperator.t:coding", "YuleModelSubtreeSlide.t:coding", "YuleModelNarrow.t:coding", "YuleModelWide.t:coding", "YuleModelWilsonBalding.t:coding", "YuleModelBICEPSEpochTop.t:coding", "YuleModelBICEPSEpochAll.t:coding", "YuleModelBICEPSTreeFlex.t:coding", "YuleBirthRateScaler.t:3rdpos", "YuleModelTreeRootScaler.t:3rdpos", "YuleModelUniformOperator.t:3rdpos", "YuleModelSubtreeSlide.t:3rdpos", "YuleModelNarrow.t:3rdpos", "YuleModelWide.t:3rdpos", "YuleModelWilsonBalding.t:3rdpos", "YuleModelBICEPSEpochTop.t:3rdpos", "YuleModelBICEPSEpochAll.t:3rdpos", "YuleModelBICEPSTreeFlex.t:3rdpos");
        assertPriorsEqual("YuleModel.t:coding", "YuleModel.t:noncoding", "YuleModel.t:1stpos", "YuleModel.t:2ndpos", "YuleModel.t:3rdpos", "YuleBirthRatePrior.t:noncoding", "YuleBirthRatePrior.t:2ndpos", "YuleBirthRatePrior.t:1stpos", "YuleBirthRatePrior.t:coding", "YuleBirthRatePrior.t:3rdpos");


        //1. Delete "coding" partition as it covers the same sites as the 1stpos, 2ndpos and 3rdpos partitions.
        warning("1. Delete \"coding\" partition as it covers the same sites as the 1stpos, 2ndpos and 3rdpos partitions.");
        selectTab(robot, "Partitions");
        selectPartitions(robot, 0);
        clickOnButtonWithText(robot, "-");
        printBeautiState();
        checkTableContents(robot, "[noncoding, primate-mtDNA, 12, 205, nucleotide, noncoding, noncoding, noncoding, false]*" +
                "[1stpos, primate-mtDNA, 12, 231, nucleotide, 1stpos, 1stpos, 1stpos, false]*" +
                "[2ndpos, primate-mtDNA, 12, 231, nucleotide, 2ndpos, 2ndpos, 2ndpos, false]*" +
                "[3rdpos, primate-mtDNA, 12, 231, nucleotide, 3rdpos, 3rdpos, 3rdpos, false]");
        assertStateEquals("Tree.t:noncoding", "birthRate.t:noncoding", "Tree.t:3rdpos", "birthRate.t:3rdpos", "Tree.t:1stpos", "birthRate.t:1stpos", "Tree.t:2ndpos", "birthRate.t:2ndpos");
        assertOperatorsEqual("YuleBirthRateScaler.t:2ndpos", "YuleModelTreeRootScaler.t:2ndpos", "YuleModelUniformOperator.t:2ndpos", "YuleModelSubtreeSlide.t:2ndpos", "YuleModelNarrow.t:2ndpos", "YuleModelWide.t:2ndpos", "YuleModelWilsonBalding.t:2ndpos", "YuleModelBICEPSEpochTop.t:2ndpos", "YuleModelBICEPSEpochAll.t:2ndpos", "YuleModelBICEPSTreeFlex.t:2ndpos", "YuleBirthRateScaler.t:1stpos", "YuleModelTreeRootScaler.t:1stpos", "YuleModelUniformOperator.t:1stpos", "YuleModelSubtreeSlide.t:1stpos", "YuleModelNarrow.t:1stpos", "YuleModelWide.t:1stpos", "YuleModelWilsonBalding.t:1stpos", "YuleModelBICEPSEpochTop.t:1stpos", "YuleModelBICEPSEpochAll.t:1stpos", "YuleModelBICEPSTreeFlex.t:1stpos", "YuleBirthRateScaler.t:noncoding", "YuleModelTreeRootScaler.t:noncoding", "YuleModelUniformOperator.t:noncoding", "YuleModelSubtreeSlide.t:noncoding", "YuleModelNarrow.t:noncoding", "YuleModelWide.t:noncoding", "YuleModelWilsonBalding.t:noncoding", "YuleModelBICEPSEpochTop.t:noncoding", "YuleModelBICEPSEpochAll.t:noncoding", "YuleModelBICEPSTreeFlex.t:noncoding", "YuleBirthRateScaler.t:3rdpos", "YuleModelTreeRootScaler.t:3rdpos", "YuleModelUniformOperator.t:3rdpos", "YuleModelSubtreeSlide.t:3rdpos", "YuleModelNarrow.t:3rdpos", "YuleModelWide.t:3rdpos", "YuleModelWilsonBalding.t:3rdpos", "YuleModelBICEPSEpochTop.t:3rdpos", "YuleModelBICEPSEpochAll.t:3rdpos", "YuleModelBICEPSTreeFlex.t:3rdpos");
        assertPriorsEqual("YuleModel.t:1stpos", "YuleModel.t:2ndpos", "YuleModel.t:3rdpos", "YuleModel.t:noncoding", "YuleBirthRatePrior.t:1stpos", "YuleBirthRatePrior.t:2ndpos", "YuleBirthRatePrior.t:3rdpos", "YuleBirthRatePrior.t:noncoding");


        //2a. Link trees...
        warning("2a. Link trees...");
        selectTab(robot, "Partitions");
        selectPartitions(robot, 0, 1, 2, 3);
        clickOnButtonWithText(robot, "Link Trees");
        printBeautiState();
        assertStateEquals("Tree.t:noncoding", "birthRate.t:noncoding", "clockRate.c:2ndpos", "clockRate.c:3rdpos", "clockRate.c:1stpos");
        assertOperatorsEqual("YuleBirthRateScaler.t:noncoding", "YuleModelTreeRootScaler.t:noncoding", "YuleModelUniformOperator.t:noncoding", "YuleModelSubtreeSlide.t:noncoding", "YuleModelNarrow.t:noncoding", "YuleModelWide.t:noncoding", "YuleModelWilsonBalding.t:noncoding", "YuleModelBICEPSEpochTop.t:noncoding", "YuleModelBICEPSEpochAll.t:noncoding", "YuleModelBICEPSTreeFlex.t:noncoding", "StrictClockRateScaler.c:1stpos", "strictClockUpDownOperator.c:1stpos", "StrictClockRateScaler.c:2ndpos", "strictClockUpDownOperator.c:2ndpos", "StrictClockRateScaler.c:3rdpos", "strictClockUpDownOperator.c:3rdpos");
        assertPriorsEqual("YuleModel.t:noncoding", "YuleBirthRatePrior.t:noncoding", "ClockPrior.c:1stpos", "ClockPrior.c:2ndpos", "ClockPrior.c:3rdpos");

        //2b. ...and call the tree "tree"
        warning("2b. ...and call the tree \"tree\"");
        selectTab(robot, "Partitions");
        
        setPartitionTableCell(robot, 7, "tree");
//        JTableCellFixture cell = beautiFrame.table().cell(TableCell.row(0).column(7));
//        Component editor = cell.editor();
//        JComboBoxFixture comboBox = new JComboBoxFixture(robot(), (JComboBox<?>) editor);
//        cell.startEditing();
//        comboBox.selectAllText();
//        comboBox.enterText("tree\n");
//        //comboBox.pressAndReleaseKeys(KeyEvent.VK_ENTER);
//        cell.stopEditing();
        checkTableContents(robot, "[noncoding, primate-mtDNA, 12, 205, nucleotide, noncoding, noncoding, tree, false]*" +
                "[1stpos, primate-mtDNA, 12, 231, nucleotide, 1stpos, 1stpos, tree, false]*" +
                "[2ndpos, primate-mtDNA, 12, 231, nucleotide, 2ndpos, 2ndpos, tree, false]*" +
                "[3rdpos, primate-mtDNA, 12, 231, nucleotide, 3rdpos, 3rdpos, tree, false]");
        printBeautiState();
        assertStateEquals("clockRate.c:2ndpos", "Tree.t:tree", "birthRate.t:tree", "clockRate.c:1stpos", "clockRate.c:3rdpos");
        assertOperatorsEqual("YuleBirthRateScaler.t:tree", "YuleModelTreeRootScaler.t:tree", "YuleModelUniformOperator.t:tree", "YuleModelSubtreeSlide.t:tree", "YuleModelNarrow.t:tree", "YuleModelWide.t:tree", "YuleModelWilsonBalding.t:tree", "YuleModelBICEPSEpochTop.t:tree", "YuleModelBICEPSEpochAll.t:tree", "YuleModelBICEPSTreeFlex.t:tree", "StrictClockRateScaler.c:1stpos", "strictClockUpDownOperator.c:1stpos", "StrictClockRateScaler.c:2ndpos", "strictClockUpDownOperator.c:2ndpos", "StrictClockRateScaler.c:3rdpos", "strictClockUpDownOperator.c:3rdpos");
        assertPriorsEqual("YuleModel.t:tree", "YuleBirthRatePrior.t:tree", "ClockPrior.c:1stpos", "ClockPrior.c:2ndpos", "ClockPrior.c:3rdpos");


        //3a. Link clocks
        warning("3a. Link clocks");
        selectTab(robot, "Partitions");
        selectPartitions(robot, 0, 1, 2, 3);
        clickOnButtonWithText(robot, "Link Clock Models");
        printBeautiState();

        
        saveFile("/tmp/", "beast.xml");

        //3b. and call the uncorrelated relaxed molecular clock "clock"
        warning("3b. and call the uncorrelated relaxed molecular clock \"clock\"");
        setPartitionTableCell(robot, 6, "clock");
//        cell = beautiFrame.table().cell(TableCell.row(0).column(6));
//        editor = cell.editor();
//        comboBox = new JComboBoxFixture(robot(), (JComboBox<?>) editor);
//        cell.startEditing();
//        comboBox.selectAllText();
//        comboBox.enterText("clock\n");
//        //comboBox.pressAndReleaseKeys(KeyEvent.VK_ENTER);
//        cell.stopEditing();
        printBeautiState();
        checkTableContents(robot, "[noncoding, primate-mtDNA, 12, 205, nucleotide, noncoding, clock, tree, false]*" +
                "[1stpos, primate-mtDNA, 12, 231, nucleotide, 1stpos, clock, tree, false]*" +
                "[2ndpos, primate-mtDNA, 12, 231, nucleotide, 2ndpos, clock, tree, false]*" +
                "[3rdpos, primate-mtDNA, 12, 231, nucleotide, 3rdpos, clock, tree, false]");
        assertStateEquals("Tree.t:tree", "birthRate.t:tree");
        assertOperatorsEqual("YuleBirthRateScaler.t:tree", "YuleModelTreeRootScaler.t:tree", "YuleModelUniformOperator.t:tree", "YuleModelSubtreeSlide.t:tree", "YuleModelNarrow.t:tree", "YuleModelWide.t:tree", "YuleModelWilsonBalding.t:tree", "YuleModelBICEPSEpochTop.t:tree", "YuleModelBICEPSEpochAll.t:tree", "YuleModelBICEPSTreeFlex.t:tree");
        assertPriorsEqual("YuleModel.t:tree", "YuleBirthRatePrior.t:tree");


        //4. Link site models temporarily in order to set the same model for all of them.
        warning("4. Link site models temporarily in order to set the same model for all of them.");
        selectTab(robot, "Partitions");
        selectPartitions(robot, 0, 1, 2, 3);
        clickOnButtonWithText(robot, "Link Site Models");
        printBeautiState();
        checkTableContents(robot, "[noncoding, primate-mtDNA, 12, 205, nucleotide, noncoding, clock, tree, false]*" +
                "[1stpos, primate-mtDNA, 12, 231, nucleotide, noncoding, clock, tree, false]*" +
                "[2ndpos, primate-mtDNA, 12, 231, nucleotide, noncoding, clock, tree, false]*" +
                "[3rdpos, primate-mtDNA, 12, 231, nucleotide, noncoding, clock, tree, false]");
        assertStateEquals("Tree.t:tree", "birthRate.t:tree");
        assertOperatorsEqual("YuleBirthRateScaler.t:tree", "YuleModelTreeRootScaler.t:tree", "YuleModelUniformOperator.t:tree", "YuleModelSubtreeSlide.t:tree", "YuleModelNarrow.t:tree", "YuleModelWide.t:tree", "YuleModelWilsonBalding.t:tree", "YuleModelBICEPSEpochTop.t:tree", "YuleModelBICEPSEpochAll.t:tree", "YuleModelBICEPSTreeFlex.t:tree");
        assertPriorsEqual("YuleModel.t:tree", "YuleBirthRatePrior.t:tree");

        //5. Set the site model to HKY+G4 (estimated)
        warning("5. Set the site model to HKY+G4 (estimated)");
        selectTab(robot, "Site Model");
        robot.clickOn("#substModelComboBox").clickOn("HKY");

        robot.clickOn("#gammaCategoryCount");
        robot.type(KeyCode.DIGIT4);

        
        // clickOnCheckbox(robot, "shape.isEstimated");
        //JCheckBoxFixture shapeIsEstimated = beautiFrame.checkBox("shape.isEstimated");
        //shapeIsEstimated.check();
        	
        checkTableContents(robot, "[noncoding, primate-mtDNA, 12, 205, nucleotide, noncoding, clock, tree, false]*" +
                "[1stpos, primate-mtDNA, 12, 231, nucleotide, noncoding, clock, tree, false]*" +
                "[2ndpos, primate-mtDNA, 12, 231, nucleotide, noncoding, clock, tree, false]*" +
                "[3rdpos, primate-mtDNA, 12, 231, nucleotide, noncoding, clock, tree, false]");
        Thread.sleep(500);
        assertStateEquals("Tree.t:tree", "birthRate.t:tree", "kappa.s:noncoding", "gammaShape.s:noncoding", "freqParameter.s:noncoding");
        assertOperatorsEqual("YuleBirthRateScaler.t:tree", "YuleModelTreeRootScaler.t:tree", "YuleModelUniformOperator.t:tree", "YuleModelSubtreeSlide.t:tree", "YuleModelNarrow.t:tree", "YuleModelWide.t:tree", "YuleModelWilsonBalding.t:tree", "YuleModelBICEPSEpochTop.t:tree", "YuleModelBICEPSEpochAll.t:tree", "YuleModelBICEPSTreeFlex.t:tree", "gammaShapeScaler.s:noncoding", "KappaScaler.s:noncoding", "FrequenciesExchanger.s:noncoding");
        assertPriorsEqual("YuleModel.t:tree", "YuleBirthRatePrior.t:tree", "KappaPrior.s:noncoding", "GammaShapePrior.s:noncoding", "FrequenciesPrior.s:noncoding");

        //6a. Unlink the site models,
        warning("6a. Unlink the site models,");
        selectTab(robot, "Partitions");
        selectPartitions(robot, 0, 1, 2, 3);
        clickOnButtonWithText(robot, "Unlink Site Models");
        printBeautiState();
        checkTableContents(robot, "[noncoding, primate-mtDNA, 12, 205, nucleotide, noncoding, clock, tree, false]*" +
                "[1stpos, primate-mtDNA, 12, 231, nucleotide, 1stpos, clock, tree, false]*" +
                "[2ndpos, primate-mtDNA, 12, 231, nucleotide, 2ndpos, clock, tree, false]*" +
                "[3rdpos, primate-mtDNA, 12, 231, nucleotide, 3rdpos, clock, tree, false]");

        //6b. and make sure that site model mutation rates are relative to 3rdpos (i.e. 3rdpos.mutationRate = 1, other 3 estimated).
        warning("6b. and make sure that site model mutation rates are relative to 3rdpos (i.e. 3rdpos.mutationRate = 1, other 3 estimated).");
        selectTab(robot, "Site Model");
//        JListFixture partitionList = beautiFrame.list();
//        partitionList.selectItem("noncoding");
        robot.clickOn(".list-view").clickOn("noncoding");
        clickOnNodesWithID(robot, "mutationRate.isEstimated");
//        mutationRateIsEstimated.requireNotSelected();
//        mutationRateIsEstimated.check();
//        mutationRateIsEstimated.requireSelected();

        robot.clickOn(".list-view").clickOn("1stpos");
//        partitionList.selectItem("1stpos");
        clickOnNodesWithID(robot, "mutationRate.isEstimated");
//        mutationRateIsEstimated = beautiFrame.checkBox("mutationRate.isEstimated");
//        mutationRateIsEstimated.requireNotSelected();
//        mutationRateIsEstimated.check();
//        mutationRateIsEstimated.requireSelected();

        robot.clickOn(".list-view").clickOn("2ndpos");
//        partitionList.selectItem("2ndpos");
        clickOnNodesWithID(robot, "mutationRate.isEstimated");
//        mutationRateIsEstimated = beautiFrame.checkBox("mutationRate.isEstimated");
//        mutationRateIsEstimated.requireNotSelected();
//        mutationRateIsEstimated.check();
//        mutationRateIsEstimated.requireSelected();

        robot.clickOn(".list-view").clickOn("3rdpos");
//        partitionList.selectItem("3rdpos");
        //clickOnNodesWithID(robot, "mutationRate.isEstimated");
//        mutationRateIsEstimated = beautiFrame.checkBox("mutationRate.isEstimated");
//        mutationRateIsEstimated.requireNotSelected();
        
        //CheckBox checkBox = robot.lookup("#mutationRate.isEstimated").queryAs(CheckBox.class);
        //assertThat(!checkBox.isSelected());

        printBeautiState();
        assertStateEquals("Tree.t:tree", "birthRate.t:tree", "gammaShape.s:noncoding", "kappa.s:noncoding", "kappa.s:1stpos", "gammaShape.s:1stpos", "kappa.s:2ndpos", "gammaShape.s:2ndpos", "kappa.s:3rdpos", "gammaShape.s:3rdpos", "mutationRate.s:noncoding", "mutationRate.s:1stpos", "mutationRate.s:2ndpos", "freqParameter.s:3rdpos", "freqParameter.s:1stpos", "freqParameter.s:2ndpos", "freqParameter.s:noncoding");
        assertOperatorsEqual("YuleBirthRateScaler.t:tree", "YuleModelTreeRootScaler.t:tree", "YuleModelUniformOperator.t:tree", "YuleModelSubtreeSlide.t:tree", "YuleModelNarrow.t:tree", "YuleModelWide.t:tree", "YuleModelWilsonBalding.t:tree", "YuleModelBICEPSEpochTop.t:tree", "YuleModelBICEPSEpochAll.t:tree", "YuleModelBICEPSTreeFlex.t:tree", "gammaShapeScaler.s:noncoding", "KappaScaler.s:noncoding", "KappaScaler.s:1stpos", "gammaShapeScaler.s:1stpos", "KappaScaler.s:2ndpos", "gammaShapeScaler.s:2ndpos", "KappaScaler.s:3rdpos", "gammaShapeScaler.s:3rdpos", "mutationRateScaler.s:noncoding", "mutationRateScaler.s:1stpos", "mutationRateScaler.s:2ndpos", "FrequenciesExchanger.s:2ndpos", "FrequenciesExchanger.s:3rdpos", "FrequenciesExchanger.s:1stpos", "FrequenciesExchanger.s:noncoding");
        assertPriorsEqual("YuleModel.t:tree", "YuleBirthRatePrior.t:tree", "GammaShapePrior.s:noncoding", "KappaPrior.s:noncoding", "FrequenciesPrior.s:noncoding", "GammaShapePrior.s:1stpos", "FrequenciesPrior.s:1stpos", "KappaPrior.s:1stpos", "GammaShapePrior.s:2ndpos", "FrequenciesPrior.s:2ndpos", "KappaPrior.s:2ndpos", "GammaShapePrior.s:3rdpos", "FrequenciesPrior.s:3rdpos", "KappaPrior.s:3rdpos", "MutationRatePrior.s:noncoding", "MutationRatePrior.s:1stpos", "MutationRatePrior.s:2ndpos");
        assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.3rdpos", "treeLikelihood.noncoding", "TreeHeight.t:tree", "YuleModel.t:tree", "birthRate.t:tree", "treeLikelihood.1stpos", "treeLikelihood.2ndpos", "gammaShape.s:noncoding", "kappa.s:noncoding", "kappa.s:1stpos", "gammaShape.s:1stpos", "kappa.s:2ndpos", "gammaShape.s:2ndpos", "kappa.s:3rdpos", "gammaShape.s:3rdpos", "mutationRate.s:noncoding", "mutationRate.s:1stpos", "mutationRate.s:2ndpos", "freqParameter.s:3rdpos", "freqParameter.s:1stpos", "freqParameter.s:2ndpos", "freqParameter.s:noncoding");

        //7a. Create a Normal calibration prior
        warning("7a. Create a Normal calibration prior");
        selectTab(robot, "Priors");
//        Component c = beautiFrame.robot().finder().findByName("addItem");
//        JButtonFixture addButton = new JButtonFixture(robot(), (JButton) c);
//        addButton.click();
        scrollToBottom(robot);
        clickOnButtonWithText(robot, "+ Add Prior");

        
        robot.clickOn("#idEntry").write("Human-Chimp");
        robot.clickOn("#listOfTaxonCandidates").clickOn("Homo_sapiens");
        clickOnButtonWithText(robot, ">>");
        robot.clickOn("#listOfTaxonCandidates").clickOn("Pan");
        clickOnButtonWithText(robot, ">>");
//        dialog.list("listOfTaxonCandidates").selectItems("Homo_sapiens", "Pan");
//        dialog.button(">>").click();
//        dialog.okButton().click();
        robot.clickOn("OK");
        

        //7b. and monophyletic constraint on Human-Chimp split of 6 +/- 0.5.
		selectTab(robot, "Priors");
		clickOnNodesWithID(robot, "Human-Chimp.prior.isMonophyletic");
		clickOnNodesWithID(robot, "Human-Chimp.prior.distr");
		robot.clickOn("Normal");
        scrollToBottom(robot);
		clickOnNodesWithID(robot, "Human-Chimp.prior.editButton");
        scrollToBottom(robot);
		

		robot.doubleClickOn("#mean").write("6");//.selectAll().setText("6");
		robot.doubleClickOn("#sigma").write("0.5");//.selectAll().setText("0.5");
		
		printBeautiState();
        assertStateEquals("Tree.t:tree", "birthRate.t:tree", "kappa.s:noncoding", "gammaShape.s:noncoding", "kappa.s:1stpos", "gammaShape.s:1stpos", "kappa.s:2ndpos", "gammaShape.s:2ndpos", "kappa.s:3rdpos", "gammaShape.s:3rdpos", "mutationRate.s:noncoding", "mutationRate.s:1stpos", "mutationRate.s:2ndpos", "clockRate.c:clock", "freqParameter.s:1stpos", "freqParameter.s:3rdpos", "freqParameter.s:2ndpos", "freqParameter.s:noncoding");
        assertOperatorsEqual("YuleBirthRateScaler.t:tree", "YuleModelTreeRootScaler.t:tree", "YuleModelUniformOperator.t:tree", "YuleModelSubtreeSlide.t:tree", "YuleModelNarrow.t:tree", "YuleModelWide.t:tree", "YuleModelWilsonBalding.t:tree", "YuleModelBICEPSEpochTop.t:tree", "YuleModelBICEPSEpochAll.t:tree", "YuleModelBICEPSTreeFlex.t:tree", "gammaShapeScaler.s:noncoding", "KappaScaler.s:noncoding", "KappaScaler.s:1stpos", "gammaShapeScaler.s:1stpos", "KappaScaler.s:2ndpos", "gammaShapeScaler.s:2ndpos", "KappaScaler.s:3rdpos", "gammaShapeScaler.s:3rdpos", "mutationRateScaler.s:noncoding", "mutationRateScaler.s:1stpos", "mutationRateScaler.s:2ndpos", "StrictClockRateScaler.c:clock", "strictClockUpDownOperator.c:clock", "FrequenciesExchanger.s:2ndpos", "FrequenciesExchanger.s:1stpos", "FrequenciesExchanger.s:3rdpos", "FrequenciesExchanger.s:noncoding");
        assertPriorsEqual("YuleModel.t:tree", "YuleBirthRatePrior.t:tree", "GammaShapePrior.s:1stpos", "GammaShapePrior.s:2ndpos", "GammaShapePrior.s:3rdpos", "GammaShapePrior.s:noncoding", "KappaPrior.s:1stpos", "KappaPrior.s:2ndpos", "KappaPrior.s:3rdpos", "KappaPrior.s:noncoding", "MutationRatePrior.s:1stpos", "MutationRatePrior.s:2ndpos", "MutationRatePrior.s:noncoding", "Human-Chimp.prior", "ClockPrior.c:clock", "FrequenciesPrior.s:noncoding", "FrequenciesPrior.s:1stpos", "FrequenciesPrior.s:2ndpos", "FrequenciesPrior.s:3rdpos");
        assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.3rdpos", "treeLikelihood.noncoding", "TreeHeight.t:tree", "YuleModel.t:tree", "birthRate.t:tree", "treeLikelihood.1stpos", "treeLikelihood.2ndpos", "kappa.s:noncoding", "gammaShape.s:noncoding", "kappa.s:1stpos", "gammaShape.s:1stpos", "kappa.s:2ndpos", "gammaShape.s:2ndpos", "kappa.s:3rdpos", "gammaShape.s:3rdpos", "mutationRate.s:noncoding", "mutationRate.s:1stpos", "mutationRate.s:2ndpos", "Human-Chimp.prior", "clockRate.c:clock", "freqParameter.s:3rdpos", "freqParameter.s:2ndpos", "freqParameter.s:noncoding", "freqParameter.s:1stpos");

        //8. Run MCMC and look at results in Tracer, TreeAnnotator->FigTree
        warning("8. Run MCMC and look at results in Tracer, TreeAnnotator->FigTree");
        File fout = new File(org.assertj.core.util.Files.temporaryFolder() + "/primates.xml");
        if (fout.exists()) {
            fout.delete();
        }

		// 9. Set up MCMC parameters
		warning("8. Set up MCMC parameters");
		selectTab(robot, "MCMC");
		robot.doubleClickOn("#chainLength").write("2000000");
//		beautiFrame.textBox("chainLength").selectAll().setText("2000000");


        fout = new File(org.assertj.core.util.Files.temporaryFolder() + "/divtutorial.xml");
        if (fout.exists()) {
            fout.delete();
        }
		saveFile(""+org.assertj.core.util.Files.temporaryFolder(), "divtutorial.xml");

		makeSureXMLParses();

        long t1 = System.currentTimeMillis();
        System.err.println("total time: " + (t1 - t0) / 1000 + " seconds");

    }

    



	@Test
    public void DivergenceDatingTutorialWithEmpiricalFreqs(FxRobot robot) throws Exception {
        try {
            long t0 = System.currentTimeMillis();
            //ScreenshotTaker screenshotTaker = new ScreenshotTaker();
            //beauti.frame.setSize(1200, 800);

            String BASE_DIR = PREFIX.substring(0, PREFIX.lastIndexOf('/'));
            File dir = new File(BASE_DIR);
            if (dir.listFiles() != null)
            for (File file : dir.listFiles()) {
                if (file.getAbsolutePath().contains(PREFIX) && file.getName().endsWith(".png")) {
                    file.delete();
                }
            }

            // 0. Load primate-mtDNA.nex
            warning("// 0. Load primate-mtDNA.nex");
            importAlignment(NEXUS_DIR, new File("primate-mtDNA.nex"));
            screenshot(PREFIX + "DataPartitions.png");

            robot.clickOn("#Mode").clickOn("Automatic set fix mean substitution rate flag");

    		BeautiTabPane pane = robot.lookup("#BeautiTabPane").queryAs(BeautiTabPane.class);
    		String[] titles = tabTitles(pane);
            assertArrayEquals(titles, "[Partitions, Tip Dates, Site Model, Clock Model, Priors, MCMC]");
            System.err.println(Arrays.toString(titles));
            selectTab(robot, "Partitions");

            // inspect alignment
//            robot.doubleClickOn(".table-view");
//            robot.clickOn("#UseColor");
//            robot.clickOn("Close");
//            screenshot(PREFIX + "Alignment.png");
            
//            JTableFixture t = beautiFrame.table();
//            t.selectCell(TableCell.row(0).column(2)).doubleClick();
//            DialogFixture dlg = WindowFinder.findDialog("AlignmentViewer").using(robot());
//            dlg.target().setSize(768, 300);
//            dlg.checkBox("UseColor").check();
//            screenshotTaker.saveComponentAsPng(dlg.target(), PREFIX + "Alignment.png");
//            dlg.close();

            
            
            // check table
            printTableContents(robot);
            checkTableContents(robot, "[coding, primate-mtDNA, 12, 693, nucleotide, coding, coding, coding, false]*" +
                    "[noncoding, primate-mtDNA, 12, 205, nucleotide, noncoding, noncoding, noncoding, false]*" +
                    "[1stpos, primate-mtDNA, 12, 231, nucleotide, 1stpos, 1stpos, 1stpos, false]*" +
                    "[2ndpos, primate-mtDNA, 12, 231, nucleotide, 2ndpos, 2ndpos, 2ndpos, false]*" +
                    "[3rdpos, primate-mtDNA, 12, 231, nucleotide, 3rdpos, 3rdpos, 3rdpos, false]");

//            assertThat(f).isNotNull();
            printBeautiState();
            assertStateEquals("Tree.t:1stpos", "birthRate.t:1stpos", "Tree.t:noncoding", "birthRate.t:noncoding", "Tree.t:coding", "birthRate.t:coding", "Tree.t:3rdpos", "birthRate.t:3rdpos", "Tree.t:2ndpos", "birthRate.t:2ndpos");
            assertOperatorsEqual("YuleBirthRateScaler.t:noncoding", "YuleModelTreeRootScaler.t:noncoding", "YuleModelUniformOperator.t:noncoding", "YuleModelSubtreeSlide.t:noncoding", "YuleModelNarrow.t:noncoding", "YuleModelWide.t:noncoding", "YuleModelWilsonBalding.t:noncoding", "YuleModelBICEPSEpochTop.t:noncoding", "YuleModelBICEPSEpochAll.t:noncoding", "YuleModelBICEPSTreeFlex.t:noncoding", "YuleBirthRateScaler.t:3rdpos", "YuleModelTreeRootScaler.t:3rdpos", "YuleModelUniformOperator.t:3rdpos", "YuleModelSubtreeSlide.t:3rdpos", "YuleModelNarrow.t:3rdpos", "YuleModelWide.t:3rdpos", "YuleModelWilsonBalding.t:3rdpos", "YuleModelBICEPSEpochTop.t:3rdpos", "YuleModelBICEPSEpochAll.t:3rdpos", "YuleModelBICEPSTreeFlex.t:3rdpos", "YuleBirthRateScaler.t:coding", "YuleModelTreeRootScaler.t:coding", "YuleModelUniformOperator.t:coding", "YuleModelSubtreeSlide.t:coding", "YuleModelNarrow.t:coding", "YuleModelWide.t:coding", "YuleModelWilsonBalding.t:coding", "YuleModelBICEPSEpochTop.t:coding", "YuleModelBICEPSEpochAll.t:coding", "YuleModelBICEPSTreeFlex.t:coding", "YuleBirthRateScaler.t:1stpos", "YuleModelTreeRootScaler.t:1stpos", "YuleModelUniformOperator.t:1stpos", "YuleModelSubtreeSlide.t:1stpos", "YuleModelNarrow.t:1stpos", "YuleModelWide.t:1stpos", "YuleModelWilsonBalding.t:1stpos", "YuleModelBICEPSEpochTop.t:1stpos", "YuleModelBICEPSEpochAll.t:1stpos", "YuleModelBICEPSTreeFlex.t:1stpos", "YuleBirthRateScaler.t:2ndpos", "YuleModelTreeRootScaler.t:2ndpos", "YuleModelUniformOperator.t:2ndpos", "YuleModelSubtreeSlide.t:2ndpos", "YuleModelNarrow.t:2ndpos", "YuleModelWide.t:2ndpos", "YuleModelWilsonBalding.t:2ndpos", "YuleModelBICEPSEpochTop.t:2ndpos", "YuleModelBICEPSEpochAll.t:2ndpos", "YuleModelBICEPSTreeFlex.t:2ndpos");
            assertPriorsEqual("YuleModel.t:coding", "YuleModel.t:noncoding", "YuleModel.t:1stpos", "YuleModel.t:2ndpos", "YuleModel.t:3rdpos", "YuleBirthRatePrior.t:1stpos", "YuleBirthRatePrior.t:noncoding", "YuleBirthRatePrior.t:coding", "YuleBirthRatePrior.t:3rdpos", "YuleBirthRatePrior.t:2ndpos");
            assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.1stpos", "TreeHeight.t:1stpos", "YuleModel.t:1stpos", "birthRate.t:1stpos", "treeLikelihood.noncoding", "TreeHeight.t:noncoding", "YuleModel.t:noncoding", "birthRate.t:noncoding", "treeLikelihood.coding", "TreeHeight.t:coding", "YuleModel.t:coding", "birthRate.t:coding", "treeLikelihood.3rdpos", "TreeHeight.t:3rdpos", "YuleModel.t:3rdpos", "birthRate.t:3rdpos", "treeLikelihood.2ndpos", "TreeHeight.t:2ndpos", "YuleModel.t:2ndpos", "birthRate.t:2ndpos");


            //1. Delete "coding" partition as it covers the same sites as the 1stpos, 2ndpos and 3rdpos partitions.
            warning("1. Delete \"coding\" partition as it covers the same sites as the 1stpos, 2ndpos and 3rdpos partitions.");
            selectTab(robot, "Partitions");
            selectPartitions(robot, 0);
            clickOnButtonWithText(robot, "-");
            printBeautiState();
            checkTableContents(robot, "[noncoding, primate-mtDNA, 12, 205, nucleotide, noncoding, noncoding, noncoding, false]*" +
                    "[1stpos, primate-mtDNA, 12, 231, nucleotide, 1stpos, 1stpos, 1stpos, false]*" +
                    "[2ndpos, primate-mtDNA, 12, 231, nucleotide, 2ndpos, 2ndpos, 2ndpos, false]*" +
                    "[3rdpos, primate-mtDNA, 12, 231, nucleotide, 3rdpos, 3rdpos, 3rdpos, false]");
            assertStateEquals("Tree.t:noncoding", "birthRate.t:noncoding", "Tree.t:2ndpos", "birthRate.t:2ndpos", "Tree.t:3rdpos", "birthRate.t:3rdpos", "Tree.t:1stpos", "birthRate.t:1stpos");
            assertOperatorsEqual("YuleBirthRateScaler.t:noncoding", "YuleModelTreeRootScaler.t:noncoding", "YuleModelUniformOperator.t:noncoding", "YuleModelSubtreeSlide.t:noncoding", "YuleModelNarrow.t:noncoding", "YuleModelWide.t:noncoding", "YuleModelWilsonBalding.t:noncoding", "YuleModelBICEPSEpochTop.t:noncoding", "YuleModelBICEPSEpochAll.t:noncoding", "YuleModelBICEPSTreeFlex.t:noncoding", "YuleBirthRateScaler.t:3rdpos", "YuleModelTreeRootScaler.t:3rdpos", "YuleModelUniformOperator.t:3rdpos", "YuleModelSubtreeSlide.t:3rdpos", "YuleModelNarrow.t:3rdpos", "YuleModelWide.t:3rdpos", "YuleModelWilsonBalding.t:3rdpos", "YuleModelBICEPSEpochTop.t:3rdpos", "YuleModelBICEPSEpochAll.t:3rdpos", "YuleModelBICEPSTreeFlex.t:3rdpos", "YuleBirthRateScaler.t:1stpos", "YuleModelTreeRootScaler.t:1stpos", "YuleModelUniformOperator.t:1stpos", "YuleModelSubtreeSlide.t:1stpos", "YuleModelNarrow.t:1stpos", "YuleModelWide.t:1stpos", "YuleModelWilsonBalding.t:1stpos", "YuleModelBICEPSEpochTop.t:1stpos", "YuleModelBICEPSEpochAll.t:1stpos", "YuleModelBICEPSTreeFlex.t:1stpos", "YuleBirthRateScaler.t:2ndpos", "YuleModelTreeRootScaler.t:2ndpos", "YuleModelUniformOperator.t:2ndpos", "YuleModelSubtreeSlide.t:2ndpos", "YuleModelNarrow.t:2ndpos", "YuleModelWide.t:2ndpos", "YuleModelWilsonBalding.t:2ndpos", "YuleModelBICEPSEpochTop.t:2ndpos", "YuleModelBICEPSEpochAll.t:2ndpos", "YuleModelBICEPSTreeFlex.t:2ndpos");
            assertPriorsEqual("YuleModel.t:noncoding", "YuleModel.t:1stpos", "YuleModel.t:2ndpos", "YuleModel.t:3rdpos", "YuleBirthRatePrior.t:noncoding", "YuleBirthRatePrior.t:2ndpos", "YuleBirthRatePrior.t:3rdpos", "YuleBirthRatePrior.t:1stpos");
            assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.noncoding", "TreeHeight.t:noncoding", "YuleModel.t:noncoding", "birthRate.t:noncoding", "treeLikelihood.2ndpos", "TreeHeight.t:2ndpos", "YuleModel.t:2ndpos", "birthRate.t:2ndpos", "treeLikelihood.3rdpos", "TreeHeight.t:3rdpos", "YuleModel.t:3rdpos", "birthRate.t:3rdpos", "treeLikelihood.1stpos", "TreeHeight.t:1stpos", "YuleModel.t:1stpos", "birthRate.t:1stpos");

            //2a. Link trees...
            warning("2a. Link trees...");
            selectTab(robot, "Partitions");
            selectPartitions(robot, 0, 1, 2, 3);
            clickOnButtonWithText(robot, "Link Trees");
            printBeautiState();
            assertStateEquals("Tree.t:noncoding", "birthRate.t:noncoding", "clockRate.c:2ndpos", "clockRate.c:1stpos", "clockRate.c:3rdpos");
            assertOperatorsEqual("YuleBirthRateScaler.t:noncoding", "YuleModelTreeRootScaler.t:noncoding", "YuleModelUniformOperator.t:noncoding", "YuleModelSubtreeSlide.t:noncoding", "YuleModelNarrow.t:noncoding", "YuleModelWide.t:noncoding", "YuleModelWilsonBalding.t:noncoding", "YuleModelBICEPSEpochTop.t:noncoding", "YuleModelBICEPSEpochAll.t:noncoding", "YuleModelBICEPSTreeFlex.t:noncoding", "StrictClockRateScaler.c:2ndpos", "strictClockUpDownOperator.c:2ndpos", "StrictClockRateScaler.c:1stpos", "strictClockUpDownOperator.c:1stpos", "StrictClockRateScaler.c:3rdpos", "strictClockUpDownOperator.c:3rdpos");
            assertPriorsEqual("YuleModel.t:noncoding", "YuleBirthRatePrior.t:noncoding", "ClockPrior.c:2ndpos", "ClockPrior.c:1stpos", "ClockPrior.c:3rdpos");
            assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.noncoding", "TreeHeight.t:noncoding", "YuleModel.t:noncoding", "birthRate.t:noncoding", "treeLikelihood.2ndpos", "treeLikelihood.3rdpos", "treeLikelihood.1stpos", "clockRate.c:2ndpos", "clockRate.c:1stpos", "clockRate.c:3rdpos");

            //2b. ...and call the tree "tree"
            warning("2b. ...and call the tree \"tree\"");
            selectTab(robot, "Partitions");
            setPartitionTableCell(robot, 7, "tree");
//            JTableCellFixture cell = beautiFrame.table().cell(TableCell.row(0).column(7));
//            Component editor = cell.editor();
//            JComboBoxFixture comboBox = new JComboBoxFixture(robot(), (JComboBox<?>) editor);
//            cell.startEditing();
//            comboBox.selectAllText();
//            comboBox.enterText("tree\n");
            //comboBox.pressAndReleaseKeys(KeyEvent.VK_ENTER);
            // cell.stopEditing();
            checkTableContents(robot, "[noncoding, primate-mtDNA, 12, 205, nucleotide, noncoding, noncoding, tree, false]*" +
                    "[1stpos, primate-mtDNA, 12, 231, nucleotide, 1stpos, 1stpos, tree, false]*" +
                    "[2ndpos, primate-mtDNA, 12, 231, nucleotide, 2ndpos, 2ndpos, tree, false]*" +
                    "[3rdpos, primate-mtDNA, 12, 231, nucleotide, 3rdpos, 3rdpos, tree, false]");
            printBeautiState();
            assertStateEquals("clockRate.c:2ndpos", "Tree.t:tree", "birthRate.t:tree", "clockRate.c:1stpos", "clockRate.c:3rdpos");
            assertOperatorsEqual("YuleBirthRateScaler.t:tree", "YuleModelTreeRootScaler.t:tree", "YuleModelUniformOperator.t:tree", "YuleModelSubtreeSlide.t:tree", "YuleModelNarrow.t:tree", "YuleModelWide.t:tree", "YuleModelWilsonBalding.t:tree", "YuleModelBICEPSEpochTop.t:tree", "YuleModelBICEPSEpochAll.t:tree", "YuleModelBICEPSTreeFlex.t:tree", "StrictClockRateScaler.c:2ndpos", "strictClockUpDownOperator.c:2ndpos", "StrictClockRateScaler.c:1stpos", "strictClockUpDownOperator.c:1stpos", "StrictClockRateScaler.c:3rdpos", "strictClockUpDownOperator.c:3rdpos");
            assertPriorsEqual("YuleModel.t:tree", "YuleBirthRatePrior.t:tree", "ClockPrior.c:1stpos", "ClockPrior.c:2ndpos", "ClockPrior.c:3rdpos");


            //3a. Link clocks
            warning("3a. Link clocks");
            selectTab(robot, "Partitions");
            selectPartitions(robot, 0, 1, 2, 3);
            clickOnButtonWithText(robot, "Link Clock Models");

            //3b. and call the uncorrelated relaxed molecular clock "clock"
            warning("3b. and call the uncorrelated relaxed molecular clock \"clock\"");
            setPartitionTableCell(robot, 6, "clock");
//            cell = beautiFrame.table().cell(TableCell.row(0).column(6));
//            editor = cell.editor();
//            comboBox = new JComboBoxFixture(robot(), (JComboBox<?>) editor);
//            cell.startEditing();
//            comboBox.selectAllText();
//            comboBox.enterText("clock\n");
//            //comboBox.pressAndReleaseKeys(KeyEvent.VK_ENTER);
//            cell.stopEditing();
            printBeautiState();
            checkTableContents(robot, "[noncoding, primate-mtDNA, 12, 205, nucleotide, noncoding, clock, tree, false]*" +
                    "[1stpos, primate-mtDNA, 12, 231, nucleotide, 1stpos, clock, tree, false]*" +
                    "[2ndpos, primate-mtDNA, 12, 231, nucleotide, 2ndpos, clock, tree, false]*" +
                    "[3rdpos, primate-mtDNA, 12, 231, nucleotide, 3rdpos, clock, tree, false]");
            assertStateEquals("Tree.t:tree", "birthRate.t:tree");
            assertOperatorsEqual("YuleBirthRateScaler.t:tree", "YuleModelTreeRootScaler.t:tree", "YuleModelUniformOperator.t:tree", "YuleModelSubtreeSlide.t:tree", "YuleModelNarrow.t:tree", "YuleModelWide.t:tree", "YuleModelWilsonBalding.t:tree", "YuleModelBICEPSEpochTop.t:tree", "YuleModelBICEPSEpochAll.t:tree", "YuleModelBICEPSTreeFlex.t:tree");
            assertPriorsEqual("YuleModel.t:tree", "YuleBirthRatePrior.t:tree");
            screenshot(PREFIX + "DataPartitions_final.png");
            //screenshotTaker.saveComponentAsPng(beauti.frame, PREFIX + "DataPartitions_final.png");

            //4. Link site models temporarily in order to set the same model for all of them.
            warning("4. Link site models temporarily in order to set the same model for all of them.");
            selectTab(robot, "Partitions");
            selectPartitions(robot, 0, 1, 2, 3);
            clickOnButtonWithText(robot, "Link Site Models");
            printBeautiState();
            checkTableContents(robot, "[noncoding, primate-mtDNA, 12, 205, nucleotide, noncoding, clock, tree, false]*" +
                    "[1stpos, primate-mtDNA, 12, 231, nucleotide, noncoding, clock, tree, false]*" +
                    "[2ndpos, primate-mtDNA, 12, 231, nucleotide, noncoding, clock, tree, false]*" +
                    "[3rdpos, primate-mtDNA, 12, 231, nucleotide, noncoding, clock, tree, false]");
            assertStateEquals("Tree.t:tree", "birthRate.t:tree");
            assertOperatorsEqual("YuleBirthRateScaler.t:tree", "YuleModelTreeRootScaler.t:tree", "YuleModelUniformOperator.t:tree", "YuleModelSubtreeSlide.t:tree", "YuleModelNarrow.t:tree", "YuleModelWide.t:tree", "YuleModelWilsonBalding.t:tree", "YuleModelBICEPSEpochTop.t:tree", "YuleModelBICEPSEpochAll.t:tree", "YuleModelBICEPSTreeFlex.t:tree");
            assertPriorsEqual("YuleModel.t:tree", "YuleBirthRatePrior.t:tree");

            //5. Set the site model to HKY+G4 (estimated)
            warning("5. Set the site model to HKY+G4 (estimated)");
            selectTab(robot, "Site Model");
            robot.clickOn("#substModelComboBox").clickOn("HKY");

            robot.clickOn("#frequenciesComboBox").clickOn("Empirical");

            robot.clickOn("#gammaCategoryCount");
            robot.type(KeyCode.DIGIT4);

            // clickOnCheckbox(robot, "shape.isEstimated");

            clickOnNodesWithID(robot, "mutationRate.isEstimated");
            clickOnNodesWithID(robot, "FixMeanMutationRate");
            setCheckBox(robot, "#FixMeanMutationRate");
            screenshot(PREFIX + "Model.png");
            printBeautiState();
            checkTableContents(robot, "[noncoding, primate-mtDNA, 12, 205, nucleotide, noncoding, clock, tree, false]*" +
                    "[1stpos, primate-mtDNA, 12, 231, nucleotide, noncoding, clock, tree, false]*" +
                    "[2ndpos, primate-mtDNA, 12, 231, nucleotide, noncoding, clock, tree, false]*" +
                    "[3rdpos, primate-mtDNA, 12, 231, nucleotide, noncoding, clock, tree, false]");
            assertStateEquals("Tree.t:tree", "birthRate.t:tree", "kappa.s:noncoding", "gammaShape.s:noncoding", "mutationRate.s:noncoding");
            assertOperatorsEqual("YuleBirthRateScaler.t:tree", "YuleModelTreeRootScaler.t:tree", "YuleModelUniformOperator.t:tree", "YuleModelSubtreeSlide.t:tree", "YuleModelNarrow.t:tree", "YuleModelWide.t:tree", "YuleModelWilsonBalding.t:tree", "YuleModelBICEPSEpochTop.t:tree", "YuleModelBICEPSEpochAll.t:tree", "YuleModelBICEPSTreeFlex.t:tree", "FixMeanMutationRatesOperator", "gammaShapeScaler.s:noncoding", "KappaScaler.s:noncoding");
            assertPriorsEqual("YuleModel.t:tree", "YuleBirthRatePrior.t:tree", "KappaPrior.s:noncoding", "GammaShapePrior.s:noncoding");
            assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.1stpos", "treeLikelihood.2ndpos", "treeLikelihood.3rdpos", "treeLikelihood.noncoding", "TreeHeight.t:tree", "YuleModel.t:tree", "birthRate.t:tree", "kappa.s:noncoding", "gammaShape.s:noncoding", "mutationRate.s:noncoding");

            //6 Unlink the site models,
            warning("6 Unlink the site models,");
            selectTab(robot, "Partitions");
            selectPartitions(robot, 0, 1, 2, 3);
            clickOnButtonWithText(robot, "Unlink Site Models");
            printBeautiState();
            checkTableContents(robot, "[noncoding, primate-mtDNA, 12, 205, nucleotide, noncoding, clock, tree, false]*" +
                    "[1stpos, primate-mtDNA, 12, 231, nucleotide, 1stpos, clock, tree, false]*" +
                    "[2ndpos, primate-mtDNA, 12, 231, nucleotide, 2ndpos, clock, tree, false]*" +
                    "[3rdpos, primate-mtDNA, 12, 231, nucleotide, 3rdpos, clock, tree, false]");

            assertStateEquals("Tree.t:tree", "birthRate.t:tree", "kappa.s:noncoding", "gammaShape.s:noncoding", "mutationRate.s:noncoding", "kappa.s:1stpos", "gammaShape.s:1stpos", "mutationRate.s:1stpos", "kappa.s:2ndpos", "gammaShape.s:2ndpos", "mutationRate.s:2ndpos", "kappa.s:3rdpos", "gammaShape.s:3rdpos", "mutationRate.s:3rdpos");
            assertOperatorsEqual("YuleBirthRateScaler.t:tree", "YuleModelTreeRootScaler.t:tree", "YuleModelUniformOperator.t:tree", "YuleModelSubtreeSlide.t:tree", "YuleModelNarrow.t:tree", "YuleModelWide.t:tree", "YuleModelWilsonBalding.t:tree", "YuleModelBICEPSEpochTop.t:tree", "YuleModelBICEPSEpochAll.t:tree", "YuleModelBICEPSTreeFlex.t:tree", "FixMeanMutationRatesOperator", "gammaShapeScaler.s:noncoding", "KappaScaler.s:noncoding", "gammaShapeScaler.s:1stpos", "KappaScaler.s:1stpos", "gammaShapeScaler.s:2ndpos", "KappaScaler.s:2ndpos", "gammaShapeScaler.s:3rdpos", "KappaScaler.s:3rdpos");
            assertPriorsEqual("YuleModel.t:tree", "YuleBirthRatePrior.t:tree", "KappaPrior.s:noncoding", "GammaShapePrior.s:noncoding", "GammaShapePrior.s:1stpos", "KappaPrior.s:1stpos", "GammaShapePrior.s:2ndpos", "KappaPrior.s:2ndpos", "GammaShapePrior.s:3rdpos", "KappaPrior.s:3rdpos");
            assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.1stpos", "treeLikelihood.2ndpos", "treeLikelihood.3rdpos", "treeLikelihood.noncoding", "TreeHeight.t:tree", "YuleModel.t:tree", "birthRate.t:tree", "kappa.s:noncoding", "gammaShape.s:noncoding", "mutationRate.s:noncoding", "kappa.s:1stpos", "gammaShape.s:1stpos", "mutationRate.s:1stpos", "kappa.s:2ndpos", "gammaShape.s:2ndpos", "mutationRate.s:2ndpos", "kappa.s:3rdpos", "gammaShape.s:3rdpos", "mutationRate.s:3rdpos");


            //7a. Create a Normal calibration prior
            warning("7a. Create a Normal calibration prior");
            selectTab(robot, "Priors");
            robot.clickOn("#TreeDistribution").clickOn("Calibrated Yule Model");
            screenshot(PREFIX + "Prior1.png");

    		scrollToBottom(robot);
            robot.clickOn("#addItem");
//            Component c = beautiFrame.robot().finder().findByName("addItem");
//            JButtonFixture addButton = new JButtonFixture(robot(), (JButton) c);
//            addButton.click();
            //JOptionPaneFixture dialog = new JOptionPaneFixture(robot());
            robot.clickOn("#idEntry"); robot.write("Human-Chimp");
            robot.clickOn("#listOfTaxonCandidates").clickOn("Homo_sapiens");
            clickOnButtonWithText(robot, ">>");
            robot.clickOn("#listOfTaxonCandidates").clickOn("Pan");
            clickOnButtonWithText(robot, ">>");
    		robot.clickOn("OK");
            printBeautiState();
            assertStateEquals("Tree.t:tree", "kappa.s:noncoding", "gammaShape.s:noncoding", "mutationRate.s:noncoding", "kappa.s:1stpos", "gammaShape.s:1stpos", "mutationRate.s:1stpos", "kappa.s:2ndpos", "gammaShape.s:2ndpos", "mutationRate.s:2ndpos", "kappa.s:3rdpos", "gammaShape.s:3rdpos", "mutationRate.s:3rdpos", "birthRateY.t:tree");
            assertOperatorsEqual("FixMeanMutationRatesOperator", "gammaShapeScaler.s:noncoding", "KappaScaler.s:noncoding", "gammaShapeScaler.s:1stpos", "KappaScaler.s:1stpos", "gammaShapeScaler.s:2ndpos", "KappaScaler.s:2ndpos", "gammaShapeScaler.s:3rdpos", "KappaScaler.s:3rdpos", "CalibratedYuleModelBICEPSEpochTop.t:tree", "CalibratedYuleModelBICEPSEpochAll.t:tree", "CalibratedYuleModelBICEPSTreeFlex.t:tree", "CalibratedYuleModelTreeRootScaler.t:tree", "CalibratedYuleModelUniformOperator.t:tree", "CalibratedYuleModelSubtreeSlide.t:tree", "CalibratedYuleModelNarrow.t:tree", "CalibratedYuleModelWide.t:tree", "CalibratedYuleModelWilsonBalding.t:tree", "CalibratedYuleBirthRateScaler.t:tree");
            assertPriorsEqual("CalibratedYuleModel.t:tree", "CalibratedYuleBirthRatePrior.t:tree", "GammaShapePrior.s:1stpos", "GammaShapePrior.s:2ndpos", "GammaShapePrior.s:3rdpos", "GammaShapePrior.s:noncoding", "KappaPrior.s:1stpos", "KappaPrior.s:2ndpos", "KappaPrior.s:3rdpos", "KappaPrior.s:noncoding", "Human-Chimp.prior");
            assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.1stpos", "treeLikelihood.2ndpos", "treeLikelihood.3rdpos", "treeLikelihood.noncoding", "TreeHeight.t:tree", "kappa.s:noncoding", "gammaShape.s:noncoding", "mutationRate.s:noncoding", "kappa.s:1stpos", "gammaShape.s:1stpos", "mutationRate.s:1stpos", "kappa.s:2ndpos", "gammaShape.s:2ndpos", "mutationRate.s:2ndpos", "kappa.s:3rdpos", "gammaShape.s:3rdpos", "mutationRate.s:3rdpos", "CalibratedYuleModel.t:tree", "birthRateY.t:tree", "Human-Chimp.prior");

            //7b. and monophyletic constraint on Human-Chimp split of 6 +/- 0.5.
            warning("7b. and monophyletic constraint on Human-Chimp split of 6 +/- 0.5.");
            selectTab(robot, "Priors");
    		clickOnNodesWithID(robot, "Human-Chimp.prior.isMonophyletic");
    		clickOnNodesWithID(robot, "Human-Chimp.prior.distr");
    		robot.clickOn("Normal");
    		clickOnNodesWithID(robot, "Human-Chimp.prior.editButton");

    		scrollToBottom(robot);
    		robot.doubleClickOn("#mean").write("6");//.selectAll().setText("6");
    		robot.doubleClickOn("#sigma").write("0.5");//.selectAll().setText("0.5");
            // beautiFrame.scrollBar().scrollToMaximum();
            screenshot(PREFIX + "TaxonSets.png");
            printBeautiState();
            assertStateEquals("Tree.t:tree", "kappa.s:noncoding", "gammaShape.s:noncoding", "mutationRate.s:noncoding", "kappa.s:1stpos", "gammaShape.s:1stpos", "mutationRate.s:1stpos", "kappa.s:2ndpos", "gammaShape.s:2ndpos", "mutationRate.s:2ndpos", "kappa.s:3rdpos", "gammaShape.s:3rdpos", "mutationRate.s:3rdpos", "birthRateY.t:tree", "clockRate.c:clock");
            assertOperatorsEqual("FixMeanMutationRatesOperator", "gammaShapeScaler.s:noncoding", "KappaScaler.s:noncoding", "gammaShapeScaler.s:1stpos", "KappaScaler.s:1stpos", "gammaShapeScaler.s:2ndpos", "KappaScaler.s:2ndpos", "gammaShapeScaler.s:3rdpos", "KappaScaler.s:3rdpos", "CalibratedYuleModelBICEPSEpochTop.t:tree", "CalibratedYuleModelBICEPSEpochAll.t:tree", "CalibratedYuleModelBICEPSTreeFlex.t:tree", "CalibratedYuleModelTreeRootScaler.t:tree", "CalibratedYuleModelUniformOperator.t:tree", "CalibratedYuleModelSubtreeSlide.t:tree", "CalibratedYuleModelNarrow.t:tree", "CalibratedYuleModelWide.t:tree", "CalibratedYuleModelWilsonBalding.t:tree", "CalibratedYuleBirthRateScaler.t:tree", "StrictClockRateScaler.c:clock", "strictClockUpDownOperator.c:clock");
            assertPriorsEqual("CalibratedYuleModel.t:tree", "CalibratedYuleBirthRatePrior.t:tree", "GammaShapePrior.s:1stpos", "GammaShapePrior.s:2ndpos", "GammaShapePrior.s:3rdpos", "GammaShapePrior.s:noncoding", "KappaPrior.s:1stpos", "KappaPrior.s:2ndpos", "KappaPrior.s:3rdpos", "KappaPrior.s:noncoding", "Human-Chimp.prior", "ClockPrior.c:clock");
            assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.1stpos", "treeLikelihood.2ndpos", "treeLikelihood.3rdpos", "treeLikelihood.noncoding", "TreeHeight.t:tree", "kappa.s:noncoding", "gammaShape.s:noncoding", "mutationRate.s:noncoding", "kappa.s:1stpos", "gammaShape.s:1stpos", "mutationRate.s:1stpos", "kappa.s:2ndpos", "gammaShape.s:2ndpos", "mutationRate.s:2ndpos", "kappa.s:3rdpos", "gammaShape.s:3rdpos", "mutationRate.s:3rdpos", "CalibratedYuleModel.t:tree", "birthRateY.t:tree", "Human-Chimp.prior", "clockRate.c:clock");


            //7c. set gamma priors on birth rate and clock rate
            warning("7c. set gamma priors on birth rate and clock rate");
            scrollToTop(robot);
    		clickOnNodesWithID(robot, "birthRateY.t:tree.distr");
            robot.clickOn("Gamma");
            
            clickOnNodesWithID(robot, "CalibratedYuleBirthRatePrior.t:tree.editButton");
            robot.doubleClickOn("#alpha").write("0.001");
            robot.doubleClickOn("#beta").write("1000");
            clickOnNodesWithID(robot, "CalibratedYuleBirthRatePrior.t:tree.editButton");

    		//clickOnNodesWithID(robot, "clockRate.c:clock.distr");
    		selectFromCombobox(robot, "clockRate.c:clock.distr", "Gamma");
            //robot.clickOn("Gamma");
            clickOnNodesWithID(robot, "ClockPrior.c:clock.editButton");
            robot.doubleClickOn("#alpha").write("0.001");
            robot.doubleClickOn("#beta").write("1000");
            printBeautiState();
            assertStateEquals("Tree.t:tree", "kappa.s:noncoding", "gammaShape.s:noncoding", "mutationRate.s:noncoding", "kappa.s:1stpos", "gammaShape.s:1stpos", "mutationRate.s:1stpos", "kappa.s:2ndpos", "gammaShape.s:2ndpos", "mutationRate.s:2ndpos", "kappa.s:3rdpos", "gammaShape.s:3rdpos", "mutationRate.s:3rdpos", "birthRateY.t:tree", "clockRate.c:clock");
            assertOperatorsEqual("FixMeanMutationRatesOperator", "gammaShapeScaler.s:noncoding", "KappaScaler.s:noncoding", "gammaShapeScaler.s:1stpos", "KappaScaler.s:1stpos", "gammaShapeScaler.s:2ndpos", "KappaScaler.s:2ndpos", "gammaShapeScaler.s:3rdpos", "KappaScaler.s:3rdpos", "CalibratedYuleModelBICEPSEpochTop.t:tree", "CalibratedYuleModelBICEPSEpochAll.t:tree", "CalibratedYuleModelBICEPSTreeFlex.t:tree", "CalibratedYuleModelTreeRootScaler.t:tree", "CalibratedYuleModelUniformOperator.t:tree", "CalibratedYuleModelSubtreeSlide.t:tree", "CalibratedYuleModelNarrow.t:tree", "CalibratedYuleModelWide.t:tree", "CalibratedYuleModelWilsonBalding.t:tree", "CalibratedYuleBirthRateScaler.t:tree", "StrictClockRateScaler.c:clock", "strictClockUpDownOperator.c:clock");
            assertPriorsEqual("CalibratedYuleModel.t:tree", "CalibratedYuleBirthRatePrior.t:tree", "ClockPrior.c:clock", "GammaShapePrior.s:1stpos", "GammaShapePrior.s:2ndpos", "GammaShapePrior.s:3rdpos", "GammaShapePrior.s:noncoding", "KappaPrior.s:1stpos", "KappaPrior.s:2ndpos", "KappaPrior.s:3rdpos", "KappaPrior.s:noncoding", "Human-Chimp.prior");
            assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.1stpos", "treeLikelihood.2ndpos", "treeLikelihood.3rdpos", "treeLikelihood.noncoding", "TreeHeight.t:tree", "kappa.s:noncoding", "gammaShape.s:noncoding", "mutationRate.s:noncoding", "kappa.s:1stpos", "gammaShape.s:1stpos", "mutationRate.s:1stpos", "kappa.s:2ndpos", "gammaShape.s:2ndpos", "mutationRate.s:2ndpos", "kappa.s:3rdpos", "gammaShape.s:3rdpos", "mutationRate.s:3rdpos", "CalibratedYuleModel.t:tree", "birthRateY.t:tree", "Human-Chimp.prior", "clockRate.c:clock");


            //8. Run MCMC and look at results in Tracer, TreeAnnotator->FigTree
            warning("8. Run MCMC and look at results in Tracer, TreeAnnotator->FigTree");
            File fout = new File(org.assertj.core.util.Files.temporaryFolder() + "/primates.xml");
            if (fout.exists()) {
                fout.delete();
            }
            makeSureXMLParses();

            long t1 = System.currentTimeMillis();
            System.err.println("total time: " + (t1 - t0) / 1000 + " seconds");
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }







	@Test
    public void DivergenceDatingPrior(FxRobot robot) throws Exception {
        long t0 = System.currentTimeMillis();

        // 0. Load primate-mtDNA.nex
        warning("// 0. Load primate-mtDNA.nex");
        importAlignment(NEXUS_DIR, new File("primate-mtDNA.nex"));

        selectPartitions(robot, 0, 1, 2, 3);
        clickOnButtonWithText(robot, "-");

        //7a. Create a Normal calibration prior
        warning("7a. Create a Normal calibration prior");
        selectTab(robot, "Priors");
        
    	selectFromCombobox(robot, "birthRate.t:3rdpos.distr", "Gamma");
        
        
        
//        Component c = beautiFrame.robot().finder().findByName("addItem");
//        JButtonFixture addButton = new JButtonFixture(robot(), (JButton) c);
//        addButton.click();
        clickOnButtonWithText(robot, "+ Add Prior");
        
        //JOptionPaneFixture dialog = new JOptionPaneFixture(robot());
        //dialog.textBox("idEntry").setText("Human-Chimp");
        robot.clickOn("#idEntry").write("Human-Chimp");
        robot.clickOn("#listOfTaxonCandidates").clickOn("Homo_sapiens");
        clickOnButtonWithText(robot, ">>");
//        robot.clickOn("#listOfTaxonCandidates").clickOn("Pan");
//        clickOnButton(robot, ">>");
//        dialog.list("listOfTaxonCandidates").selectItems("Homo_sapiens", "Pan");
//        dialog.button(">>").click();
//        dialog.okButton().click();
        robot.clickOn("OK");
        
        printBeautiState();
        
        
        //7b. and monophyletic constraint on Human-Chimp split of 6 +/- 0.5.
        warning("7b. and monophyletic constraint on Human-Chimp split of 6 +/- 0.5.");
        selectTab(robot, "Priors");
        clickOnNodesWithID(robot, "Human-Chimp.prior.isMonophyletic");
        printBeautiState();
        clickOnNodesWithID(robot, "Human-Chimp.prior.distr");
        robot.clickOn("Normal");
        printBeautiState();
		//robot.interact(()->robot.lookup("#Human-Chimp.prior.distr").queryAs(ComboBox.class).getSelectionModel().select("Normal"));

        
        clickOnNodesWithID(robot, "Human-Chimp.prior.editButton");

        robot.doubleClickOn("#mean").write("6");//.selectAll().setText("6");
        robot.doubleClickOn("#sigma").write("0.5");//.selectAll().setText("0.5");
        printBeautiState();

        //8. Run MCMC and look at results in Tracer, TreeAnnotator->FigTree
        warning("8. Run MCMC and look at results in Tracer, TreeAnnotator->FigTree");
        File fout = new File(org.assertj.core.util.Files.temporaryFolder() + "/primates.xml");
        if (fout.exists()) {
            fout.delete();
        }

		// 9. Set up MCMC parameters
		warning("8. Set up MCMC parameters");
		selectTab(robot, "MCMC");
		clickOnNodesWithID(robot, "#chainLength");
		robot.doubleClickOn("#chainLength").write("2000000");


        fout = new File(org.assertj.core.util.Files.temporaryFolder() + "/divtutorial.xml");
        if (fout.exists()) {
            fout.delete();
        }
		saveFile(""+org.assertj.core.util.Files.temporaryFolder(), "divtutorial.xml");

		makeSureXMLParses();

        long t1 = System.currentTimeMillis();
        System.err.println("total time: " + (t1 - t0) / 1000 + " seconds");

    }
}


