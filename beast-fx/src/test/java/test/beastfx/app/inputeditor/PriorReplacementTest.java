package test.beastfx.app.inputeditor;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
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
import beast.base.inference.CompoundDistribution;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.distribution.Dirichlet;
import beast.base.spec.inference.distribution.IID;
import beast.base.spec.inference.distribution.Normal;
import beast.base.spec.inference.distribution.TruncatedReal;
import beast.base.spec.inference.distribution.Uniform;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.inference.parameter.RealVectorParam;
import beast.base.spec.inference.parameter.SimplexParam;
import beastfx.app.inputeditor.BeautiConfig;
import beastfx.app.inputeditor.BeautiDoc;
import beastfx.app.inputeditor.BeautiSubTemplate;
import beastfx.app.inputeditor.IIDInputEditor;
import beastfx.app.inputeditor.InputEditor;
import beastfx.app.inputeditor.InputEditor.ButtonStatus;
import beastfx.app.inputeditor.InputEditor.ExpandOption;
import beastfx.app.inputeditor.ScalarDistributionInputEditor;
import beastfx.app.inputeditor.TensorDistributionInputEditor;
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
 * Replacing a vector prior (IIDInputEditor, TensorDistributionInputEditor), and the
 * inner distribution of an IID, in the priors panel (issue #230).
 */
@ExtendWith(ApplicationExtension.class)
public class PriorReplacementTest {

	@Start
	public void start(Stage stage) {
		// nothing to show, only the JavaFX toolkit is required
	}

	/** the editors cache templates in static fields, so do not leak them into other tests **/
	@BeforeEach
	@AfterEach
	public void clearTemplateCache() throws Exception {
		clearStatic(ScalarDistributionInputEditor.class, "scalarTemplates", "templateInstances", "templateDomains");
		clearStatic(IIDInputEditor.class, "tensorTemplates", "templateInstances", "templateDomains");
		clearStatic(TensorDistributionInputEditor.class, "tensorTemplates", "templateInstances", "templateDomains");
	}

	private static void clearStatic(Class<?> c, String... names) throws Exception {
		for (String name : names) {
			Field field = c.getDeclaredField(name);
			field.setAccessible(true);
			field.set(null, null);
		}
	}


	/** IID prior with a Normal base distribution on a 2-dimensional real vector, in a CompoundDistribution **/
	private static class IIDFixture {
		BeautiDoc doc;
		RealVectorParam<Real> rates;
		IID<?,?,?> iid;
		CompoundDistribution prior;

		IIDFixture() throws Exception {
			doc = new BeautiDoc();
			doc.loadTemplate(doc.processTemplate(BeautiConfig.TEMPLATE_DIR + "/Standard.xml"));

			rates = new RealVectorParam<>(new double[] {0.5, 0.5}, Real.INSTANCE);
			rates.setID("rates.c:test");
			Normal normal = new Normal();
			normal.initByName(
					"mean", new RealScalarParam<>(0.0, Real.INSTANCE),
					"sigma", new RealScalarParam<>(1.0, PositiveReal.INSTANCE));
			normal.setID("Normal.0");
			iid = new IID<>();
			iid.initByName("param", rates, "distr", normal);
			iid.setID("RatesPrior.c:test");
			prior = new CompoundDistribution();
			prior.initByName("distribution", iid);
			prior.setID("prior");
			doc.registerPlugin(rates);
			doc.registerPlugin(normal);
			doc.registerPlugin(iid);
			doc.registerPlugin(prior);
		}

		/** editor for the priors list, expanded as in the priors panel **/
		InputEditor expandedEditor() throws Exception {
			InputEditor editor = doc.getInputEditorFactory().createInputEditor(
					(Input<?>) prior.pDistributions, (BEASTInterface) prior, true,
					ExpandOption.TRUE, ButtonStatus.ALL, null, doc);
			Parent root = editor.getComponent();
			new Scene(root);
			root.applyCss();
			root.layout();
			return editor;
		}
	}


	/**
	 * Selecting another prior for a vector must detach the old prior from the parameter.
	 * The scalar editor already did this, the IID editor did not, so the old prior kept
	 * restricting the parameter's bounds until the panel was rebuilt.
	 */
	@Test
	public void testReplacingIIDDetachesOldPrior() throws Exception {
		AtomicReference<Throwable> error = new AtomicReference<>();
		AtomicReference<IIDFixture> fixture = new AtomicReference<>();
		runOnFXThread(() -> {
			IIDFixture f = new IIDFixture();
			fixture.set(f);
			InputEditor editor = f.expandedEditor();
			ComboBox<BeautiSubTemplate> comboBox = findComboBoxOffering(editor.getComponent(), "IID");
			assertNotNull(comboBox, "no combo box offering IID");
			select(comboBox, "IID");
		}, error);
		rethrow(error);

		IIDFixture f = fixture.get();
		Object newPrior = f.prior.pDistributions.get().get(0);
		assertTrue(newPrior instanceof IID, "prior was not replaced by an IID: " + newPrior);
		assertNotSame(f.iid, newPrior, "prior was not replaced");
		assertFalse(f.rates.getOutputs().contains(f.iid), "old prior still attached to the parameter");
		assertTrue(f.rates.getOutputs().contains(newPrior), "new prior not attached to the parameter");
		assertEquals(f.rates, ((IID<?,?,?>) newPrior).paramInput.get());
	}


	/**
	 * Selecting another inner distribution of an IID (the nested combo box) must update
	 * the bounds of the vector straight away. The IID used to compute its bounds from
	 * the base distribution cached in initAndValidate, which the swap did not update.
	 */
	@Test
	public void testSwitchingIIDBaseDistributionUpdatesBounds() throws Exception {
		AtomicReference<Throwable> error = new AtomicReference<>();
		AtomicReference<IIDFixture> fixture = new AtomicReference<>();
		runOnFXThread(() -> {
			IIDFixture f = new IIDFixture();
			fixture.set(f);
			InputEditor editor = f.expandedEditor();
			ComboBox<BeautiSubTemplate> comboBox = findComboBoxOffering(editor.getComponent(), "Uniform");
			assertNotNull(comboBox, "no combo box offering Uniform as base distribution of the IID");
			select(comboBox, "Uniform");
		}, error);
		rethrow(error);

		IIDFixture f = fixture.get();
		assertTrue(f.iid.distInput.get() instanceof Uniform,
				"base distribution of the IID was not replaced by Uniform: " + f.iid.distInput.get());
		assertEquals(0.0, f.rates.getLower(), 0.0);
		assertEquals(1.0, f.rates.getUpper(), 0.0);
	}


	/** Dirichlet prior on a 2-dimensional simplex, in a CompoundDistribution **/
	private static class DirichletFixture {
		BeautiDoc doc;
		SimplexParam freqs;
		Dirichlet dirichlet;
		CompoundDistribution prior;

		DirichletFixture() throws Exception {
			doc = new BeautiDoc();
			doc.loadTemplate(doc.processTemplate(BeautiConfig.TEMPLATE_DIR + "/Standard.xml"));

			freqs = new SimplexParam(new double[] {0.5, 0.5});
			freqs.setID("freqs.s:test");
			RealVectorParam<PositiveReal> alpha = new RealVectorParam<>(new double[] {2.0, 2.0}, PositiveReal.INSTANCE);
			alpha.setID("alpha.s:test");
			dirichlet = new Dirichlet();
			dirichlet.initByName("param", freqs, "alpha", alpha);
			dirichlet.setID("FreqsPrior.s:test");
			prior = new CompoundDistribution();
			prior.initByName("distribution", dirichlet);
			prior.setID("prior");
			doc.registerPlugin(freqs);
			doc.registerPlugin(alpha);
			doc.registerPlugin(dirichlet);
			doc.registerPlugin(prior);
		}

		InputEditor expandedEditor() throws Exception {
			InputEditor editor = doc.getInputEditorFactory().createInputEditor(
					(Input<?>) prior.pDistributions, (BEASTInterface) prior, true,
					ExpandOption.TRUE, ButtonStatus.ALL, null, doc);
			Parent root = editor.getComponent();
			new Scene(root);
			root.applyCss();
			root.layout();
			return editor;
		}
	}


	/** as testReplacingIIDDetachesOldPrior, for TensorDistributionInputEditor **/
	@Test
	public void testReplacingDirichletDetachesOldPrior() throws Exception {
		AtomicReference<Throwable> error = new AtomicReference<>();
		AtomicReference<DirichletFixture> fixture = new AtomicReference<>();
		runOnFXThread(() -> {
			DirichletFixture f = new DirichletFixture();
			fixture.set(f);
			InputEditor editor = f.expandedEditor();
			ComboBox<BeautiSubTemplate> comboBox = findComboBoxOffering(editor.getComponent(), "Dirichlet");
			assertNotNull(comboBox, "no combo box offering Dirichlet");
			select(comboBox, "Dirichlet");
		}, error);
		rethrow(error);

		DirichletFixture f = fixture.get();
		Object newPrior = f.prior.pDistributions.get().get(0);
		assertTrue(newPrior instanceof Dirichlet, "prior was not replaced by a Dirichlet: " + newPrior);
		assertNotSame(f.dirichlet, newPrior, "prior was not replaced");
		assertFalse(f.freqs.getOutputs().contains(f.dirichlet), "old prior still attached to the parameter");
		assertTrue(f.freqs.getOutputs().contains(newPrior), "new prior not attached to the parameter");
	}


	/**
	 * The label of a vector prior shows the template name followed by the current parameter
	 * values, also while expanded. Editing a parameter in the expanded panel must update the
	 * values in the label straight away.
	 */
	@Test
	public void testExpandedDirichletLabelFollowsAlphaEdit() throws Exception {
		AtomicReference<Throwable> error = new AtomicReference<>();
		AtomicReference<String> before = new AtomicReference<>();
		AtomicReference<String> after = new AtomicReference<>();
		runOnFXThread(() -> {
			DirichletFixture f = new DirichletFixture();
			InputEditor editor = f.expandedEditor();
			ComboBox<BeautiSubTemplate> comboBox = findComboBoxOffering(editor.getComponent(), "Dirichlet");
			assertNotNull(comboBox, "no combo box offering Dirichlet");
			before.set(comboBox.getButtonCell().getText());

			TextField alpha = findTextField(editor.getComponent(), "alpha");
			assertNotNull(alpha, "no text field for alpha of the Dirichlet");
			// typing a value is processed when the key is released
			alpha.setText("3.0 3.0");
			alpha.getOnKeyReleased().handle(new KeyEvent(KeyEvent.KEY_RELEASED, "0", "0", KeyCode.DIGIT0,
					false, false, false, false));
			after.set(comboBox.getButtonCell().getText());
		}, error);
		rethrow(error);

		assertEquals("Dirichlet[2.0, 2.0]", before.get());
		assertEquals("Dirichlet[3.0, 3.0]", after.get());
	}


	/**
	 * The label of a vector prior shows its parameter values whether expanded or collapsed,
	 * and follows an edit of alpha, e.g. for the Dirichlet prior on the nucleotide frequencies.
	 */
	@Test
	public void testDirichletLabelFollowsExpandCollapseAndAlpha() throws Exception {
		AtomicReference<Throwable> error = new AtomicReference<>();
		List<String> labels = new ArrayList<>();
		runOnFXThread(() -> {
			BeautiDoc doc = new BeautiDoc();
			doc.loadTemplate(doc.processTemplate(BeautiConfig.TEMPLATE_DIR + "/Standard.xml"));
			SimplexParam freqs = new SimplexParam(new double[] {0.25, 0.25, 0.25, 0.25});
			freqs.setID("freqParameter.s:test");
			RealVectorParam<PositiveReal> alpha = new RealVectorParam<>(new double[] {4.0, 4.0, 4.0, 4.0}, PositiveReal.INSTANCE);
			alpha.setID("alpha.s:test");
			Dirichlet dirichlet = new Dirichlet();
			dirichlet.initByName("param", freqs, "alpha", alpha);
			dirichlet.setID("FrequenciesPrior.s:test");
			CompoundDistribution prior = new CompoundDistribution();
			prior.initByName("distribution", dirichlet);
			prior.setID("prior");
			doc.registerPlugin(freqs);
			doc.registerPlugin(alpha);
			doc.registerPlugin(dirichlet);
			doc.registerPlugin(prior);

			InputEditor editor = doc.getInputEditorFactory().createInputEditor(
					(Input<?>) prior.pDistributions, (BEASTInterface) prior, true,
					ExpandOption.TRUE, ButtonStatus.ALL, null, doc);
			Parent root = editor.getComponent();
			new Scene(root);
			root.applyCss();
			root.layout();
			ComboBox<BeautiSubTemplate> comboBox = findComboBoxOffering(root, "Dirichlet");
			assertNotNull(comboBox, "no combo box offering Dirichlet");
			Button expandButton = findButton(root, "FrequenciesPrior.s:test.editButton");
			assertNotNull(expandButton, "no expand/collapse button");

			labels.add(comboBox.getButtonCell().getText());     // expanded
			expandButton.fire();                                 // collapse
			labels.add(comboBox.getButtonCell().getText());
			expandButton.fire();                                 // expand
			labels.add(comboBox.getButtonCell().getText());

			TextField alphaField = findTextField(root, "alpha");
			assertNotNull(alphaField, "no text field for alpha");
			alphaField.setText("1.0 2.0 3.0 4.0");
			alphaField.getOnKeyReleased().handle(new KeyEvent(KeyEvent.KEY_RELEASED, "0", "0", KeyCode.DIGIT0,
					false, false, false, false));
			labels.add(comboBox.getButtonCell().getText());     // expanded, after editing alpha
			expandButton.fire();                                 // collapse
			labels.add(comboBox.getButtonCell().getText());
		}, error);
		rethrow(error);

		assertEquals(List.of(
				"Dirichlet[4.0, 4.0, 4.0, 4.0]",
				"Dirichlet[4.0, 4.0, 4.0, 4.0]",
				"Dirichlet[4.0, 4.0, 4.0, 4.0]",
				"Dirichlet[1.0, 2.0, 3.0, 4.0]",
				"Dirichlet[1.0, 2.0, 3.0, 4.0]"), labels);
	}

	/** button with the given id in the node tree rooted at node, or null if there is none **/
	private static Button findButton(Node node, String id) {
		if (node instanceof Button button && id.equals(button.getId())) {
			return button;
		}
		if (node instanceof Parent parent) {
			for (Node child : parent.getChildrenUnmodifiable()) {
				Button button = findButton(child, id);
				if (button != null) {
					return button;
				}
			}
		}
		return null;
	}


	/**
	 * Selecting another inner distribution of a "Bounded" prior (the nested combo box of a
	 * truncated distribution) must update the bounds of the parameter straight away. The
	 * truncated distribution used to compute its bounds from the inner distribution cached
	 * when it was first needed, which the swap did not update.
	 */
	@Test
	public void testSwitchingBoundedInnerDistributionUpdatesBounds() throws Exception {
		AtomicReference<Throwable> error = new AtomicReference<>();
		AtomicReference<RealScalarParam<Real>> param = new AtomicReference<>();
		AtomicReference<TruncatedReal> bounded = new AtomicReference<>();
		List<Double> boundsBefore = new ArrayList<>();
		runOnFXThread(() -> {
			BeautiDoc doc = new BeautiDoc();
			doc.loadTemplate(doc.processTemplate(BeautiConfig.TEMPLATE_DIR + "/Standard.xml"));
			RealScalarParam<Real> x = new RealScalarParam<>(0.5, Real.INSTANCE);
			x.setID("x.c:test");
			param.set(x);
			Normal normal = new Normal();
			normal.initByName(
					"mean", new RealScalarParam<>(0.0, Real.INSTANCE),
					"sigma", new RealScalarParam<>(1.0, PositiveReal.INSTANCE));
			normal.setID("Normal.0");
			TruncatedReal truncated = new TruncatedReal();
			truncated.initByName("distribution", normal,
					"lower", new RealScalarParam<>(0.0, Real.INSTANCE),
					"upper", new RealScalarParam<>(10.0, Real.INSTANCE),
					"param", x);
			truncated.setID("XPrior.c:test");
			bounded.set(truncated);
			CompoundDistribution prior = new CompoundDistribution();
			prior.initByName("distribution", truncated);
			prior.setID("prior");
			doc.registerPlugin(x);
			doc.registerPlugin(normal);
			doc.registerPlugin(truncated);
			doc.registerPlugin(prior);
			boundsBefore.add(x.getLower());
			boundsBefore.add(x.getUpper());

			InputEditor editor = doc.getInputEditorFactory().createInputEditor(
					(Input<?>) prior.pDistributions, (BEASTInterface) prior, true,
					ExpandOption.TRUE, ButtonStatus.ALL, null, doc);
			Parent root = editor.getComponent();
			new Scene(root);
			root.applyCss();
			root.layout();
			// the outer combo box selects the Bounded prior itself, the inner one its distribution
			ComboBox<BeautiSubTemplate> inner = findComboBoxShowing(root, "Normal");
			assertNotNull(inner, "no combo box for the inner distribution of the Bounded prior");
			select(inner, "Uniform");
		}, error);
		rethrow(error);

		assertEquals(List.of(0.0, 10.0), boundsBefore);
		assertTrue(bounded.get().distributionInput.get() instanceof Uniform,
				"inner distribution was not replaced by Uniform: " + bounded.get().distributionInput.get());
		assertEquals(0.0, param.get().getLower(), 0.0);
		assertEquals(1.0, param.get().getUpper(), 0.0);
	}

	/** first combo box in the node tree rooted at node whose selected template has the given id **/
	@SuppressWarnings("unchecked")
	private static ComboBox<BeautiSubTemplate> findComboBoxShowing(Node node, String templateID) {
		List<ComboBox<?>> comboBoxes = new ArrayList<>();
		collectComboBoxes(node, comboBoxes);
		for (ComboBox<?> comboBox : comboBoxes) {
			if (comboBox.getValue() instanceof BeautiSubTemplate t && t.getID().equals(templateID)) {
				return (ComboBox<BeautiSubTemplate>) comboBox;
			}
		}
		return null;
	}


	@SuppressWarnings("unchecked")
	private static void select(ComboBox<BeautiSubTemplate> comboBox, String templateID) {
		BeautiSubTemplate template = comboBox.getItems().stream()
				.filter(t -> t.getID().equals(templateID)).findFirst().orElse(null);
		assertNotNull(template, templateID + " not offered");
		// select it, then run the combo box's action as selecting it in the GUI does
		// (setValue alone does not fire it while the combo box is not showing)
		comboBox.setValue(template);
		comboBox.getOnAction().handle(new ActionEvent(comboBox, comboBox));
	}


	/** first combo box in the node tree rooted at node that offers the template with the given id **/
	@SuppressWarnings("unchecked")
	private static ComboBox<BeautiSubTemplate> findComboBoxOffering(Node node, String templateID) {
		List<ComboBox<?>> comboBoxes = new ArrayList<>();
		collectComboBoxes(node, comboBoxes);
		for (ComboBox<?> comboBox : comboBoxes) {
			for (Object item : comboBox.getItems()) {
				if (item instanceof BeautiSubTemplate t && t.getID().equals(templateID)) {
					return (ComboBox<BeautiSubTemplate>) comboBox;
				}
			}
		}
		return null;
	}

	private static void collectComboBoxes(Node node, List<ComboBox<?>> comboBoxes) {
		if (node instanceof ComboBox<?> comboBox) {
			comboBoxes.add(comboBox);
		}
		if (node instanceof Parent parent) {
			for (Node child : parent.getChildrenUnmodifiable()) {
				collectComboBoxes(child, comboBoxes);
			}
		}
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


	private static void rethrow(AtomicReference<Throwable> error) {
		if (error.get() instanceof AssertionError e) {
			throw e;
		}
		if (error.get() != null) {
			error.get().printStackTrace();
			fail("test could not be run: " + error.get());
		}
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
