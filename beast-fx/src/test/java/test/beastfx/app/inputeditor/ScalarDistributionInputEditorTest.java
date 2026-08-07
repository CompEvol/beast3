package test.beastfx.app.inputeditor;


import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import beast.base.inference.Distribution;
import beast.base.inference.State;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.domain.UnitInterval;
import beast.base.spec.evolution.tree.MRCAPrior;
import beast.base.spec.inference.distribution.Beta;
import beast.base.spec.inference.distribution.ScalarDistribution;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.type.RealScalar;
import beastfx.app.inputeditor.BeautiConfig;
import beastfx.app.inputeditor.BeautiDoc;
import beastfx.app.inputeditor.InputEditor;
import beastfx.app.inputeditor.ScalarDistributionInputEditor;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
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
