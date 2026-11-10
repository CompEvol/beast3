package test.beastfx.app.inputeditor;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.inference.CompoundDistribution;
import beast.base.inference.Distribution;
import beast.base.inference.State;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.domain.UnitInterval;
import beast.base.spec.evolution.tree.MRCAPrior;
import beast.base.spec.inference.distribution.Beta;
import beast.base.spec.inference.distribution.LogUniform;
import beast.base.spec.inference.distribution.Normal;
import beast.base.spec.inference.distribution.ScalarDistribution;
import beast.base.spec.inference.distribution.Uniform;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.type.RealScalar;
import beastfx.app.inputeditor.BeautiConfig;
import beastfx.app.inputeditor.BeautiDoc;
import beastfx.app.inputeditor.BeautiSubTemplate;
import beastfx.app.inputeditor.InputEditor;
import beastfx.app.inputeditor.InputEditor.ButtonStatus;
import beastfx.app.inputeditor.InputEditor.ExpandOption;
import beastfx.app.inputeditor.ScalarDistributionInputEditor;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;


/**
 * ScalarDistributionInputEditor is used both for entries of a list of distributions
 * (as in the priors panel) and for single ScalarDistribution valued inputs, like the
 * distr input of a Prior. This tests the latter, which is how packages such as
 * bModelTest wrap a distribution inside another Distribution.
 */
@ExtendWith(ApplicationExtension.class)
public class ScalarDistributionInputEditorTest {

	/** Distribution with a single ScalarDistribution valued input, like bModelTest's BMTPrior **/
	public static class DistrWrapper extends Distribution {
		public Input<ScalarDistribution<RealScalar<? extends Real>, Double>> distInput = new Input<>("distr",
				"distribution the prior applies to", Validate.REQUIRED);

		@Override
		public void initAndValidate() {}

		@Override
		public double calculateLogP() {
			return 0;
		}

		@Override
		public List<String> getArguments() {
			return new ArrayList<>();
		}

		@Override
		public List<String> getConditions() {
			return new ArrayList<>();
		}

		@Override
		public void sample(State state, Random random) {}
	}


	@Start
	public void start(Stage stage) {
		// nothing to show, only the JavaFX toolkit is required
	}

	/** the editor caches templates in static fields, so do not leak them into other tests **/
	@BeforeEach
	@AfterEach
	public void clearTemplateCache() throws Exception {
		for (String name : new String[] {"scalarTemplates", "templateInstances", "templateDomains"}) {
			Field field = ScalarDistributionInputEditor.class.getDeclaredField(name);
			field.setAccessible(true);
			field.set(null, null);
		}
	}


	@Test
	public void testEditorForSingleValuedDistrInput() throws Exception {
		AtomicReference<Throwable> error = new AtomicReference<>();
		List<Throwable> failures = new ArrayList<>();
		runOnFXThread(() -> {
			BeautiDoc doc = new BeautiDoc();
			doc.loadTemplate(doc.processTemplate(BeautiConfig.TEMPLATE_DIR + "/Standard.xml"));

			// more than one wrapper, since a failure for the first one used to leave
			// the static template cache half initialised, making every editor after
			// it fail with an unrelated IndexOutOfBoundsException
			for (int i = 0; i < 2; i++) {
				DistrWrapper wrapper = new DistrWrapper();
				Beta beta = new Beta();
				beta.initByName(
						"alpha", new RealScalarParam<>(1.0, PositiveReal.INSTANCE),
						"beta", new RealScalarParam<>(4.0, PositiveReal.INSTANCE));
				beta.setID("Beta" + i + ".s:test");
				wrapper.initByName("distr", asRealScalarDistribution(beta));
				wrapper.setID("wrapperPrior" + i + ".s:test");
				doc.registerPlugin(beta);
				doc.registerPlugin(wrapper);

				// this is what BEAUti does for every input of an object shown in a panel
				try {
					InputEditor editor = doc.getInputEditorFactory().createInputEditor(
							(Input<?>) wrapper.distInput, (BEASTInterface) wrapper, doc);
					assertNotNull(editor, "no input editor created for distr input");
				} catch (Throwable e) {
					e.printStackTrace();
					failures.add(e);
				}
			}
		}, error);

		if (error.get() != null) {
			error.get().printStackTrace();
			fail("test could not be run: " + error.get());
		}
		if (!failures.isEmpty()) {
			fail("could not create input editor for distr input: " + failures);
		}
	}


	/**
	 * An MRCAPrior starts out without a distribution, which used to make the editor
	 * for its distr input throw a NullPointerException, so that BEAUti popped up a
	 * "Could not add entry for distr" message when the prior was added (issue #128).
	 */
	@Test
	public void testEditorForMRCAPriorWithoutDistribution() throws Exception {
		AtomicReference<Throwable> error = new AtomicReference<>();
		List<Throwable> failures = new ArrayList<>();
		AtomicReference<InputEditor> editor = new AtomicReference<>();
		runOnFXThread(() -> {
			BeautiDoc doc = new BeautiDoc();
			doc.loadTemplate(doc.processTemplate(BeautiConfig.TEMPLATE_DIR + "/Standard.xml"));

			MRCAPrior prior = new MRCAPrior();
			prior.setID("allTaxa.prior");
			doc.registerPlugin(prior);

			// this is what BEAUti does for every input of an item in the priors panel
			try {
				editor.set(doc.getInputEditorFactory().createInputEditor(
						(Input<?>) prior.distInput, (BEASTInterface) prior, doc));
			} catch (Throwable e) {
				e.printStackTrace();
				failures.add(e);
			}
		}, error);

		if (error.get() != null) {
			error.get().printStackTrace();
			fail("test could not be run: " + error.get());
		}
		if (!failures.isEmpty()) {
			fail("could not create input editor for distr input without distribution: " + failures);
		}
		assertNotNull(editor.get(), "no input editor created for distr input");
		// the editor should offer a way to select a distribution
		assertNotNull(findComboBox(editor.get().getComponent()),
				"no combo box to select a distribution from");
	}


	/**
	 * The prior dropdown for a positive parameter, such as the Yule birth rate, did not
	 * offer a uniform distribution, because Uniform is typed for a Real-domain parameter
	 * (issue #217). NonNegativeUniform fills that gap, since BEAUti treats NonNegativeReal
	 * priors as compatible with PositiveReal parameters.
	 */
	@Test
	public void testUniformOfferedForPositiveParameter() throws Exception {
		AtomicReference<Throwable> error = new AtomicReference<>();
		List<String> offered = new ArrayList<>();
		runOnFXThread(() -> {
			BeautiDoc doc = new BeautiDoc();
			doc.loadTemplate(doc.processTemplate(BeautiConfig.TEMPLATE_DIR + "/Standard.xml"));

			RealScalarParam<PositiveReal> birthRate = new RealScalarParam<>(1.0, PositiveReal.INSTANCE);
			birthRate.setID("birthRate.t:test");
			LogUniform logUniform = new LogUniform();
			logUniform.initByName("param", birthRate,
					"lower", new RealScalarParam<>(1.0E-8, PositiveReal.INSTANCE),
					"upper", new RealScalarParam<>(1.0E8, PositiveReal.INSTANCE));
			logUniform.setID("BirthRatePrior.t:test");
			CompoundDistribution prior = new CompoundDistribution();
			prior.initByName("distribution", logUniform);
			prior.setID("prior");
			doc.registerPlugin(birthRate);
			doc.registerPlugin(logUniform);
			doc.registerPlugin(prior);

			// this is what the priors panel does for its list of distributions
			InputEditor editor = doc.getInputEditorFactory().createInputEditor(
					(Input<?>) prior.pDistributions, (BEASTInterface) prior, doc);
			ComboBox<?> comboBox = findComboBox(editor.getComponent());
			assertNotNull(comboBox, "no combo box to select a distribution from");
			for (Object item : comboBox.getItems()) {
				offered.add(((BeautiSubTemplate) item).getID());
			}
		}, error);

		if (error.get() != null) {
			error.get().printStackTrace();
			fail("test could not be run: " + error.get());
		}
		assertTrue(offered.contains("NonNegativeUniform"), "NonNegativeUniform not offered: " + offered);
		assertTrue(offered.contains("LogUniform"), "LogUniform not offered: " + offered);
		// Uniform, for a Real-domain parameter, still should not be offered
		assertFalse(offered.contains("Uniform"), "Uniform should not be offered: " + offered);
	}


	/**
	 * Selecting another distribution in the priors panel must attach the parameter to
	 * the new distribution straight away, so that the parameter's effective bounds
	 * reflect it without first switching panels (issue #230). The initial value
	 * button shows only the initial value, not bounds.
	 */
	@Test
	public void testSwitchToUniformUpdatesBounds() throws Exception {
		AtomicReference<Throwable> error = new AtomicReference<>();
		AtomicReference<RealScalarParam<Real>> growthRate = new AtomicReference<>();
		AtomicReference<Object> newPrior = new AtomicReference<>();
		List<String> buttonTexts = new ArrayList<>();
		runOnFXThread(() -> {
			BeautiDoc doc = new BeautiDoc();
			doc.loadTemplate(doc.processTemplate(BeautiConfig.TEMPLATE_DIR + "/Standard.xml"));

			RealScalarParam<Real> rate = new RealScalarParam<>(3.0E-4, Real.INSTANCE);
			rate.setID("growthRate.t:test");
			growthRate.set(rate);
			Normal normal = new Normal();
			normal.initByName("param", rate,
					"mean", new RealScalarParam<>(0.0, Real.INSTANCE),
					"sigma", new RealScalarParam<>(1.0, PositiveReal.INSTANCE));
			normal.setID("GrowthRatePrior.t:test");
			CompoundDistribution prior = new CompoundDistribution();
			prior.initByName("distribution", normal);
			prior.setID("prior");
			doc.registerPlugin(rate);
			doc.registerPlugin(normal);
			doc.registerPlugin(prior);

			InputEditor editor = doc.getInputEditorFactory().createInputEditor(
					(Input<?>) prior.pDistributions, (BEASTInterface) prior, doc);
			collectButtonTexts(editor.getComponent(), buttonTexts);

			@SuppressWarnings("unchecked")
			ComboBox<BeautiSubTemplate> comboBox = (ComboBox<BeautiSubTemplate>) findComboBox(editor.getComponent());
			assertNotNull(comboBox, "no combo box to select a distribution from");
			BeautiSubTemplate uniform = comboBox.getItems().stream()
					.filter(t -> t.getID().equals("Uniform")).findFirst().orElse(null);
			assertNotNull(uniform, "Uniform not offered for a Real parameter");
			// select it, then run the combo box's action as selecting it in the GUI does
			// (setValue alone does not fire it while the combo box is not showing)
			comboBox.setValue(uniform);
			comboBox.getOnAction().handle(new ActionEvent(comboBox, comboBox));
			newPrior.set(prior.pDistributions.get().get(0));
		}, error);

		if (error.get() != null) {
			error.get().printStackTrace();
			fail("test could not be run: " + error.get());
		}
		assertTrue(newPrior.get() instanceof Uniform, "prior was not replaced by Uniform: " + newPrior.get());
		assertEquals(0.0, growthRate.get().getLower(), 0.0);
		assertEquals(1.0, growthRate.get().getUpper(), 0.0);
		assertTrue(buttonTexts.contains("initial = 3.0E-4"), "initial value button text: " + buttonTexts);
	}


	/**
	 * Editing a parameter of the selected distribution, e.g. the lower of a Uniform,
	 * must update the parameters shown in the combo box label straight away, not
	 * only after switching panels.
	 */
	@Test
	public void testEditingDistributionParameterUpdatesComboBox() throws Exception {
		AtomicReference<Throwable> error = new AtomicReference<>();
		AtomicReference<String> before = new AtomicReference<>();
		AtomicReference<String> after = new AtomicReference<>();
		runOnFXThread(() -> {
			BeautiDoc doc = new BeautiDoc();
			doc.loadTemplate(doc.processTemplate(BeautiConfig.TEMPLATE_DIR + "/Standard.xml"));

			RealScalarParam<Real> rate = new RealScalarParam<>(0.7, Real.INSTANCE);
			rate.setID("growthRate.t:test");
			Uniform uniform = new Uniform();
			uniform.initByName("param", rate,
					"lower", new RealScalarParam<>(0.0, Real.INSTANCE),
					"upper", new RealScalarParam<>(1.0, Real.INSTANCE));
			uniform.setID("GrowthRatePrior.t:test");
			CompoundDistribution prior = new CompoundDistribution();
			prior.initByName("distribution", uniform);
			prior.setID("prior");
			doc.registerPlugin(rate);
			doc.registerPlugin(uniform);
			doc.registerPlugin(prior);

			// expanded, as in the priors panel, so that the parameters of the
			// distribution can be edited
			InputEditor editor = doc.getInputEditorFactory().createInputEditor(
					(Input<?>) prior.pDistributions, (BEASTInterface) prior, true,
					ExpandOption.TRUE, ButtonStatus.ALL, null, doc);
			// the combo box label is only rendered once the editor is in a scene
			Parent root = editor.getComponent();
			new Scene(root);
			root.applyCss();
			root.layout();
			ComboBox<?> comboBox = findComboBox(root);
			assertNotNull(comboBox, "no combo box to select a distribution from");
			before.set(comboBox.getButtonCell().getText());

			TextField lower = findTextField(editor.getComponent(), "lower");
			assertNotNull(lower, "no text field for the lower of the Uniform");
			// typing a value is processed when the key is released
			lower.setText("0.5");
			lower.getOnKeyReleased().handle(new KeyEvent(KeyEvent.KEY_RELEASED, "5", "5", KeyCode.DIGIT5,
					false, false, false, false));
			after.set(comboBox.getButtonCell().getText());
		}, error);

		if (error.get() != null) {
			error.get().printStackTrace();
			fail("test could not be run: " + error.get());
		}
		assertEquals("Uniform[0.0,1.0]", before.get());
		assertEquals("Uniform[0.5,1.0]", after.get());
	}


	/** text field with the given id in the node tree rooted at node, or null if there is none **/
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


	private static void collectButtonTexts(Node node, List<String> texts) {
		if (node instanceof Button button) {
			texts.add(button.getText());
		}
		if (node instanceof Parent parent) {
			for (Node child : parent.getChildrenUnmodifiable()) {
				collectButtonTexts(child, texts);
			}
		}
	}


	/** first combo box in the node tree rooted at node, or null if there is none **/
	private static ComboBox<?> findComboBox(Node node) {
		if (node instanceof ComboBox<?> comboBox) {
			return comboBox;
		}
		if (node instanceof Parent parent) {
			for (Node child : parent.getChildrenUnmodifiable()) {
				ComboBox<?> comboBox = findComboBox(child);
				if (comboBox != null) {
					return comboBox;
				}
			}
		}
		return null;
	}


	@SuppressWarnings("unchecked")
	private static ScalarDistribution<RealScalar<? extends Real>, Double> asRealScalarDistribution(
			ScalarDistribution<RealScalar<UnitInterval>, Double> dist) {
		return (ScalarDistribution<RealScalar<? extends Real>, Double>) (ScalarDistribution<?, ?>) dist;
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
