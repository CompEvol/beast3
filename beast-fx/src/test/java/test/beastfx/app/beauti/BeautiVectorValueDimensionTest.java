package test.beastfx.app.beauti;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import beast.base.spec.inference.parameter.RealVectorParam;
import beastfx.app.beauti.BeautiTabPane;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Labeled;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

/**
 * A vector parameter refuses a number of values that is neither 1 nor its dimension
 * (issue #244). In BEAUti the inline field of a prior and the dialog opened by its
 * "initial" button must both show that refusal, and leave the parameter as it was.
 */
@ExtendWith(ApplicationExtension.class)
public class BeautiVectorValueDimensionTest extends BeautiBase {

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

	/** Dirichlet prior on the frequencies, alpha has dimension 4 */
	@Test
	void inlineFieldRefusesWrongNumberOfValues(FxRobot robot) throws Exception {
		openFrequenciesPrior(robot);
		RealVectorParam<?> alpha = (RealVectorParam<?>) doc.pluginmap.get("FrequenciesPrior.s:anolis")
				.getInput("alpha").get();
		List<Double> before = List.copyOf(alpha.getElements());
		assertEquals(4, before.size());

		// 3 values used to become "1 2 3 1", 5 values used to be cut to "1 2 3 4"
		for (String wrong : new String[] {"1.0 2.0 3.0", "1.0 2.0 3.0 4.0 5.0"}) {
			paste(robot, alphaField(robot), wrong);
			assertEquals(before, alpha.getElements(), wrong + " should be refused and alpha left as it was");
			int n = wrong.split(" ").length;
			assertTrue(errorShown(robot, "has " + n + " values, but dimension 4"), "no error shown for " + wrong);
		}

		paste(robot, alphaField(robot), "1.0 2.0 3.0 4.0");
		assertEquals(List.of(1.0, 2.0, 3.0, 4.0), alpha.getElements());
	}

	/** the real OK button of the dialog: a warning is shown and the dialog stays open */
	@Test
	void dialogRefusesWrongNumberOfValues(FxRobot robot) throws Exception {
		openFrequenciesPrior(robot);
		RealVectorParam<?> freqs = (RealVectorParam<?>) doc.pluginmap.get("freqParameter.s:anolis");
		List<Double> before = List.copyOf(freqs.getElements());

		robot.clickOn(initialButton(robot, "freqParameter.s:anolis"));
		WaitForAsyncUtils.waitForFxEvents();
		DialogPane dialog = dialogWith(robot, "dimension");
		assertNotNull(dialog, "the dialog for freqParameter did not open");

		// 4 values with dimension 3
		enterInDialog(robot, fieldIn(dialog, "dimension"), "3");
		robot.clickOn(buttonIn(dialog, "OK"));
		WaitForAsyncUtils.waitForFxEvents();
		DialogPane warning = robot.lookup(n -> n instanceof DialogPane p && p != dialog
				&& p.getContentText() != null && p.getContentText().contains("has 4 values, but dimension 3")).query();
		assertNotNull(warning, "no warning shown for 4 values with dimension 3");
		robot.clickOn(buttonIn(warning, "OK"));
		WaitForAsyncUtils.waitForFxEvents();

		assertTrue(dialog.getScene().getWindow().isShowing(), "the dialog should stay open after the warning");
		robot.clickOn(buttonIn(dialog, "Cancel"));
		WaitForAsyncUtils.waitForFxEvents();
		assertTrue(dialog.getScene() == null || !dialog.getScene().getWindow().isShowing(), "Cancel should close the dialog");
		assertEquals(before, freqs.getElements(), "the frequencies should be unchanged");
	}

	private void openFrequenciesPrior(FxRobot robot) {
		importAlignment(NEXUS_DIR, new File("anolis.nex"));
		robot.clickOn("#SiteModel");
		robot.clickOn("#substModelComboBox").clickOn("HKY");
		selectTab(robot, "Priors");
		clickOnNodesWithID(robot, "FrequenciesPrior.s:anolis.editButton");
		WaitForAsyncUtils.waitForFxEvents();
	}

	private TextField alphaField(FxRobot robot) {
		return (TextField) robot.lookup(n -> n instanceof TextField && n.isVisible() && "alpha".equals(n.getId())).query();
	}

	private Button initialButton(FxRobot robot, String paramID) {
		Set<Node> buttons = robot.lookup(n -> n instanceof Button b && b.isVisible() && b.getScene() != null
				&& b.getTooltip() != null
				&& ("Initial value of " + paramID).equals(b.getTooltip().getText())).queryAll();
		assertFalse(buttons.isEmpty(), "no initial button for " + paramID);
		return (Button) buttons.iterator().next();
	}

	private DialogPane dialogWith(FxRobot robot, String fieldID) {
		for (Node n : robot.lookup(n -> n instanceof DialogPane).queryAll()) {
			if (fieldIn((DialogPane) n, fieldID) != null) {
				return (DialogPane) n;
			}
		}
		return null;
	}

	private static TextField fieldIn(DialogPane pane, String id) {
		return (TextField) pane.lookupAll("*").stream()
				.filter(n -> n instanceof TextField && id.equals(n.getId())).findFirst().orElse(null);
	}

	private static Button buttonIn(DialogPane pane, String text) {
		return (Button) pane.lookupAll(".button").stream()
				.filter(n -> n instanceof Button b && text.equals(b.getText())).findFirst()
				.orElseThrow(() -> new AssertionError("no " + text + " button"));
	}

	/**
	 * Put the whole text in the field at once, as a paste does, and press Enter. Typing it key by
	 * key would set the values the field holds after each key, e.g. "1.0" on the way to "1.0 2.0".
	 */
	private void paste(FxRobot robot, TextField field, String text) {
		assertNotNull(field, "no text field");
		robot.interact(() -> { field.requestFocus(); field.setText(text); });
		robot.type(KeyCode.ENTER);
		WaitForAsyncUtils.waitForFxEvents();
	}

	/** an error marker next to a field, with a tooltip containing message */
	private boolean errorShown(FxRobot robot, String message) {
		return !robot.lookup(n -> n instanceof Labeled l && l.isVisible() && l.getTooltip() != null
				&& l.getTooltip().getText().contains(message)).queryAll().isEmpty();
	}

	/**
	 * Put text in a field of the dialog. The field reads its text when a key is released;
	 * End is used for that, because Enter would press the dialog's default button, OK.
	 */
	private void enterInDialog(FxRobot robot, TextField field, String text) {
		assertNotNull(field, "no text field");
		robot.interact(() -> { field.requestFocus(); field.setText(text); });
		robot.type(KeyCode.END);
		WaitForAsyncUtils.waitForFxEvents();
	}
}
