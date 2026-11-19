package test.beastfx.app.beauti;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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
import beast.base.inference.Logger;
import beast.base.inference.MCMC;
import beast.base.parser.XMLParser;
import beast.base.spec.inference.distribution.CountPrior;
import beast.base.spec.inference.distribution.Poisson;
import beast.base.spec.inference.operator.BitFlipOperator;
import beastfx.app.beauti.BeautiTabPane;
import javafx.stage.Stage;

/**
 * The random local clock in BEAUti puts a CountPrior on its indicators (Poisson number of
 * rate changes, every placement equally probable) and flips them with a symmetric
 * BitFlipOperator, so the whole prior is in the model (#231).
 */
@ExtendWith(ApplicationExtension.class)
public class BeautiRandomLocalClockTest extends BeautiBase {

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
	void randomLocalClockHasCountPriorOnIndicators(FxRobot robot) throws Exception {
		importAlignment(NEXUS_DIR, new File("anolis.nex"));
		selectTab(robot, "Clock Model");
		// the clock model choice: the panel's input is called "index" (BeautiPanelConfig)
		robot.clickOn("#indexComboBox").clickOn("Random Local Clock");
		// the priors panel shows the new prior
		selectTab(robot, "Priors");
		printBeautiState();

		CountPrior prior = assertInstanceOf(CountPrior.class, doc.pluginmap.get("RRateChangesPrior.c:anolis"));
		assertEquals("Indicators.c:anolis", ((BEASTInterface) prior.paramInput.get()).getID());
		Poisson count = assertInstanceOf(Poisson.class, prior.getCountDistribution());
		assertEquals(Math.log(2), count.lambdaInput.get().get(), 1e-12);
		BitFlipOperator flip = assertInstanceOf(BitFlipOperator.class, doc.pluginmap.get("IndicatorsBitFlip.c:anolis"));
		assertFalse(flip.uniformInput.get(), "the bit flip must be symmetric with CountPrior");
		// the number of rate changes is still logged
		Logger tracelog = (Logger) doc.pluginmap.get("tracelog");
		assertTrue(tracelog.loggersInput.get().contains(doc.pluginmap.get("RRateChanges.c:anolis")),
				"RRateChanges.c:anolis not in the tracelog: " + tracelog.loggersInput.get());

		// the XML that BEAUti writes can be read
		File file = tmp.resolve("rlc-beauti.xml").toFile();
		doc.save(file);
		BEASTInterface run = new XMLParser().parseFile(file);
		assertTrue(run instanceof MCMC);
	}
}
