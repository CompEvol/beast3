package test.beastfx.app.inputeditor;


import beast.base.core.Input;
import beast.base.inference.Distribution;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.distribution.IID;
import beast.base.spec.inference.distribution.Normal;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.inference.parameter.RealVectorParam;
import beast.base.spec.inference.parameter.SimplexParam;
import beastfx.app.inputeditor.*;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Regression test for issue #160: a vector-valued parameter declared with an
 * unconstrained {@code domain="Real"} -- e.g. Bayesian Skyline's population sizes,
 * {@code <parameter spec="RealVectorParam" domain="Real" .../>} -- must not offer
 * Dirichlet in the Priors panel's "distr" combo box, since Dirichlet only applies to
 * a Simplex (elements constrained to [0,1] and summing to 1), not an arbitrary real
 * vector. IID legitimately applies to a Real-domain vector, so it must still be
 * offered there; conversely it must not be offered for a Simplex-valued parameter.
 */
@ExtendWith(ApplicationExtension.class)
public class TensorDistributionInputEditorTest {

	@Start
	public void start(Stage stage) {
		// nothing to show, only the JavaFX toolkit is required
	}

	/** the editor caches templates in static fields, so do not leak them into other tests **/
	@BeforeEach
	@AfterEach
	public void clearTemplateCache() throws Exception {
		for (String name : new String[] {"tensorTemplates", "templateInstances", "templateDomains"}) {
			Field field = TensorDistributionInputEditor.class.getDeclaredField(name);
			field.setAccessible(true);
			field.set(null, null);
		}
	}


	@Test
	public void realDomainVectorDoesNotOfferDirichletButOffersIID() throws Exception {
		// Bug (#160): a vector param declared with the generic, unconstrained "Real"
		// domain -- BEAUti's default when no domain is given, e.g. Bayesian Skyline's
		// popSizes: <parameter spec="RealVectorParam" domain="Real" .../> -- used to
		// have Dirichlet offered as a compatible prior in the "distr" combo box.
		// Dirichlet actually requires a Simplex (elements in [0,1] that sum to 1),
		// which "Real" does not guarantee, so offering it there would let a user build
		// an invalid model.
		RealVectorParam<Real> popSizes = new RealVectorParam<>(
				new double[] {380.0, 380.0, 380.0, 380.0, 380.0}, Real.INSTANCE);

		Set<String> distrNames = distrNamesOfferedFor(popSizes, "bPopSizes.t:anolis");

		// Dirichlet must not appear: it needs a Simplex-valued param, not a plain Real vector.
		assertFalse(distrNames.contains("Dirichlet"),
				"Dirichlet requires a Simplex, so it must not be offered for a domain=\"Real\" vector: " + distrNames);
		// IID must still appear: it legitimately applies a scalar distribution
		// independently to each element of any Real-domain vector, so the fix must not
		// over-correct and hide it too.
		assertTrue(distrNames.contains("IID"),
				"IID legitimately applies to a domain=\"Real\" vector, so it should still be offered: " + distrNames);
	}


	@Test
	public void simplexOffersDirichletButNotIID() throws Exception {
		// Reverse/positive-control case: a true Simplex-valued param (elements in [0,1]
		// that sum to 1) is exactly what Dirichlet requires, so it should be offered
		// here. IID's default template targets a plain "Real" domain, not a Simplex, so
		// it should be excluded -- this proves the fix actually distinguishes the two
		// cases both ways, rather than just suppressing Dirichlet unconditionally.
		// dimension 4, like DNA base frequencies (A, C, G, T)
		SimplexParam freqs = new SimplexParam(new double[] {0.3, 0.2, 0.25, 0.25});

		Set<String> distrNames = distrNamesOfferedFor(freqs, "bFreqs.s:anolis");

		assertTrue(distrNames.contains("Dirichlet"),
				"Dirichlet should be offered for a Simplex-valued vector: " + distrNames);
		assertFalse(distrNames.contains("IID"),
				"IID's default template targets domain=\"Real\", not Simplex, so it should not be offered: " + distrNames);
	}


	/**
	 * Builds a single-item "distribution" list (as PriorListInputEditor does for the
	 * Priors panel), wraps {@code param} in an IID prior so there is a concrete
	 * TensorDistribution to attach the "distr" combo box editor to, creates that
	 * editor exactly the way BEAUti does, and returns the display names of the
	 * distributions offered in its combo box.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	private Set<String> distrNamesOfferedFor(RealVectorParam<?> param, String paramID) throws Exception {
		AtomicReference<Throwable> error = new AtomicReference<>();
		AtomicReference<Set<String>> result = new AtomicReference<>();
		runOnFXThread(() -> {
			BeautiDoc doc = new BeautiDoc();
			doc.loadTemplate(doc.processTemplate(BeautiConfig.TEMPLATE_DIR + "/Standard.xml"));

			param.setID(paramID);

			Normal normal = new Normal();
			normal.initByName(
					"mean", new RealScalarParam<>(0.0, Real.INSTANCE),
					"sigma", new RealScalarParam<>(1.0, PositiveReal.INSTANCE));

			// wrap param in a valid prior so there is a TensorDistribution to hang the
			// "distr" editor off, just like a row already in the Priors panel
			IID iid = new IID();
			iid.initByName("param", param, "distr", normal);
			iid.setID(paramID + ".prior");

			doc.registerPlugin(param);
			doc.registerPlugin(normal);
			doc.registerPlugin(iid);

			List<Distribution> distributions = new ArrayList<>(List.of((Distribution) iid));
			Input<List<Distribution>> listInput =
					new Input<>("distribution", "priors", distributions, Distribution.class);

			// this is what PriorListInputEditor.addPluginItem does for every row shown
			// in the Priors panel
			InputEditor editor = doc.getInputEditorFactory().createInputEditor(
					listInput, 0, iid, false, InputEditor.ExpandOption.FALSE,
					InputEditor.ButtonStatus.NONE, null, doc);
			assertNotNull(editor, "no input editor created for the distr list item");

			ComboBox<?> comboBox = findComboBox(editor.getComponent());
			assertNotNull(comboBox, "no combo box to select a distribution from");

			result.set(comboBox.getItems().stream()
					.map(item -> ((BeautiSubTemplate) item).toString())
					.collect(Collectors.toSet()));
		}, error);

		if (error.get() != null) {
			error.get().printStackTrace();
			fail("test could not be run: " + error.get());
		}
		return result.get();
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
