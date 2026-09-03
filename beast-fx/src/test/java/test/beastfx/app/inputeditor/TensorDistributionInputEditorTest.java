package test.beastfx.app.inputeditor;


import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.inference.Distribution;
import beast.base.spec.domain.*;
import beast.base.spec.inference.distribution.*;
import beast.base.spec.inference.parameter.*;
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
 * Regression tests for the "distr" combo box in the Priors panel (issue #160, and the
 * domain-inherited-from-Real / Boolean-vector bugs found alongside it):
 * <ul>
 * <li>a vector param declared with the generic, unconstrained {@code domain="Real"} --
 * e.g. Bayesian Skyline's popSizes -- must offer IID but not Dirichlet;</li>
 * <li>a Simplex-valued param (elements in [0,1] summing to 1) must offer Dirichlet but
 * not IID;</li>
 * <li>a vector param declared with a domain narrower than Real (e.g. PositiveReal) must
 * still offer IID -- IID's own template declares the unconstrained "Real" domain, so
 * the compatibility check must recognise that an unconstrained template covers every
 * narrower Real-family domain, not just an exact {@code domain="Real"} match;</li>
 * <li>a Boolean vector param must not be offered every template indiscriminately --
 * {@code getParameterDomain} previously had no case for {@code BoolVector}, so it fell
 * through to a sentinel the compatibility check also didn't recognise, and treated as
 * "compatible with anything".</li>
 * </ul>
 * Note: the editor actually behind an IID row is {@link IIDInputEditor}, not the more
 * generic {@link TensorDistributionInputEditor} (used for other TensorDistribution rows,
 * e.g. Dirichlet, that have no dedicated editor) -- both classes had the same bug.
 */
@ExtendWith(ApplicationExtension.class)
public class TensorDistributionInputEditorTest {

	@Start
	public void start(Stage stage) {
		// nothing to show, only the JavaFX toolkit is required
	}

	/** both editors cache templates in static fields, so do not leak them into other tests **/
	@BeforeEach
	@AfterEach
	public void clearTemplateCache() throws Exception {
		for (Class<?> editorClass : new Class<?>[] {TensorDistributionInputEditor.class, IIDInputEditor.class}) {
			for (String name : new String[] {"tensorTemplates", "templateInstances", "templateDomains"}) {
				Field field = editorClass.getDeclaredField(name);
				field.setAccessible(true);
				field.set(null, null);
			}
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
	public void positiveRealDomainVectorOffersIID() throws Exception {
		// a domain narrower than the generic "Real" (PositiveReal "inherits" from Real)
		// must still offer IID: IID's own template declares domain="Real", and the
		// compatibility check must accept an unconstrained template for any narrower
		// Real-family param, not just an exact domain match.
		RealVectorParam<PositiveReal> rates = new RealVectorParam<>(
				new double[] {0.5, 0.5}, PositiveReal.INSTANCE);

		Set<String> distrNames = distrNamesOfferedFor(rates, "rates.s:anolis");

		assertTrue(distrNames.contains("IID"),
				"IID should still be offered for a PositiveReal (Real-family) vector: " + distrNames);
	}


	@Test
	public void intVectorOffersNoCompatiblePriorYet() throws Exception {
		// IID's only registered template targets domain="Real"; there is no Int-domain
		// IID template yet, so an Int vector currently has no compatible entry in the
		// "distr" combo box at all. This documents the current (safe) behaviour --
		// registering an Int-domain IID template would need createComboBox()'s "restore
		// selected template" match-by-class-name loop to also disambiguate by domain
		// first, since it would then share IID's class name with the Real-domain one.
		IntVectorParam<NonNegativeInt> counts = new IntVectorParam<>(new int[] {1, 2}, NonNegativeInt.INSTANCE);
		counts.setID("counts.s:anolis");

		IntUniform intUniform = new IntUniform(
				new IntScalarParam(0, Int.INSTANCE),
				new IntScalarParam(0, Int.INSTANCE),
				new IntScalarParam(10, Int.INSTANCE));

		IID iid = new IID();
		iid.initByName("param", counts, "distr", intUniform);
		iid.setID("counts.prior");

		Set<String> distrNames = distrNamesOfferedFor(iid, counts, intUniform);

		assertTrue(distrNames.isEmpty(),
				"no Int-domain prior template is registered yet, so none should be offered for an Int vector: " + distrNames);
	}


	@Test
	public void boolVectorDoesNotOfferEveryTemplate() throws Exception {
		// getParameterDomain() had no case for BoolVector, so it fell back to a domain
		// class the compatibility check also didn't recognise, and that fell through to
		// "don't know how to handle -- accept anything", offering every real/int-typed
		// prior (including IID) for a Boolean vector. There is no Boolean-domain prior
		// template registered yet, so the fix must show none, not all of them.
		BoolVectorParam flags = new BoolVectorParam(new boolean[] {true, false});
		flags.setID("flags.s:anolis");

		Bernoulli bernoulli = new Bernoulli();
		bernoulli.initByName(
				"param", new BoolScalarParam(false),
				"p", new RealScalarParam<>(0.5, UnitInterval.INSTANCE));

		IID iid = new IID();
		iid.initByName("param", flags, "distr", bernoulli);
		iid.setID("flags.prior");

		Set<String> distrNames = distrNamesOfferedFor(iid, flags, bernoulli);

		assertTrue(distrNames.isEmpty(),
				"no prior template declares a Boolean domain yet, so none should be offered for a Boolean vector: " + distrNames);
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


	@Test
	public void markovChainDistributionRowOffersNoDistrPicker() throws Exception {
		// Bayesian Skyline wraps its popSizes (a PositiveReal vector) in a
		// MarkovChainDistribution ("Markov chained prior on population sizes"), connected
		// to the same "distribution" list the Priors panel renders -- see
		// TreePriors.xml's CoalescentBayesianSkyline subtemplate. MarkovChainDistribution
		// requires specific wiring (jeffreys/initialMean) that a domain-only compatibility
		// check can't validate, and has no registered subtemplate to switch back to, so
		// swapping it via a generic "distr" combo box (which would offer IID, since IID's
		// "Real" template covers any Real-family domain including PositiveReal) would be
		// unsafe and irreversible. MarkovChainDistributionInputEditor -- registered for
		// MarkovChainDistribution.class specifically, taking priority over the generic
		// TensorDistributionInputEditor -- fixes this by never offering a picker at all,
		// without adding any MarkovChainDistribution-specific logic to the generic editor.
		RealVectorParam<PositiveReal> popSizes = new RealVectorParam<>(
				new double[] {380.0, 380.0, 380.0, 380.0, 380.0}, PositiveReal.INSTANCE);
		popSizes.setID("bPopSizes.t:anolis");

		MarkovChainDistribution mcd = new MarkovChainDistribution();
		mcd.initByName("param", popSizes, "jeffreys", true);
		mcd.setID("MarkovChainedPopSizes.t:anolis");

		Set<String> distrNames = distrNamesOfferedFor(mcd, popSizes, null);

		assertTrue(distrNames.isEmpty(),
				"MarkovChainDistributionInputEditor should never offer a distr picker: " + distrNames);
	}


	@Test
	public void markovChainDistributionNeverOffersItselfAsAlternative() throws Exception {
		// MarkovChainDistribution must not appear as a swap-to option in some other,
		// unrelated PositiveReal vector's row either (unlike IID, which legitimately
		// applies to any such vector) -- it has no registered subtemplate at all, so it
		// can never be scanned as a candidate regardless of domain compatibility.
		RealVectorParam<PositiveReal> rates = new RealVectorParam<>(
				new double[] {0.5, 0.5}, PositiveReal.INSTANCE);

		Set<String> distrNames = distrNamesOfferedFor(rates, "rates.s:anolis");

		assertFalse(distrNames.contains("Markov Chain Distribution"),
				"MarkovChainDistribution has no registered subtemplate, so it must never be offered as an alternative for an unrelated vector: " + distrNames);
	}


	@Test
	public void intSumWiresIntoNewSpecPoissonWithoutPrior() throws Exception {
		// EBSP's "indicatorsPrior.alltrees" needs a Poisson prior on a *computed* count
		// (how many of indicators.alltrees are true), not on a StateNode directly. The
		// deprecated beast.base.spec.inference.distribution.Prior wrapper used to do this,
		// but Prior is unusable (its own "distr" input is typed to the legacy
		// ParametricDistribution hierarchy, incompatible with every spec-package
		// distribution) and must not be used. IntSum -- unlike Sum, which always exposes a
		// RealScalar -- exposes an IntScalar, so it wires directly into the spec-package
		// Poisson's "param" input (IntScalar<NonNegativeInt>) with no wrapper at all:
		// Poisson itself, being a TensorDistribution, is a Distribution and can go straight
		// into the "distribution" list.
		BoolVectorParam indicators = new BoolVectorParam(new boolean[] {true, false, true});
		indicators.setID("indicators.alltrees");

		beast.base.spec.evolution.IntSum indsSum = new beast.base.spec.evolution.IntSum();
		indsSum.initByName("arg", List.of(indicators));
		indsSum.setID("indsSum.alltrees");

		beast.base.spec.inference.distribution.Poisson poisson =
				new beast.base.spec.inference.distribution.Poisson();
		poisson.initByName("param", indsSum,
				"lambda", new RealScalarParam<>(0.69314718056, PositiveReal.INSTANCE));
		poisson.setID("indicatorsPrior.alltrees");

		assertEquals(2, indsSum.get(), "IntSum should count the 2 true indicators");
		assertTrue(Double.isFinite(poisson.calculateLogP()),
				"Poisson.calculateLogP() should compute a finite logP directly from IntSum, with no Prior wrapper");
	}


	/**
	 * Convenience overload for the common case: wraps {@code param} in a fresh IID(Normal)
	 * prior, just like {@link #distrNamesOfferedFor(TensorDistribution, BEASTInterface,
	 * BEASTInterface)} but without the caller having to build that prior by hand.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	private Set<String> distrNamesOfferedFor(RealVectorParam<?> param, String paramID) throws Exception {
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

		return distrNamesOfferedFor(iid, param, normal);
	}


	/**
	 * Builds a single-item "distribution" list (as PriorListInputEditor does for the
	 * Priors panel) around the already-initialised {@code prior} (with IDs already set
	 * on {@code prior}, {@code param} and {@code distr}), creates the "distr" combo box
	 * editor for it exactly the way BEAUti does, and returns the display names of the
	 * distributions offered in its combo box (empty if none are compatible -- BEAUti
	 * shows no combo box at all in that case, rather than an empty one). {@code distr}
	 * may be {@code null} for a TensorDistribution with no separate nested distribution
	 * object of its own (e.g. MarkovChainDistribution, unlike IID/Dirichlet's "distr"/
	 * "alpha").
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	private Set<String> distrNamesOfferedFor(TensorDistribution<?, ?> prior, BEASTInterface param, BEASTInterface distr) throws Exception {
		AtomicReference<Throwable> error = new AtomicReference<>();
		AtomicReference<Set<String>> result = new AtomicReference<>();
		runOnFXThread(() -> {
			BeautiDoc doc = new BeautiDoc();
			doc.loadTemplate(doc.processTemplate(BeautiConfig.TEMPLATE_DIR + "/Standard.xml"));

			doc.registerPlugin(param);
			if (distr != null) {
				doc.registerPlugin(distr);
			}
			doc.registerPlugin((BEASTInterface) prior);

			List<Distribution> distributions = new ArrayList<>(List.of((Distribution) prior));
			Input<List<Distribution>> listInput =
					new Input<>("distribution", "priors", distributions, Distribution.class);

			// this is what PriorListInputEditor.addPluginItem does for every row shown
			// in the Priors panel
			InputEditor editor = doc.getInputEditorFactory().createInputEditor(
					listInput, 0, (BEASTInterface) prior, false, InputEditor.ExpandOption.FALSE,
					InputEditor.ButtonStatus.NONE, null, doc);
			assertNotNull(editor, "no input editor created for the distr list item");

			ComboBox<?> comboBox = findComboBox(editor.getComponent());
			result.set(comboBox == null ? Set.of() : comboBox.getItems().stream()
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
