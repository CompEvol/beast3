package test.beastfx.app.beauti;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import beast.base.parser.XMLParser;
import beast.base.spec.inference.distribution.Dirichlet;
import beast.base.spec.inference.parameter.RealVectorParam;
import beastfx.app.beauti.BeautiTabPane;
import javafx.scene.Node;
import javafx.scene.control.Labeled;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

/**
 * A prior that is not valid for its parameter must be reported in the Priors panel
 * (issue #240). Before, the first sign of a problem was BEAST refusing to read the XML.
 */
@ExtendWith(ApplicationExtension.class)
public class BeautiPriorValidationTest extends BeautiBase {

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

	/**
	 * Bounded Real starts with the range [0,1], kappa with the value 2. Picking Bounded
	 * Real for kappa gives a model BEAST cannot read, which has to be reported until the
	 * user widens the range.
	 */
	@Test
	void initialValueOutsideRangeOfNewPriorIsReported(FxRobot robot) throws Exception {
		importAlignment(NEXUS_DIR, new File("anolis.nex"));
		robot.clickOn("#SiteModel");
		robot.clickOn("#substModelComboBox").clickOn("HKY");
		selectTab(robot, "Priors");
		assertFalse(isReported(robot, "KappaPrior.s:anolis"), "the default prior on kappa is valid");
		assertXMLParses();

		selectFromCombobox(robot, "kappa.s:anolis.distr", "Bounded Real");
		WaitForAsyncUtils.waitForFxEvents();
		screenshot("/tmp/priorValidation1.png");
		assertThrows(Exception.class, this::parseXML, "kappa = 2 under Bounded Real [0,1] is not a valid model");
		assertTrue(isReported(robot, "KappaPrior.s:anolis"), "the problem with the prior on kappa should be reported");
		String message = message(robot, "KappaPrior.s:anolis");
		System.err.println("Message shown: " + message);
		assertTrue(message.contains("kappa.s:anolis"), "the message should name the parameter: " + message);
		assertTrue(message.contains("[0.0,1.0]"), "the message should give the allowed range: " + message);
		// issue #253: say why the value is not valid, and give the value once
		assertTrue(message.contains("It is outside the allowed range [0.0,1.0]"),
				"the message should say the value is outside the range: " + message);
		assertEquals(message.indexOf("2.0"), message.lastIndexOf("2.0"), "the value should be given once: " + message);
		assertFalse(isReported(robot, "FrequenciesPrior.s:anolis"), "the other priors are valid");
		assertFalse(isReported(robot, "YuleBirthRatePrior.t:anolis"), "the other priors are valid");

		// the user fixes it by widening the range
		clickOnNodesWithID(robot, "KappaPrior.s:anolis.editButton");
		WaitForAsyncUtils.waitForFxEvents();
		type(robot, visibleTextField(robot, "upper", "1.0"), "10.0");
		screenshot("/tmp/priorValidation2.png");
		assertFalse(isReported(robot, "KappaPrior.s:anolis"), "kappa = 2 is in [0,10], so nothing should be reported");
		assertXMLParses();
	}

	/**
	 * A Dirichlet whose alpha has a different dimension from its parameter is not
	 * valid. BEAUti gives no way to get there today, so the model is changed directly.
	 */
	@Test
	void dirichletDimensionMismatchIsReported(FxRobot robot) throws Exception {
		importAlignment(NEXUS_DIR, new File("anolis.nex"));
		robot.clickOn("#SiteModel");
		robot.clickOn("#substModelComboBox").clickOn("HKY");
		selectTab(robot, "Priors");
		assertFalse(isReported(robot, "FrequenciesPrior.s:anolis"), "the default prior on the frequencies is valid");

		Dirichlet prior = (Dirichlet) doc.pluginmap.get("FrequenciesPrior.s:anolis");
		robot.interact(() -> ((RealVectorParam<?>) prior.alphaInput.get()).setDimension(2));
		// leaving the panel and coming back rebuilds it
		selectTab(robot, "MCMC");
		selectTab(robot, "Priors");
		WaitForAsyncUtils.waitForFxEvents();
		screenshot("/tmp/priorValidation3.png");
		assertTrue(isReported(robot, "FrequenciesPrior.s:anolis"), "the dimension mismatch should be reported");
		String message = message(robot, "FrequenciesPrior.s:anolis");
		System.err.println("Message shown: " + message);
		assertTrue(message.contains("Dimensions of alpha and param"), message);

		// the error goes away after an edit that makes the prior valid again
		robot.interact(() -> ((RealVectorParam<?>) prior.alphaInput.get()).setDimension(4));
		clickOnNodesWithID(robot, "FrequenciesPrior.s:anolis.editButton");
		WaitForAsyncUtils.waitForFxEvents();
		TextField alpha = (TextField) robot.lookup(n -> n instanceof TextField && n.isVisible() && "alpha".equals(n.getId())).query();
		type(robot, alpha, "2.0 2.0 2.0 2.0");
		assertFalse(isReported(robot, "FrequenciesPrior.s:anolis"), "alpha has four values again, so nothing should be reported");
		assertXMLParses();
	}

	private Labeled validateLabel(FxRobot robot, String priorID) {
		Node label = robot.lookup(n -> (priorID + ".validateLabel").equals(n.getId())).query();
		assertNotNull(label, "no validation label for " + priorID);
		return (Labeled) label;
	}

	private boolean isReported(FxRobot robot, String priorID) {
		return validateLabel(robot, priorID).isVisible();
	}

	private String message(FxRobot robot, String priorID) {
		return validateLabel(robot, priorID).getTooltip().getText();
	}

	private TextField visibleTextField(FxRobot robot, String id, String text) {
		for (Node n : robot.lookup(n -> n instanceof TextField && n.isVisible() && id.equals(n.getId())).queryAll()) {
			if (((TextField) n).getText().equals(text)) {
				return (TextField) n;
			}
		}
		throw new AssertionError("no visible text field " + id + " with text " + text);
	}

	private void type(FxRobot robot, TextField field, String text) {
		robot.interact(() -> { field.requestFocus(); field.selectAll(); });
		robot.write(text);
		robot.type(KeyCode.ENTER);
		WaitForAsyncUtils.waitForFxEvents();
		assertEquals(text, field.getText());
	}

	private void parseXML() throws Exception {
		File file = File.createTempFile("priorValidation", ".xml");
		file.deleteOnExit();
		doc.save(file);
		new XMLParser().parseFile(file);
	}

	private void assertXMLParses() {
		try {
			parseXML();
		} catch (Exception e) {
			throw new AssertionError("the XML should parse: " + e.getMessage(), e);
		}
	}
}
