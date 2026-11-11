package test.beastfx.app.beauti;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import beast.base.core.BEASTInterface;
import beast.base.inference.MCMC;
import beast.base.parser.XMLParser;
import beast.base.spec.evolution.substitutionmodel.GTR;
import beast.base.spec.inference.distribution.Dirichlet;
import beast.base.spec.type.Simplex;
import beastfx.app.beauti.BeautiTabPane;
import javafx.stage.Stage;

/**
 * GTR in BEAUti has its six rates as one vector of proportions with a Dirichlet prior (issue #146).
 */
@ExtendWith(ApplicationExtension.class)
public class BeautiGTRTest extends BeautiBase {

	@TempDir
	Path tmp;

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

	@Test
	void gtrHasDirichletPriorOnRates(FxRobot robot) throws Exception {
		importAlignment(NEXUS_DIR, new File("anolis.nex"));
		robot.clickOn("#SiteModel");
		robot.clickOn("#substModelComboBox").clickOn("GTR");
		selectTab(robot, "Priors");

		printBeautiState();
		assertStateEquals("Tree.t:anolis", "birthRate.t:anolis", "gtrRates.s:anolis", "freqParameter.s:anolis");
		assertOperatorsEqual("YuleBirthRateScaler.t:anolis", "YuleModelTreeRootScaler.t:anolis",
				"YuleModelUniformOperator.t:anolis", "YuleModelSubtreeSlide.t:anolis", "YuleModelNarrow.t:anolis",
				"YuleModelWide.t:anolis", "YuleModelWilsonBalding.t:anolis", "YuleModelTreeScaler.t:anolis",
				"SubstitutionRatesExchanger.s:anolis", "FrequenciesExchanger.s:anolis");
		assertPriorsEqual("YuleModel.t:anolis", "YuleBirthRatePrior.t:anolis", "FrequenciesPrior.s:anolis",
				"SubstitutionRatesPrior.s:anolis");

		GTR gtr = (GTR) doc.pluginmap.get("gtr.s:anolis");
		Simplex rates = gtr.ratesInput.get();
		assertNotNull(rates, "GTR should get its rates from the rates input");
		assertEquals(6, rates.size());
		Dirichlet prior = assertInstanceOf(Dirichlet.class, doc.pluginmap.get("SubstitutionRatesPrior.s:anolis"));
		assertEquals("[1.0, 1.0, 1.0, 1.0, 1.0, 1.0]", prior.alphaInput.get().getElements().toString());

		// the XML that BEAUti writes can be read and run
		File file = tmp.resolve("gtr-beauti.xml").toFile();
		doc.save(file);
		BEASTInterface run = new XMLParser().parseFile(file);
		assertTrue(run instanceof MCMC);
	}
}
