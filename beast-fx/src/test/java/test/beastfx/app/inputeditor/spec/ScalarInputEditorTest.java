package test.beastfx.app.inputeditor.spec;


import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.spec.domain.Int;
import beast.base.spec.domain.NonNegativeReal;
import beast.base.spec.domain.PositiveInt;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.distribution.Normal;
import beast.base.spec.inference.distribution.Poisson;
import beast.base.spec.inference.distribution.TruncatedInt;
import beast.base.spec.inference.distribution.TruncatedReal;
import beast.base.spec.inference.distribution.Uniform;
import beast.base.spec.inference.parameter.IntScalarParam;
import beast.base.spec.inference.parameter.RealScalarParam;
import beastfx.app.inputeditor.spec.ScalarInputEditor;
import beastfx.app.inputeditor.BeautiConfig;
import beastfx.app.inputeditor.BeautiDoc;
import beastfx.app.inputeditor.InputEditor;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
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
	 * #330: an unset lower/upper of a Bounded prior is a number field showing the default
	 * bound, not a combo box of templates that would accept a distribution as the bound.
	 * Typing a number sets a fixed value; clearing the field unsets the bound again.
	 */
	@Test
	public void unsetBoundOfBoundedRealIsANumberField() throws Exception {
		AtomicReference<Throwable> error = new AtomicReference<>();
		runOnFXThread(() -> {
			BeautiDoc doc = new BeautiDoc();
			doc.loadTemplate(doc.processTemplate(BeautiConfig.TEMPLATE_DIR + "/Standard.xml"));

			RealScalarParam<PositiveReal> shape = new RealScalarParam<>(1.0, PositiveReal.INSTANCE);
			shape.setID("gammaShape.s:test");
			Normal normal = new Normal();
			normal.initByName("mean", new RealScalarParam<>(0.0, Real.INSTANCE),
					"sigma", new RealScalarParam<>(1.0, PositiveReal.INSTANCE));
			// as GammaShapePrior in Standard.xml: a lower bound, no upper
			TruncatedReal bounded = new TruncatedReal();
			bounded.initByName("param", shape, "distribution", normal,
					"lower", new RealScalarParam<>(0.1, Real.INSTANCE));
			bounded.setID("GammaShapePrior.s:test");
			doc.registerPlugin(shape);
			doc.registerPlugin(bounded);

			TextField field = unsetBoundField(doc, bounded, "upper", String.valueOf(Real.INSTANCE.getUpper()));

			type(field, "10");
			assertTrue(bounded.upperInput.get() instanceof RealScalarParam<?> p && p.get() == 10.0,
					"typing a number sets the upper bound, but it is " + bounded.upperInput.get());
			assertFalse(((RealScalarParam<?>) bounded.upperInput.get()).isEstimatedInput.get(),
					"a bound typed in is a fixed value, not estimated");

			// backspacing to empty and typing a new value keeps the same parameter (#331 review)
			Object set = bounded.upperInput.get();
			type(field, "");
			assertSame(set, bounded.upperInput.get(), "an empty field while typing keeps the bound");
			type(field, "3");
			assertSame(set, bounded.upperInput.get(), "retyping a set bound keeps the same parameter");
			assertEquals(3.0, ((RealScalarParam<?>) set).get());

			type(field, "");
			pressEnter(field);
			assertNull(bounded.upperInput.get(), "clearing the field and pressing Enter unsets the upper bound");
		}, error);
		rethrow(error);
	}

	/** #330, for an integer Bounded prior: the bound typed in is an integer */
	@Test
	public void unsetBoundOfBoundedIntIsANumberField() throws Exception {
		AtomicReference<Throwable> error = new AtomicReference<>();
		runOnFXThread(() -> {
			BeautiDoc doc = new BeautiDoc();
			doc.loadTemplate(doc.processTemplate(BeautiConfig.TEMPLATE_DIR + "/Standard.xml"));

			IntScalarParam<PositiveInt> count = new IntScalarParam<>(1, PositiveInt.INSTANCE);
			count.setID("count.s:test");
			TruncatedInt bounded = new TruncatedInt();
			bounded.initByName("param", count,
					"distribution", new Poisson(null, new RealScalarParam<>(2.0, NonNegativeReal.INSTANCE)),
					"lower", new IntScalarParam<>(1, Int.INSTANCE));
			bounded.setID("CountPrior.s:test");
			doc.registerPlugin(count);
			doc.registerPlugin(bounded);

			TextField field = unsetBoundField(doc, bounded, "upper", String.valueOf(Real.INSTANCE.getUpper()));

			type(field, "8");
			assertTrue(bounded.upperInput.get() instanceof IntScalarParam<?> p && p.get() == 8,
					"typing a number sets the integer upper bound, but it is " + bounded.upperInput.get());
		}, error);
		rethrow(error);
	}

	/**
	 * the editor BEAUti builds for an unset bound: a number editor with an empty field that
	 * shows the default bound, and no combo box
	 */
	private TextField unsetBoundField(BeautiDoc doc, BEASTInterface bounded, String inputName,
			String defaultBound) throws Exception {
		Input<?> input = bounded.getInput(inputName);
		assertNull(input.get(), inputName + " should start unset");
		InputEditor editor = doc.getInputEditorFactory().createInputEditor(input, bounded, doc);
		assertTrue(editor instanceof ScalarInputEditor,
				"an unset bound gets the number editor, not " + editor.getClass().getSimpleName());
		assertNull(find(editor.getComponent(), ComboBox.class, null),
				"an unset bound must not be a combo box of templates");
		TextField field = find(editor.getComponent(), TextField.class, inputName);
		assertNotNull(field, "no \"" + inputName + "\" field found");
		assertEquals("", field.getText(), "an unset bound shows no value");
		assertEquals(defaultBound, field.getPromptText(), "an unset bound shows the default bound");
		return field;
	}

	/** set the field's text and release a key, as typing does */
	private static void type(TextField field, String text) {
		field.setText(text);
		field.getOnKeyReleased().handle(new KeyEvent(KeyEvent.KEY_RELEASED, "", "", KeyCode.UNDEFINED,
				false, false, false, false));
	}

	/** press Enter in the field */
	private static void pressEnter(TextField field) {
		field.getOnAction().handle(new javafx.event.ActionEvent());
	}

	/** the first node of the given class (and id, unless null) in the tree rooted at node */
	private static <T extends Node> T find(Node node, Class<T> type, String id) {
		if (type.isInstance(node) && (id == null || id.equals(node.getId()))) {
			return type.cast(node);
		}
		if (node instanceof Parent parent) {
			for (Node child : parent.getChildrenUnmodifiable()) {
				T found = find(child, type, id);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	}

	private static void rethrow(AtomicReference<Throwable> error) {
		if (error.get() instanceof AssertionError a) {
			throw a;
		}
		if (error.get() != null) {
			error.get().printStackTrace();
			fail("test could not be run: " + error.get());
		}
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
