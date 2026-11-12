package test.beastfx.app.beauti;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import beast.base.parser.XMLParser;
import beast.base.spec.inference.distribution.Dirichlet;
import beast.base.spec.inference.parameter.RealVectorParam;
import beast.base.spec.inference.parameter.SimplexParam;
import beastfx.app.beauti.BeautiTabPane;
import javafx.scene.Node;
import javafx.scene.control.Button;
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
		expand(robot, "KappaPrior.s:anolis", () -> robot.lookup(n -> n instanceof TextField t
				&& "upper".equals(t.getId()) && "1.0".equals(t.getText())).tryQuery().orElse(null));
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
		expand(robot, "FrequenciesPrior.s:anolis", () -> robot.lookup(n -> n instanceof TextField
				&& "alpha".equals(n.getId())).tryQuery().orElse(null));
		TextField alpha = (TextField) robot.lookup(n -> n instanceof TextField && isShowing(n) && "alpha".equals(n.getId())).query();
		type(robot, alpha, "2.0 2.0 2.0 2.0");
		assertFalse(isReported(robot, "FrequenciesPrior.s:anolis"), "alpha has four values again, so nothing should be reported");
		assertXMLParses();
	}

	/**
	 * A simplex whose values are all in [0,1] but do not sum to 1 is not valid. The message
	 * must say the sum is wrong, not that the value is outside the range (issue #281).
	 */
	@Test
	void simplexWithWrongSumIsReported(FxRobot robot) throws Exception {
		importAlignment(NEXUS_DIR, new File("anolis.nex"));
		robot.clickOn("#SiteModel");
		robot.clickOn("#substModelComboBox").clickOn("HKY");
		selectTab(robot, "Priors");
		assertFalse(isReported(robot, "FrequenciesPrior.s:anolis"), "the default prior on the frequencies is valid");

		Dirichlet prior = (Dirichlet) doc.pluginmap.get("FrequenciesPrior.s:anolis");
		SimplexParam freqs = (SimplexParam) prior.paramInput.get();
		robot.interact(() -> {
			for (int i = 0; i < freqs.size(); i++) {
				freqs.set(i, 0.3);
			}
		});
		// leaving the panel and coming back rebuilds it
		selectTab(robot, "MCMC");
		selectTab(robot, "Priors");
		WaitForAsyncUtils.waitForFxEvents();
		screenshot("/tmp/priorValidation4.png");
		assertTrue(isReported(robot, "FrequenciesPrior.s:anolis"), "the wrong sum should be reported");
		String message = message(robot, "FrequenciesPrior.s:anolis");
		System.err.println("Message shown: " + message);
		assertTrue(message.contains(freqs.getID()), "the message should name the parameter: " + message);
		assertTrue(message.contains("The values must sum to 1"), "the message should say the sum is wrong: " + message);
		assertFalse(message.contains("outside the"), "every value is in range: " + message);
		// the "initial = ..." button gets its message from PriorEditorUtil, which failed on a
		// vector parameter and said the value was outside the range
		String buttonMessage = initialValueTooltip(robot, freqs.getID());
		System.err.println("Initial value button: " + buttonMessage);
		assertTrue(buttonMessage.contains("The values must sum to 1, but they sum to 1.2"),
				"the initial value button should say the sum is wrong: " + buttonMessage);
		assertFalse(buttonMessage.contains("outside the"), "every value is in range: " + buttonMessage);

		robot.interact(() -> {
			for (int i = 0; i < freqs.size(); i++) {
				freqs.set(i, 0.25);
			}
		});
		selectTab(robot, "MCMC");
		selectTab(robot, "Priors");
		WaitForAsyncUtils.waitForFxEvents();
		assertFalse(isReported(robot, "FrequenciesPrior.s:anolis"), "the values sum to 1 again, so nothing should be reported");
		assertXMLParses();
	}

	/**
	 * Expand the editor of a prior. Expanded priors are remembered in a static set, so an
	 * earlier test in the same JVM may have left this one expanded already; clicking its
	 * edit button again would collapse it, and the hidden field could not take the focus.
	 */
	private void expand(FxRobot robot, String priorID, Supplier<Node> field) {
		if (!isShowing(field.get())) {
			clickOnNodesWithID(robot, priorID + ".editButton");
			WaitForAsyncUtils.waitForFxEvents();
		}
		assertTrue(isShowing(field.get()), "the editor of " + priorID + " is not expanded");
	}

	private static boolean isShowing(Node node) {
		for (Node n = node; n != null; n = n.getParent()) {
			if (!n.isVisible()) return false;
		}
		return node != null;
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

	/** @return the tooltip of the "initial = ..." button of the parameter with this ID **/
	private String initialValueTooltip(FxRobot robot, String paramID) {
		for (Node n : robot.lookup(n -> n instanceof Button b && isShowing(b) && b.getTooltip() != null
				&& b.getTooltip().getText().contains(paramID)).queryAll()) {
			return ((Button) n).getTooltip().getText();
		}
		throw new AssertionError("no initial value button for " + paramID);
	}

	private TextField visibleTextField(FxRobot robot, String id, String text) {
		for (Node n : robot.lookup(n -> n instanceof TextField && isShowing(n) && id.equals(n.getId())).queryAll()) {
			if (((TextField) n).getText().equals(text)) {
				return (TextField) n;
			}
		}
		throw new AssertionError("no visible text field " + id + " with text " + text);
	}

	private void type(FxRobot robot, TextField field, String text) {
		robot.interact(() -> { field.requestFocus(); field.selectAll(); });
		assertTrue(field.isFocused(), "the field did not get the focus, so the keys would not reach it");
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
