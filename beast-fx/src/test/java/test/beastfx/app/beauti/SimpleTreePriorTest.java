package test.beastfx.app.beauti;

import beastfx.app.beauti.BeautiTabPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.io.File;

@ExtendWith(ApplicationExtension.class)
public class SimpleTreePriorTest extends BeautiBase {

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
	
	/** check the standard tree priors are there and result in correct behaviour **/
	@Test
	public void simpleTreePriorTest(FxRobot robot) throws Exception {
		warning("Load anolis.nex");
		importAlignment(NEXUS_DIR, new File("anolis.nex"));

		selectTab(robot, "Priors");
		
		warning("Change to Coalescent - constant population");
		
		robot.clickOn("#TreeDistribution").clickOn("Coalescent Constant Population");
		printBeautiState();
		assertStateEquals("Tree.t:anolis", "popSize.t:anolis");
		assertOperatorsEqual("CoalescentConstantTreeRootScaler.t:anolis", "CoalescentConstantUniformOperator.t:anolis", "CoalescentConstantSubtreeSlide.t:anolis", "CoalescentConstantNarrow.t:anolis", "CoalescentConstantWide.t:anolis", "CoalescentConstantWilsonBalding.t:anolis", "CoalescentConstantTreeScaler.t:anolis", "PopSizeScaler.t:anolis");
		assertPriorsEqual("CoalescentConstant.t:anolis", "PopSizePrior.t:anolis");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.anolis", "TreeHeight.t:anolis", "popSize.t:anolis", "CoalescentConstant.t:anolis");

		warning("Change to Coalescent - exponential population");
		robot.clickOn("#TreeDistribution").clickOn("Coalescent Exponential Population");
		printBeautiState();
		assertStateEquals("Tree.t:anolis", "ePopSize.t:anolis", "growthRate.t:anolis");
		assertOperatorsEqual("CoalescentExponentialTreeRootScaler.t:anolis", "CoalescentExponentialUniformOperator.t:anolis", "CoalescentExponentialSubtreeSlide.t:anolis", "CoalescentExponentialNarrow.t:anolis", "CoalescentExponentialWide.t:anolis", "CoalescentExponentialWilsonBalding.t:anolis", "CoalescentExponentialTreeScaler.t:anolis", "ePopSizeScaler.t:anolis", "GrowthRateRandomWalk.t:anolis");
		assertPriorsEqual("CoalescentExponential.t:anolis", "ePopSizePrior.t:anolis", "GrowthRatePrior.t:anolis");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.anolis", "TreeHeight.t:anolis", "CoalescentExponential.t:anolis", "ePopSize.t:anolis", "growthRate.t:anolis");
		
		warning("Change to Coalescent - BPS");
		robot.clickOn("#TreeDistribution").clickOn("Coalescent Bayesian Skyline");
		printBeautiState();
		assertStateEquals("Tree.t:anolis", "bPopSizes.t:anolis", "bGroupSizes.t:anolis");
		assertOperatorsEqual("BayesianSkylineTreeRootScaler.t:anolis", "BayesianSkylineUniformOperator.t:anolis", "BayesianSkylineSubtreeSlide.t:anolis", "BayesianSkylineNarrow.t:anolis", "BayesianSkylineWide.t:anolis", "BayesianSkylineWilsonBalding.t:anolis", "BayesianSkylineTreeScaler.t:anolis", "popSizesScaler.t:anolis", "groupSizesDelta.t:anolis");
		assertPriorsEqual("BayesianSkyline.t:anolis", "MarkovChainedPopSizes.t:anolis");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.anolis", "TreeHeight.t:anolis", "BayesianSkyline.t:anolis", "bPopSizes.t:anolis", "bGroupSizes.t:anolis");

		warning("Change to Coalescent - Extended BSP");
		robot.clickOn("#TreeDistribution").clickOn("Coalescent Extended Bayesian Skyline");
		printBeautiState();
		assertStateEquals("Tree.t:anolis", "indicators.alltrees", "populationMean.alltrees", "popSizes.alltrees");
		assertOperatorsEqual("ExtendedBayesianSkylineTreeScaler.t:anolis", "ExtendedBayesianSkylineTreeRootScaler.t:anolis", "ExtendedBayesianSkylineUniformOperator.t:anolis", "ExtendedBayesianSkylineSubtreeSlide.t:anolis", "ExtendedBayesianSkylineNarrow.t:anolis", "ExtendedBayesianSkylineWide.t:anolis", "ExtendedBayesianSkylineWilsonBalding.t:anolis", "bitflip.alltrees", "indicatorSampler.alltrees", "indicatorScaler.alltrees", "EBSPupDownOperator.alltrees");
		assertPriorsEqual("ExtendedBayesianSkyline.t:anolis", "populationMeanPrior.alltrees", "indicatorsPrior.alltrees", "popSizePrior.alltrees");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.anolis", "TreeHeight.t:anolis", "ExtendedBayesianSkyline.t:anolis", "indicators.alltrees", "populationMean.alltrees", "popSizes.alltrees", "sumIndicators");

		warning("Change to Yule");
		robot.clickOn("#TreeDistribution").clickOn("Yule Model");
		printBeautiState();
		assertStateEquals("Tree.t:anolis", "birthRate.t:anolis");
		assertOperatorsEqual("YuleModelTreeRootScaler.t:anolis", "YuleModelUniformOperator.t:anolis", "YuleModelSubtreeSlide.t:anolis", "YuleModelNarrow.t:anolis", "YuleModelWide.t:anolis", "YuleModelWilsonBalding.t:anolis", "YuleModelTreeScaler.t:anolis", "YuleBirthRateScaler.t:anolis");
		assertPriorsEqual("YuleModel.t:anolis", "YuleBirthRatePrior.t:anolis");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.anolis", "TreeHeight.t:anolis", "YuleModel.t:anolis", "birthRate.t:anolis");
		
		warning("Change to Birth-Death");
		robot.clickOn("#TreeDistribution").clickOn("Birth Death Model");
		printBeautiState();
		assertStateEquals("Tree.t:anolis", "BDBirthRate.t:anolis", "BDDeathRate.t:anolis");
		assertOperatorsEqual("BirthDeathTreeRootScaler.t:anolis", "BirthDeathUniformOperator.t:anolis", "BirthDeathSubtreeSlide.t:anolis", "BirthDeathNarrow.t:anolis", "BirthDeathWide.t:anolis", "BirthDeathWilsonBalding.t:anolis", "BirthDeathTreeScaler.t:anolis", "BirthRateScaler.t:anolis", "DeathRateScaler.t:anolis");
		assertPriorsEqual("BirthDeath.t:anolis", "BirthRatePrior.t:anolis", "DeathRatePrior.t:anolis");
		assertTraceLogEqual("posterior", "likelihood", "prior", "treeLikelihood.anolis", "TreeHeight.t:anolis", "BirthDeath.t:anolis", "BDBirthRate.t:anolis", "BDDeathRate.t:anolis");

		makeSureXMLParses();
	}

}
