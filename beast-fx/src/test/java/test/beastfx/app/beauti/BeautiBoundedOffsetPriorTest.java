package test.beastfx.app.beauti;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import beastfx.app.beauti.BeautiTabPane;
import beastfx.app.inputeditor.BeautiSubTemplate;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Bounded Real with Offset Real inside it (issue #232). The Priors panel must show the
 * inputs of the distribution inside Offset, and must not offer a Uniform-type distribution
 * inside Bounded, since its bounds would be shown twice.
 */
@ExtendWith(ApplicationExtension.class)
public class BeautiBoundedOffsetPriorTest extends BeautiBase {

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
	void boundedOffsetShowsInnermostInputs(FxRobot robot) throws Exception {
		importAlignment(NEXUS_DIR, new File("anolis.nex"));
		robot.clickOn("#SiteModel");
		robot.clickOn("#substModelComboBox").clickOn("HKY");
		selectTab(robot, "Priors");

		selectFromCombobox(robot, "kappa.s:anolis.distr", "Bounded Real");
		WaitForAsyncUtils.waitForFxEvents();
		if (showingTextFields(robot, "upper").isEmpty()) {
			clickOnNodesWithID(robot, "KappaPrior.s:anolis.editButton");
			WaitForAsyncUtils.waitForFxEvents();
		}
		assertFalse(showingTextFields(robot, "upper").isEmpty(), "the editor of the Bounded prior is not expanded");

		// the box for the distribution inside Bounded
		ComboBox<BeautiSubTemplate> inner = comboBoxOffering(robot, "OffsetReal", null);
		assertNotNull(inner, "no box offers Offset inside Bounded");
		List<String> ids = new ArrayList<>();
		for (BeautiSubTemplate t : inner.getItems()) {
			ids.add(t.getID());
		}
		System.err.println("Inside Bounded: " + ids);
		for (String uniform : new String[] {"Uniform", "NonNegativeUniform", "LogUniform"}) {
			assertFalse(ids.contains(uniform), uniform + " is offered inside Bounded: " + ids);
		}

		select(robot, inner, "OffsetReal");
		screenshot("/tmp/boundedOffset1.png");
		assertFalse(showingTextFields(robot, "offset").isEmpty(), "no field for the offset");

		// the box for the distribution inside Offset: pick Normal, whose inputs must show
		ComboBox<BeautiSubTemplate> innermost = comboBoxOffering(robot, "Normal", "OffsetReal");
		assertNotNull(innermost, "no box for the distribution inside Offset");
		select(robot, innermost, "Normal");
		screenshot("/tmp/boundedOffset2.png");
		assertFalse(showingTextFields(robot, "offset").isEmpty(), "no field for the offset after choosing Normal");
		assertFalse(showingTextFields(robot, "mean").isEmpty(), "no field for the mean of the Normal inside Offset");
		assertFalse(showingTextFields(robot, "sigma").isEmpty(), "no field for the sigma of the Normal inside Offset");
		// only kappa has an initial value; the row for the distribution inside Offset has none
		// (it used to show "initial = 0.0", the placeholder param of the inner Offset)
		assertFalse(robot.lookup(n -> n instanceof Button b && isShowing(n) && "initial = 0.0".equals(b.getText()))
				.tryQuery().isPresent(), "initial value button shown for the distribution inside Offset");

		// and another one: Gamma has alpha and beta instead
		innermost = comboBoxOffering(robot, "Gamma", "OffsetReal");
		select(robot, innermost, "Gamma");
		screenshot("/tmp/boundedOffset3.png");
		assertTrue(showingTextFields(robot, "mean").isEmpty(), "the Normal's fields are still shown");
		assertFalse(showingTextFields(robot, "alpha").isEmpty(), "no field for the alpha of the Gamma inside Offset");

		// collapse it again: expanded priors are remembered in a static set, which would
		// otherwise leave kappa expanded for the next BEAUti test in the same JVM
		clickOnNodesWithID(robot, "KappaPrior.s:anolis.editButton");
		WaitForAsyncUtils.waitForFxEvents();
		assertTrue(showingTextFields(robot, "upper").isEmpty(), "the editor of the Bounded prior is still expanded");
	}

	private void select(FxRobot robot, ComboBox<BeautiSubTemplate> box, String templateID) {
		robot.interact(() -> {
			for (BeautiSubTemplate t : box.getItems()) {
				if (t.getID().equals(templateID)) {
					box.setValue(t);
					return;
				}
			}
			throw new AssertionError(templateID + " is not offered");
		});
		WaitForAsyncUtils.waitForFxEvents();
	}

	/**
	 * a showing combo box, other than the one for the prior itself, that offers the template
	 * and not the template notOffered (if given). The panel is rebuilt after each choice, so
	 * the boxes are found again each time.
	 */
	@SuppressWarnings("unchecked")
	private ComboBox<BeautiSubTemplate> comboBoxOffering(FxRobot robot, String templateID, String notOffered) {
		for (Node n : robot.lookup(n -> n instanceof ComboBox<?> && isShowing(n)).queryAll()) {
			ComboBox<?> box = (ComboBox<?>) n;
			if ("kappa.s:anolis.distr".equals(box.getId())) {
				continue;
			}
			boolean offers = false, offersExcluded = false;
			for (Object item : box.getItems()) {
				if (item instanceof BeautiSubTemplate t) {
					offers |= t.getID().equals(templateID);
					offersExcluded |= t.getID().equals(notOffered);
				}
			}
			if (offers && !offersExcluded) {
				return (ComboBox<BeautiSubTemplate>) box;
			}
		}
		return null;
	}

	private List<TextField> showingTextFields(FxRobot robot, String id) {
		List<TextField> fields = new ArrayList<>();
		for (Node n : robot.lookup(n -> n instanceof TextField && id.equals(n.getId()) && isShowing(n)).queryAll()) {
			fields.add((TextField) n);
		}
		return fields;
	}

	private static boolean isShowing(Node node) {
		for (Node n = node; n != null; n = n.getParent()) {
			if (!n.isVisible()) return false;
		}
		return node != null;
	}
}
