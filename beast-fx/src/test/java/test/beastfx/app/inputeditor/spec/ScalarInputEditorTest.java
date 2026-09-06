package test.beastfx.app.inputeditor.spec;


import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.distribution.Uniform;
import beast.base.spec.inference.parameter.RealScalarParam;
import beastfx.app.inputeditor.BeautiConfig;
import beastfx.app.inputeditor.BeautiDoc;
import beastfx.app.inputeditor.InputEditor;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.CheckBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Regression test: a distribution's own "lower"/"upper" bound (e.g. Uniform's support)
 * defines the distribution itself, not a value to be sampled -- there is no sensible
 * prior/operator for "the bound of my own bound", so the "estimate" checkbox must never
 * be offered for it, unlike a genuine hyperparameter (e.g. Normal's mean/sigma).
 */
@ExtendWith(ApplicationExtension.class)
public class ScalarInputEditorTest {

	@Start
	public void start(Stage stage) {
		// nothing to show, only the JavaFX toolkit is required
	}

	@Test
	public void uniformLowerAndUpperNeverOfferEstimate() throws Exception {
		AtomicReference<Throwable> error = new AtomicReference<>();
		AtomicReference<Boolean> lowerVisible = new AtomicReference<>();
		AtomicReference<Boolean> upperVisible = new AtomicReference<>();
		runOnFXThread(() -> {
			BeautiDoc doc = new BeautiDoc();
			doc.loadTemplate(doc.processTemplate(BeautiConfig.TEMPLATE_DIR + "/Standard.xml"));

			RealScalarParam<Real> lower = new RealScalarParam<>(-1e307, Real.INSTANCE);
			lower.setID("lower.s:test");
			RealScalarParam<Real> upper = new RealScalarParam<>(1e307, Real.INSTANCE);
			upper.setID("upper.s:test");

			Uniform uniform = new Uniform();
			uniform.initByName("lower", lower, "upper", upper);
			uniform.setID("uniformDist.s:test");

			doc.registerPlugin(lower);
			doc.registerPlugin(upper);
			doc.registerPlugin(uniform);

			lowerVisible.set(isEstimateCheckboxVisible(doc, uniform, "lower"));
			upperVisible.set(isEstimateCheckboxVisible(doc, uniform, "upper"));
		}, error);

		if (error.get() != null) {
			error.get().printStackTrace();
			fail("test could not be run: " + error.get());
		}

		assertFalse(lowerVisible.get(), "the estimate checkbox for Uniform's own \"lower\" bound must never be shown");
		assertFalse(upperVisible.get(), "the estimate checkbox for Uniform's own \"upper\" bound must never be shown");
	}


	/**
	 * Builds the editor exactly as BEAUti does for a single (non-list) Input, the way an
	 * expanded distribution's own hyperparameter row is rendered, and reports whether its
	 * "estimate" checkbox ends up visible.
	 */
	private boolean isEstimateCheckboxVisible(BeautiDoc doc, BEASTInterface beastObject, String inputName) throws Exception {
		Input<?> input = beastObject.getInput(inputName);
		InputEditor editor = doc.getInputEditorFactory().createInputEditor(input, beastObject, doc);
		assertNotNull(editor, "no input editor created for " + inputName);

		CheckBox checkBox = findCheckBox(editor.getComponent(), inputName + ".isEstimated");
		assertNotNull(checkBox, "no \"" + inputName + ".isEstimated\" checkbox found");
		return checkBox.isVisible();
	}


	/** the CheckBox with the given id in the node tree rooted at node, or null if there is none **/
	private static CheckBox findCheckBox(Node node, String id) {
		if (node instanceof CheckBox checkBox && id.equals(checkBox.getId())) {
			return checkBox;
		}
		if (node instanceof Parent parent) {
			for (Node child : parent.getChildrenUnmodifiable()) {
				CheckBox checkBox = findCheckBox(child, id);
				if (checkBox != null) {
					return checkBox;
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
