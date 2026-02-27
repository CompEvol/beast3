package test.beastfx.app.beauti;


import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import beastfx.app.beauti.BeautiTabPane;
import javafx.stage.Stage;

@ExtendWith(ApplicationExtension.class)
public class SimpleClockModelTest extends BeautiBase {

	@Start
    public void start(Stage stage) {
    	try {
    		BeautiTabPane tabPane = BeautiTabPane.main2(new String[] {}, stage);
    		this.doc = tabPane.doc;
            stage.show();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

	/** check the standard clock models are there and result in correct behaviour **/
	@Test
	public void simpleClockModelTest(FxRobot robot) throws Exception {
		warning("Load anolis.nex");
		importAlignment(NEXUS_DIR, new File("anolis.nex"));

		//JTabbedPaneFixture f = beautiFrame.tabbedPane();
		selectTab(robot, "Clock Model");

		warning("Change to Relaxed Clock - exponential");
		robot.clickOn(".combo-box").clickOn("Relaxed Clock Exponential");
		printBeautiState();
		assertStateEquals("Tree.t:anolis", "birthRate.t:anolis", "expRateCategories.c:anolis");
		assertOperatorsEqual("YuleBirthRateScaler.t:anolis", "YuleModelBICEPSEpochTop.t:anolis", "YuleModelBICEPSEpochAll.t:anolis", "YuleModelBICEPSTreeFlex.t:anolis", "YuleModelTreeRootScaler.t:anolis", "YuleModelUniformOperator.t:anolis", "YuleModelSubtreeSlide.t:anolis", "YuleModelNarrow.t:anolis", "YuleModelWide.t:anolis", "YuleModelWilsonBalding.t:anolis", "ExpCategoriesRandomWalk.c:anolis", "ExpCategoriesSwapOperator.c:anolis", "ExpCategoriesUniform.c:anolis");
		assertPriorsEqual("YuleModel.t:anolis", "YuleBirthRatePrior.t:anolis");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.anolis", "TreeHeight.t:anolis", "YuleModel.t:anolis", "birthRate.t:anolis", "rateStat.c:anolis");
		
		warning("Change to Relaxed Clock - log normal");
		robot.clickOn(".combo-box").clickOn("Relaxed Clock Log Normal");
		printBeautiState();
		assertStateEquals("Tree.t:anolis", "birthRate.t:anolis", "ucldStdev.c:anolis", "rateCategories.c:anolis");
		assertOperatorsEqual("YuleBirthRateScaler.t:anolis", "YuleModelBICEPSEpochTop.t:anolis", "YuleModelBICEPSEpochAll.t:anolis", "YuleModelBICEPSTreeFlex.t:anolis", "YuleModelTreeRootScaler.t:anolis", "YuleModelUniformOperator.t:anolis", "YuleModelSubtreeSlide.t:anolis", "YuleModelNarrow.t:anolis", "YuleModelWide.t:anolis", "YuleModelWilsonBalding.t:anolis", "ucldStdevScaler.c:anolis", "CategoriesRandomWalk.c:anolis", "CategoriesSwapOperator.c:anolis", "CategoriesUniform.c:anolis");
		assertPriorsEqual("YuleModel.t:anolis", "YuleBirthRatePrior.t:anolis", "ucldStdevPrior.c:anolis");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.anolis", "TreeHeight.t:anolis", "YuleModel.t:anolis", "birthRate.t:anolis", "ucldStdev.c:anolis", "rate.c:anolis");

		warning("Change to Random Local Clock");
		robot.clickOn(".combo-box").clickOn("Random Local Clock");
		printBeautiState();
		assertStateEquals("Tree.t:anolis", "birthRate.t:anolis", "Indicators.c:anolis", "clockrates.c:anolis");
		assertOperatorsEqual("YuleBirthRateScaler.t:anolis", "YuleModelBICEPSEpochTop.t:anolis", "YuleModelBICEPSEpochAll.t:anolis", "YuleModelBICEPSTreeFlex.t:anolis", "YuleModelTreeRootScaler.t:anolis", "YuleModelUniformOperator.t:anolis", "YuleModelSubtreeSlide.t:anolis", "YuleModelNarrow.t:anolis", "YuleModelWide.t:anolis", "YuleModelWilsonBalding.t:anolis", "IndicatorsBitFlip.c:anolis", "ClockRateScaler.c:anolis");
		assertPriorsEqual("YuleModel.t:anolis", "YuleBirthRatePrior.t:anolis", "RRatesPrior.c:sanolis", "RRateChangesPrior.c:anolis");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.anolis", "TreeHeight.t:anolis", "YuleModel.t:anolis", "birthRate.t:anolis", "Indicators.c:anolis", "clockrates.c:anolis", "RRateChanges.c:anolis");

		warning("Change to Strickt Clock");
		robot.clickOn(".combo-box").clickOn("Strict Clock");
		printBeautiState();
		assertStateEquals("Tree.t:anolis", "birthRate.t:anolis");
		assertOperatorsEqual("YuleBirthRateScaler.t:anolis", "YuleModelBICEPSEpochTop.t:anolis", "YuleModelBICEPSEpochAll.t:anolis", "YuleModelBICEPSTreeFlex.t:anolis", "YuleModelTreeRootScaler.t:anolis", "YuleModelUniformOperator.t:anolis", "YuleModelSubtreeSlide.t:anolis", "YuleModelNarrow.t:anolis", "YuleModelWide.t:anolis", "YuleModelWilsonBalding.t:anolis");
		assertPriorsEqual("YuleModel.t:anolis", "YuleBirthRatePrior.t:anolis");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.anolis", "TreeHeight.t:anolis", "YuleModel.t:anolis", "birthRate.t:anolis");

		makeSureXMLParses();
	}

	/** switch to coalescent tree prior, then 
	 * check the standard clock models are there and result in correct behaviour **/
	@Test
	public void simpleClockModelTest2(FxRobot robot) throws Exception {
		warning("Load anolis.nex");
		importAlignment(NEXUS_DIR, new File("anolis.nex"));

		//JTabbedPaneFixture f = beautiFrame.tabbedPane();
		selectTab(robot, "Priors");
		
		warning("Change to Coalescent - constant population");
		
		robot.clickOn(".combo-box").clickOn("Coalescent Constant Population");
		printBeautiState();
		assertStateEquals("Tree.t:anolis", "popSize.t:anolis");
		assertOperatorsEqual("CoalescentConstantBICEPSEpochTop.t:anolis", "CoalescentConstantBICEPSEpochAll.t:anolis", "CoalescentConstantBICEPSTreeFlex.t:anolis", "CoalescentConstantTreeRootScaler.t:anolis", "CoalescentConstantUniformOperator.t:anolis", "CoalescentConstantSubtreeSlide.t:anolis", "CoalescentConstantNarrow.t:anolis", "CoalescentConstantWide.t:anolis", "CoalescentConstantWilsonBalding.t:anolis", "PopSizeScaler.t:anolis");
		assertPriorsEqual("CoalescentConstant.t:anolis", "PopSizePrior.t:anolis");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.anolis", "TreeHeight.t:anolis", "popSize.t:anolis", "CoalescentConstant.t:anolis");
		
		selectTab(robot, "Clock Model");

		warning("Change to Relaxed Clock - exponential");
		selectFromCombobox(robot, "indexComboBox", "Relaxed Clock Exponential");
		printBeautiState();
		assertStateEquals("Tree.t:anolis", "popSize.t:anolis", "expRateCategories.c:anolis");
		assertOperatorsEqual("CoalescentConstantBICEPSEpochTop.t:anolis", "CoalescentConstantBICEPSEpochAll.t:anolis", "CoalescentConstantBICEPSTreeFlex.t:anolis", "CoalescentConstantTreeRootScaler.t:anolis", "CoalescentConstantUniformOperator.t:anolis", "CoalescentConstantSubtreeSlide.t:anolis", "CoalescentConstantNarrow.t:anolis", "CoalescentConstantWide.t:anolis", "CoalescentConstantWilsonBalding.t:anolis", "PopSizeScaler.t:anolis", "ExpCategoriesRandomWalk.c:anolis", "ExpCategoriesSwapOperator.c:anolis", "ExpCategoriesUniform.c:anolis");
		assertPriorsEqual("CoalescentConstant.t:anolis", "PopSizePrior.t:anolis");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.anolis", "TreeHeight.t:anolis", "popSize.t:anolis", "CoalescentConstant.t:anolis", "rateStat.c:anolis");
		
		warning("Change to Relaxed Clock - log normal");
		selectFromCombobox(robot, "indexComboBox", "Relaxed Clock Log Normal");
		printBeautiState();
		assertStateEquals("Tree.t:anolis", "popSize.t:anolis", "ucldStdev.c:anolis", "rateCategories.c:anolis");
		assertOperatorsEqual("CoalescentConstantBICEPSEpochTop.t:anolis", "CoalescentConstantBICEPSEpochAll.t:anolis", "CoalescentConstantBICEPSTreeFlex.t:anolis", "CoalescentConstantTreeRootScaler.t:anolis", "CoalescentConstantUniformOperator.t:anolis", "CoalescentConstantSubtreeSlide.t:anolis", "CoalescentConstantNarrow.t:anolis", "CoalescentConstantWide.t:anolis", "CoalescentConstantWilsonBalding.t:anolis", "PopSizeScaler.t:anolis", "ucldStdevScaler.c:anolis", "CategoriesRandomWalk.c:anolis", "CategoriesSwapOperator.c:anolis", "CategoriesUniform.c:anolis");
		assertPriorsEqual("CoalescentConstant.t:anolis", "PopSizePrior.t:anolis", "ucldStdevPrior.c:anolis");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.anolis", "TreeHeight.t:anolis", "popSize.t:anolis", "CoalescentConstant.t:anolis", "ucldStdev.c:anolis", "rate.c:anolis");

		warning("Change to Random Local Clock");
		selectFromCombobox(robot, "indexComboBox", "Random Local Clock");
		printBeautiState();
		assertStateEquals("Tree.t:anolis", "popSize.t:anolis", "Indicators.c:anolis", "clockrates.c:anolis");
		assertOperatorsEqual("CoalescentConstantBICEPSEpochTop.t:anolis", "CoalescentConstantBICEPSEpochAll.t:anolis", "CoalescentConstantBICEPSTreeFlex.t:anolis", "CoalescentConstantTreeRootScaler.t:anolis", "CoalescentConstantUniformOperator.t:anolis", "CoalescentConstantSubtreeSlide.t:anolis", "CoalescentConstantNarrow.t:anolis", "CoalescentConstantWide.t:anolis", "CoalescentConstantWilsonBalding.t:anolis", "PopSizeScaler.t:anolis", "IndicatorsBitFlip.c:anolis", "ClockRateScaler.c:anolis");
		assertPriorsEqual("CoalescentConstant.t:anolis", "PopSizePrior.t:anolis", "RRatesPrior.c:sanolis", "RRateChangesPrior.c:anolis");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.anolis", "TreeHeight.t:anolis", "popSize.t:anolis", "CoalescentConstant.t:anolis", "Indicators.c:anolis", "clockrates.c:anolis", "RRateChanges.c:anolis");

		warning("Change to Strickt Clock");
		selectFromCombobox(robot, "indexComboBox", "Strict Clock");
		printBeautiState();
		assertStateEquals("Tree.t:anolis", "popSize.t:anolis");
		assertOperatorsEqual("CoalescentConstantBICEPSEpochTop.t:anolis", "CoalescentConstantBICEPSEpochAll.t:anolis", "CoalescentConstantBICEPSTreeFlex.t:anolis", "CoalescentConstantTreeRootScaler.t:anolis", "CoalescentConstantUniformOperator.t:anolis", "CoalescentConstantSubtreeSlide.t:anolis", "CoalescentConstantNarrow.t:anolis", "CoalescentConstantWide.t:anolis", "CoalescentConstantWilsonBalding.t:anolis", "PopSizeScaler.t:anolis");
		assertPriorsEqual("CoalescentConstant.t:anolis", "PopSizePrior.t:anolis");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.anolis", "TreeHeight.t:anolis", "popSize.t:anolis", "CoalescentConstant.t:anolis");

		makeSureXMLParses();
	}

}
