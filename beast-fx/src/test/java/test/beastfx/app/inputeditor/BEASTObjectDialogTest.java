package test.beastfx.app.inputeditor;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import beast.base.spec.domain.Real;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.inference.parameter.RealVectorParam;
import beastfx.app.inputeditor.BEASTObjectDialog;
import beastfx.app.inputeditor.BeautiConfig;
import beastfx.app.inputeditor.BeautiDoc;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;


/**
 * The dialog opened by the "initial = ..." button of a prior shows both the Value and the
 * Dimension of a vector parameter. A vector parameter fills its dimension by repeating its
 * values, so changing the dimension in the dialog used to change the stored values silently
 * (dimension 3 with "0.1 0.2" became [0.1, 0.2, 0.1]) while the Value field still showed
 * "0.1 0.2". The dialog now only accepts 1 value or one value per element.
 */
@ExtendWith(ApplicationExtension.class)
public class BEASTObjectDialogTest {

	@Start
	public void start(Stage stage) {
		// nothing to show, only the JavaFX toolkit is required
	}


	@Test
	public void testValueAndDimensionCheck() throws Exception {
		AtomicReference<Throwable> error = new AtomicReference<>();
		List<String> messages = new ArrayList<>();
		runOnFXThread(() -> {
			BeautiDoc doc = new BeautiDoc();
			doc.loadTemplate(doc.processTemplate(BeautiConfig.TEMPLATE_DIR + "/Standard.xml"));
			RealVectorParam<Real> rates = new RealVectorParam<>(new double[] {0.1, 0.2}, Real.INSTANCE);
			rates.setID("rates.c:test");
			doc.registerPlugin(rates);

			// unchanged: 2 values, dimension 2
			messages.add(check(doc, rates, null, null));
			// dimension changed to 3, still 2 values: refused
			messages.add(check(doc, rates, null, "3"));
			// dimension 3 with 3 values: accepted
			messages.add(check(doc, rates, "0.1 0.2 0.3", "3"));
			// dimension 3 with 1 value, used for every element: accepted
			messages.add(check(doc, rates, "0.5", "3"));
			// 3 values while dimension stays 2: refused (would silently make the dimension 3)
			messages.add(check(doc, rates, "0.1 0.2 0.3", null));
		}, error);

		if (error.get() != null) {
			error.get().printStackTrace();
			fail("test could not be run: " + error.get());
		}
		assertNull(messages.get(0), "unchanged vector refused");
		assertNotNull(messages.get(1), "dimension 3 with 2 values accepted");
		assertTrue(messages.get(1).contains("Value has 2 numbers, but Dimension is 3"), messages.get(1));
		assertNull(messages.get(2), "3 values for dimension 3 refused");
		assertNull(messages.get(3), "1 value for dimension 3 refused");
		assertNotNull(messages.get(4), "3 values for dimension 2 accepted");
		assertTrue(messages.get(4).contains("Value has 3 numbers, but Dimension is 2"), messages.get(4));
	}


	/**
	 * setDimension(), used by models that size a parameter in code, updates the dimension but
	 * not the value, so a parameter can be mismatched before the dialog is opened. OK must
	 * then still be allowed when the user changes nothing, and checked once they edit.
	 */
	@Test
	public void testMismatchNotMadeInDialogIsAllowed() throws Exception {
		AtomicReference<Throwable> error = new AtomicReference<>();
		List<String> messages = new ArrayList<>();
		runOnFXThread(() -> {
			BeautiDoc doc = new BeautiDoc();
			doc.loadTemplate(doc.processTemplate(BeautiConfig.TEMPLATE_DIR + "/Standard.xml"));
			RealVectorParam<Real> rates = new RealVectorParam<>(new double[] {0.1, 0.2}, Real.INSTANCE);
			rates.setID("rates.c:test");
			doc.registerPlugin(rates);
			rates.setDimension(3);   // value is still [0.1, 0.2]

			messages.add(check(doc, rates, null, null));           // no edit: allowed
			messages.add(check(doc, rates, "0.1 0.2 0.4", null));   // edited to fit: allowed
			messages.add(check(doc, rates, "0.1 0.4", null));       // edited, still mismatched: refused
		}, error);

		if (error.get() != null) {
			error.get().printStackTrace();
			fail("test could not be run: " + error.get());
		}
		assertNull(messages.get(0), "unchanged parameter refused");
		assertNull(messages.get(1), "3 values for dimension 3 refused");
		assertNotNull(messages.get(2), "2 values for dimension 3 accepted after an edit");
	}


	/** parameters that are not vectors have nothing to check **/
	@Test
	public void testScalarIsNotChecked() throws Exception {
		RealScalarParam<Real> rate = new RealScalarParam<>(0.5, Real.INSTANCE);
		assertNull(BEASTObjectDialog.checkValueAndDimension(rate));
	}


	/**
	 * Open the dialog for p, type the given value and dimension (null leaves the field as
	 * it is) as a user would, and return the result of the check made when OK is pressed.
	 */
	private static String check(BeautiDoc doc, RealVectorParam<?> p, String value, String dimension) {
		BEASTObjectDialog dlg = new BEASTObjectDialog(p, p.getClass(), doc);
		if (value != null) {
			type(findTextField(dlg.m_panel, "value"), value);
		}
		if (dimension != null) {
			type(findTextField(dlg.m_panel, "dimension"), dimension);
		}
		// the check made when OK is pressed
		return BEASTObjectDialog.checkValueAndDimension(dlg.m_panel.m_beastObject, p);
	}

	/** typing a value is processed when the key is released **/
	private static void type(TextField field, String text) {
		assertNotNull(field, "no text field");
		field.setText(text);
		field.getOnKeyReleased().handle(new KeyEvent(KeyEvent.KEY_RELEASED, "1", "1", KeyCode.DIGIT1,
				false, false, false, false));
	}

	private static TextField findTextField(Node node, String id) {
		if (node instanceof TextField textField && id.equals(textField.getId())) {
			return textField;
		}
		if (node instanceof Parent parent) {
			for (Node child : parent.getChildrenUnmodifiable()) {
				TextField textField = findTextField(child, id);
				if (textField != null) {
					return textField;
				}
			}
		}
		return null;
	}

	/** run task on the JavaFX application thread, recording any exception in error **/
	private static void runOnFXThread(FXTask task, AtomicReference<Throwable> error) throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		Platform.runLater(() -> {
			try {
				task.run();
			} catch (Throwable e) {
				error.set(e);
			} finally {
				latch.countDown();
			}
		});
		if (!latch.await(120, TimeUnit.SECONDS)) {
			throw new AssertionError("timed out waiting for JavaFX thread");
		}
	}

	private interface FXTask {
		void run() throws Exception;
	}
}
