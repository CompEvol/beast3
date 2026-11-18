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
import beast.base.spec.domain.Int;
import beast.base.spec.domain.NonNegativeInt;
import beast.base.spec.domain.NonNegativeReal;
import beast.base.spec.domain.PositiveInt;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.domain.UnitInterval;
import beast.base.spec.evolution.tree.MRCAPrior;
import beast.base.spec.inference.distribution.Beta;
import beast.base.spec.inference.distribution.Binomial;
import beast.base.spec.inference.distribution.Geometric;
import beast.base.spec.inference.distribution.IntUniform;
import beast.base.spec.inference.distribution.LogNormal;
import beast.base.spec.inference.distribution.LogUniform;
import beast.base.spec.inference.distribution.NegativeBinomial;
import beast.base.spec.inference.distribution.OffsetInt;
import beast.base.spec.inference.distribution.Normal;
import beast.base.spec.inference.distribution.OffsetReal;
import beast.base.spec.inference.distribution.Poisson;
import beast.base.spec.inference.distribution.TruncatedInt;
import beast.base.spec.inference.distribution.TruncatedReal;
import beast.base.spec.inference.distribution.ScalarDistribution;
import beast.base.spec.inference.distribution.Uniform;
import beast.base.spec.inference.parameter.IntScalarParam;
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
	 * The choices for the inner distribution of a Bounded Real and an Offset Real prior
	 * (issue #232). Inside Bounded, any real distribution can be truncated, also one whose
	 * support is wider than the parameter's domain, and Offset can go inside; Bounded cannot,
	 * so the nesting stops. Inside Offset, no wrapper is offered.
	 */
	@Test
	public void testInnerDistributionChoicesOfWrappers() throws Exception {
		AtomicReference<Throwable> error = new AtomicReference<>();
		List<String> insideBounded = new ArrayList<>();
		List<String> insideOffset = new ArrayList<>();
		runOnFXThread(() -> {
			BeautiDoc doc = new BeautiDoc();
			doc.loadTemplate(doc.processTemplate(BeautiConfig.TEMPLATE_DIR + "/Standard.xml"));

			RealScalarParam<PositiveReal> kappa = new RealScalarParam<>(2.0, PositiveReal.INSTANCE);
			kappa.setID("kappa.s:test");
			TruncatedReal bounded = new TruncatedReal();
			bounded.initByName("param", kappa, "distribution", standardNormal(),
					"lower", new RealScalarParam<>(0.0, Real.INSTANCE),
					"upper", new RealScalarParam<>(100.0, Real.INSTANCE));
			bounded.setID("KappaPrior.s:test");
			doc.registerPlugin(kappa);
			doc.registerPlugin(bounded);
			insideBounded.addAll(choices(doc, bounded, bounded.distributionInput));

			RealScalarParam<PositiveReal> rate = new RealScalarParam<>(2.0, PositiveReal.INSTANCE);
			rate.setID("rate.s:test");
			OffsetReal offset = new OffsetReal();
			offset.initByName("param", rate, "distribution", new LogUniform());
			offset.setID("RatePrior.s:test");
			doc.registerPlugin(rate);
			doc.registerPlugin(offset);
			insideOffset.addAll(choices(doc, offset, offset.distributionInput));
		}, error);

		if (error.get() != null) {
			error.get().printStackTrace();
			fail("test could not be run: " + error.get());
		}
		assertTrue(insideBounded.contains("Normal"), "Normal can be truncated to a positive range: " + insideBounded);
		// Bounded truncates to the parameter's domain, so the Offset inside it is the one for any real
		assertTrue(insideBounded.contains("OffsetReal"), "Offset is allowed inside Bounded: " + insideBounded);
		assertFalse(insideBounded.contains("BoundedReal"), "Bounded inside Bounded: " + insideBounded);
		// a distribution with its own lower and upper would show the bounds twice
		for (String uniform : new String[] {"Uniform", "NonNegativeUniform", "LogUniform"}) {
			assertFalse(insideBounded.contains(uniform), uniform + " offered inside Bounded: " + insideBounded);
		}
		for (String intDistr : new String[] {"BoundedInt", "OffsetInt", "Poisson", "Geometric", "NegativeBinomial", "Binomial"}) {
			assertFalse(insideBounded.contains(intDistr), "integer distribution offered for a real parameter: " + insideBounded);
		}
		assertFalse(insideOffset.isEmpty(), "nothing offered inside Offset");
		assertFalse(insideOffset.contains("BoundedReal") || insideOffset.contains("OffsetReal"),
				"wrapper offered inside Offset: " + insideOffset);
	}

	/**
	 * The count distributions Geometric, NegativeBinomial and Binomial are offered for an
	 * integer parameter, here inside a Bounded Int prior on a NonNegativeInt count.
	 */
	@Test
	public void testCountDistributionsOfferedForIntParameter() throws Exception {
		AtomicReference<Throwable> error = new AtomicReference<>();
		List<String> insideBounded = new ArrayList<>();
		runOnFXThread(() -> {
			BeautiDoc doc = new BeautiDoc();
			doc.loadTemplate(doc.processTemplate(BeautiConfig.TEMPLATE_DIR + "/Standard.xml"));

			IntScalarParam<NonNegativeInt> count = new IntScalarParam<>(2, NonNegativeInt.INSTANCE);
			count.setID("count.s:test");
			TruncatedInt bounded = new TruncatedInt();
			bounded.initByName("param", count,
					"distribution", new Poisson(null, new RealScalarParam<>(2.0, NonNegativeReal.INSTANCE)),
					"lower", new IntScalarParam<>(0, Int.INSTANCE),
					"upper", new IntScalarParam<>(10, Int.INSTANCE));
			bounded.setID("CountPrior.s:test");
			doc.registerPlugin(count);
			doc.registerPlugin(bounded);
			insideBounded.addAll(choices(doc, bounded, bounded.distributionInput));

			// the editor, with its density plot, quantiles and mean, builds for each of them
			RealScalarParam<UnitInterval> p = new RealScalarParam<>(0.3, UnitInterval.INSTANCE);
			ScalarDistribution<?, ?>[] counts = {
					new Geometric(null, p),
					new NegativeBinomial(null, new RealScalarParam<>(4.0, PositiveReal.INSTANCE),
							new RealScalarParam<>(0.5, PositiveReal.INSTANCE)),
					new Binomial(null, new IntScalarParam<>(10, NonNegativeInt.INSTANCE), p)};
			for (ScalarDistribution<?, ?> inner : counts) {
				IntScalarParam<NonNegativeInt> k = new IntScalarParam<>(2, NonNegativeInt.INSTANCE);
				String name = inner.getClass().getSimpleName();
				k.setID(name + "Count.s:test");
				TruncatedInt wrapper = new TruncatedInt();
				wrapper.initByName("param", k, "distribution", inner,
						"lower", new IntScalarParam<>(0, Int.INSTANCE),
						"upper", new IntScalarParam<>(8, Int.INSTANCE));
				wrapper.setID(name + "Prior.s:test");
				doc.registerPlugin(k);
				doc.registerPlugin(wrapper);
				InputEditor editor = doc.getInputEditorFactory().createInputEditor(wrapper.distributionInput, wrapper, doc);
				assertNotNull(editor.getComponent(), "no editor for " + name);
			}
		}, error);

		if (error.get() != null) {
			error.get().printStackTrace();
			fail("test could not be run: " + error.get());
		}
		for (String countDistr : new String[] {"Poisson", "Geometric", "NegativeBinomial", "Binomial"}) {
			assertTrue(insideBounded.contains(countDistr), countDistr + " not offered for a count: " + insideBounded);
		}
		assertFalse(insideBounded.contains("Normal"), "real distribution offered for a count: " + insideBounded);
	}

	/**
	 * Of the Bounded and Offset templates, the priors panel offers the variant meant for the
	 * parameter's domain, so that the defaults fit it: e.g. Offset Int with offset 1 for a
	 * PositiveInt parameter, where Poisson + 0 would put probability on 0.
	 */
	@Test
	public void testWrapperVariantFitsParameterDomain() throws Exception {
		assertEquals(List.of("BoundedInt", "OffsetInt"), wrapperTemplatesOffered(
				new IntScalarParam<>(0, Int.INSTANCE), "IntUniform"));
		assertEquals(List.of("BoundedInt.nonNegative", "OffsetInt.nonNegative"), wrapperTemplatesOffered(
				new IntScalarParam<>(0, NonNegativeInt.INSTANCE), "Poisson"));
		assertEquals(List.of("BoundedInt.positive", "OffsetInt.positive"), wrapperTemplatesOffered(
				new IntScalarParam<>(1, PositiveInt.INSTANCE), "IntUniform"));
		assertEquals(List.of("BoundedReal", "OffsetReal"), wrapperTemplatesOffered(
				new RealScalarParam<>(0.5, Real.INSTANCE), "Normal"));
		assertEquals(List.of("BoundedReal", "OffsetReal.nonNegative"), wrapperTemplatesOffered(
				new RealScalarParam<>(0.5, PositiveReal.INSTANCE), "Normal"));
		assertEquals(List.of("BoundedReal", "OffsetReal.unitInterval"), wrapperTemplatesOffered(
				new RealScalarParam<>(0.5, UnitInterval.INSTANCE), "Normal"));
	}

	/**
	 * Selecting Offset Int for a PositiveInt parameter gives Poisson + 1, declared PositiveInt:
	 * a valid prior for the parameter's initial value 1.
	 */
	@Test
	public void testOffsetIntForPositiveIntStartsAtOne() throws Exception {
		AtomicReference<Throwable> error = new AtomicReference<>();
		AtomicReference<Object> newPrior = new AtomicReference<>();
		runOnFXThread(() -> {
			BeautiDoc doc = new BeautiDoc();
			doc.loadTemplate(doc.processTemplate(BeautiConfig.TEMPLATE_DIR + "/Standard.xml"));
			IntScalarParam<PositiveInt> count = new IntScalarParam<>(1, PositiveInt.INSTANCE);
			CompoundDistribution prior = priorOn(doc, count, "IntUniform");
			InputEditor editor = doc.getInputEditorFactory().createInputEditor(
					(Input<?>) prior.pDistributions, (BEASTInterface) prior, doc);
			@SuppressWarnings("unchecked")
			ComboBox<BeautiSubTemplate> comboBox = (ComboBox<BeautiSubTemplate>) findComboBox(editor.getComponent());
			BeautiSubTemplate offset = comboBox.getItems().stream()
					.filter(t -> t.getID().startsWith("OffsetInt")).findFirst().orElseThrow();
			comboBox.setValue(offset);
			comboBox.getOnAction().handle(new ActionEvent(comboBox, comboBox));
			newPrior.set(prior.pDistributions.get().get(0));
		}, error);

		if (error.get() != null) {
			error.get().printStackTrace();
			fail("test could not be run: " + error.get());
		}
		assertTrue(newPrior.get() instanceof OffsetInt<?>, "prior was not replaced by OffsetInt: " + newPrior.get());
		OffsetInt<?> offset = (OffsetInt<?>) newPrior.get();
		assertEquals(1, offset.getOffset());
		assertEquals(PositiveInt.INSTANCE, offset.getParameterDomain());
		assertEquals(1, offset.getLowerBoundOfParameter());
		assertTrue(Double.isFinite(offset.calculateLogP()), "logP " + offset.calculateLogP());
	}

	/**
	 * Inside an Offset, an inner distribution is offered only when, shifted by the offset, its
	 * support fits the parameter's domain and the Offset's declared domain: inside an Offset 0
	 * declared NonNegativeReal, Normal (support the whole line) is not offered, though the
	 * parameter is Real; Exponential and Gamma are.
	 */
	@Test
	public void testInnerDistributionsOfOffsetFitTheDomain() throws Exception {
		List<String> inner = innerChoicesOfOffsetReal();
		for (String d : new String[] {"Normal", "Laplace", "Cauchy"}) {
			assertFalse(inner.contains(d), d + " offered inside an Offset declared NonNegativeReal: " + inner);
		}
		for (String d : new String[] {"LogNormal", "Exponential", "Gamma"}) {
			assertTrue(inner.contains(d), d + " not offered inside an Offset declared NonNegativeReal: " + inner);
		}
	}

	/**
	 * For a PositiveInt parameter, the count distributions (support from 0) are offered inside
	 * Offset 1 but not inside Offset 0, where IntUniform [1, 10] still is.
	 */
	@Test
	public void testInnerIntDistributionsOfOffsetFitTheDomain() throws Exception {
		List<String> atZero = innerChoicesOfOffsetInt(0, new IntUniform());
		List<String> atOne = innerChoicesOfOffsetInt(1, new IntUniform());
		for (String fromZero : new String[] {"Poisson", "Geometric", "NegativeBinomial", "Binomial"}) {
			assertFalse(atZero.contains(fromZero), fromZero + " offered inside Offset 0 for a PositiveInt parameter: " + atZero);
			assertTrue(atOne.contains(fromZero), fromZero + " not offered inside Offset 1 for a PositiveInt parameter: " + atOne);
		}
		assertTrue(atZero.contains("IntUniform"), "IntUniform not offered inside Offset 0: " + atZero);
	}

	/**
	 * When no distribution fits inside an Offset, e.g. Offset -1 on a PositiveInt parameter around
	 * a distribution BEAUti has no template for, they are offered anyway, so the box stays and
	 * the panel can report the prior as invalid.
	 */
	@Test
	public void testInnerDistributionsOfOffsetOfferedWhenNoneFits() throws Exception {
		List<String> inner = innerChoicesOfOffsetInt(-1, new IntUniformWithoutTemplate());
		assertTrue(inner.contains("Poisson") && inner.contains("IntUniform"), "nothing offered: " + inner);
	}

	/** an integer distribution without a BEAUti template */
	public static class IntUniformWithoutTemplate extends IntUniform {
	}

	/**
	 * inner choices of an Offset with the given offset around the given IntUniform, set to [1, 5],
	 * on a PositiveInt parameter
	 */
	private List<String> innerChoicesOfOffsetInt(int offset, IntUniform innerDistr) throws Exception {
		AtomicReference<Throwable> error = new AtomicReference<>();
		List<String> inner = new ArrayList<>();
		runOnFXThread(() -> {
			BeautiDoc doc = new BeautiDoc();
			doc.loadTemplate(doc.processTemplate(BeautiConfig.TEMPLATE_DIR + "/Standard.xml"));
			IntScalarParam<PositiveInt> count = new IntScalarParam<>(3, PositiveInt.INSTANCE);
			count.setID("count.s:test");
			innerDistr.initByName("lower", new IntScalarParam<>(1, Int.INSTANCE), "upper", new IntScalarParam<>(5, Int.INSTANCE));
			OffsetInt<PositiveInt> wrapper = new OffsetInt<>();
			wrapper.initByName("param", count, "distribution", innerDistr,
					"offset", new IntScalarParam<>(offset, Int.INSTANCE));
			wrapper.setID("CountPrior.s:test");
			doc.registerPlugin(count);
			doc.registerPlugin(wrapper);
			inner.addAll(choices(doc, wrapper, wrapper.distributionInput));
		}, error);
		if (error.get() != null) {
			error.get().printStackTrace();
			fail("test could not be run: " + error.get());
		}
		return inner;
	}

	/** inner choices of an Offset 0 declared NonNegativeReal around LogNormal, on a Real parameter */
	private List<String> innerChoicesOfOffsetReal() throws Exception {
		AtomicReference<Throwable> error = new AtomicReference<>();
		List<String> inner = new ArrayList<>();
		runOnFXThread(() -> {
			BeautiDoc doc = new BeautiDoc();
			doc.loadTemplate(doc.processTemplate(BeautiConfig.TEMPLATE_DIR + "/Standard.xml"));
			RealScalarParam<Real> x = new RealScalarParam<>(1.0, Real.INSTANCE);
			x.setID("x.s:test");
			LogNormal logNormal = new LogNormal();
			logNormal.initByName("M", new RealScalarParam<>(0.0, Real.INSTANCE), "S", new RealScalarParam<>(1.0, PositiveReal.INSTANCE));
			OffsetReal<NonNegativeReal> wrapper = new OffsetReal<>();
			wrapper.initByName("param", x, "distribution", logNormal, "domain", NonNegativeReal.INSTANCE,
					"offset", new RealScalarParam<>(0.0, Real.INSTANCE));
			wrapper.setID("XPrior.s:test");
			doc.registerPlugin(x);
			doc.registerPlugin(wrapper);
			inner.addAll(choices(doc, wrapper, wrapper.distributionInput));
		}, error);
		if (error.get() != null) {
			error.get().printStackTrace();
			fail("test could not be run: " + error.get());
		}
		return inner;
	}

	/** @return the IDs of the Bounded and Offset templates offered in the priors panel for the parameter */
	private List<String> wrapperTemplatesOffered(BEASTInterface param, String priorTemplate) throws Exception {
		AtomicReference<Throwable> error = new AtomicReference<>();
		List<String> offered = new ArrayList<>();
		runOnFXThread(() -> {
			BeautiDoc doc = new BeautiDoc();
			doc.loadTemplate(doc.processTemplate(BeautiConfig.TEMPLATE_DIR + "/Standard.xml"));
			CompoundDistribution prior = priorOn(doc, param, priorTemplate);
			InputEditor editor = doc.getInputEditorFactory().createInputEditor(
					(Input<?>) prior.pDistributions, (BEASTInterface) prior, doc);
			ComboBox<?> comboBox = findComboBox(editor.getComponent());
			assertNotNull(comboBox, "no combo box to select a distribution from");
			for (Object item : comboBox.getItems()) {
				String id = ((BeautiSubTemplate) item).getID();
				if (id.startsWith("Bounded") || id.startsWith("Offset")) {
					offered.add(id);
				}
			}
		}, error);
		if (error.get() != null) {
			error.get().printStackTrace();
			fail("test could not be run: " + error.get());
		}
		return offered;
	}

	/** a priors panel list holding a prior on the parameter, of the given simple kind */
	private static CompoundDistribution priorOn(BeautiDoc doc, BEASTInterface param, String kind) {
		param.setID("x.s:test");
		ScalarDistribution<?, ?> d;
		switch (kind) {
		case "IntUniform" -> {
			IntUniform u = new IntUniform();
			u.initByName("param", param, "lower", new IntScalarParam<>(-5, Int.INSTANCE), "upper", new IntScalarParam<>(5, Int.INSTANCE));
			d = u;
		}
		case "Poisson" -> {
			Poisson p = new Poisson();
			p.initByName("param", param, "lambda", new RealScalarParam<>(2.0, NonNegativeReal.INSTANCE));
			d = p;
		}
		default -> {
			Normal n = new Normal();
			n.initByName("param", param, "mean", new RealScalarParam<>(0.0, Real.INSTANCE),
					"sigma", new RealScalarParam<>(1.0, PositiveReal.INSTANCE));
			d = n;
		}
		}
		d.setID("XPrior.s:test");
		CompoundDistribution prior = new CompoundDistribution();
		prior.initByName("distribution", d);
		prior.setID("prior");
		doc.registerPlugin(param);
		doc.registerPlugin(d);
		doc.registerPlugin(prior);
		return prior;
	}

	/**
	 * A Normal bounded to [-1, 1] is not a valid prior for a PositiveReal parameter: it gives
	 * probability to values the parameter cannot take. The priors panel reports it as invalid;
	 * bounded to [0, 1] it is valid.
	 */
	@Test
	public void testBoundedPriorOutsideDomainIsReported() throws Exception {
		assertEquals(List.of(InputEditor.ValidationStatus.IS_INVALID), boundedPriorStatus(-1.0));
		assertFalse(boundedPriorStatus(0.0).contains(InputEditor.ValidationStatus.IS_INVALID));
	}

	/** validation statuses of the editor of a Normal prior bounded to [lower, 1] on a positive parameter */
	private List<InputEditor.ValidationStatus> boundedPriorStatus(double lower) throws Exception {
		AtomicReference<Throwable> error = new AtomicReference<>();
		List<InputEditor.ValidationStatus> statuses = new ArrayList<>();
		runOnFXThread(() -> {
			BeautiDoc doc = new BeautiDoc();
			doc.loadTemplate(doc.processTemplate(BeautiConfig.TEMPLATE_DIR + "/Standard.xml"));

			RealScalarParam<PositiveReal> kappa = new RealScalarParam<>(0.5, PositiveReal.INSTANCE);
			kappa.setID("kappa.s:test");
			TruncatedReal bounded = new TruncatedReal();
			bounded.initByName("param", kappa, "distribution", standardNormal(),
					"lower", new RealScalarParam<>(lower, Real.INSTANCE),
					"upper", new RealScalarParam<>(1.0, Real.INSTANCE));
			bounded.setID("KappaPrior.s:test");
			CompoundDistribution prior = new CompoundDistribution();
			prior.initByName("distribution", bounded);
			prior.setID("prior");
			doc.registerPlugin(kappa);
			doc.registerPlugin(bounded);
			doc.registerPlugin(prior);

			InputEditor editor = doc.getInputEditorFactory().createInputEditor(
					(Input<?>) prior.pDistributions, (BEASTInterface) prior, doc);
			ScalarDistributionInputEditor priorEditor = findEditor(editor.getComponent(), ScalarDistributionInputEditor.class);
			assertNotNull(priorEditor, "no editor for the prior");
			priorEditor.addValidationListener(new InputEditor.Base(doc) {
				@Override
				public Class<?> type() {
					return Object.class;
				}
				@Override
				public void startValidating(InputEditor.ValidationStatus state) {
					statuses.add(state);
				}
			});
			priorEditor.validateInput();
		}, error);
		if (error.get() != null) {
			error.get().printStackTrace();
			fail("test could not be run: " + error.get());
		}
		return statuses;
	}

	/**
	 * Bounded with Offset inside it: the expanded editor shows the bounds, the offset, and
	 * the inputs of the distribution inside Offset (here the mean and sigma of a Normal).
	 */
	@Test
	public void testBoundedOffsetShowsInnermostInputs() throws Exception {
		AtomicReference<Throwable> error = new AtomicReference<>();
		List<String> missing = new ArrayList<>();
		List<String> initialButtons = new ArrayList<>();
		runOnFXThread(() -> {
			BeautiDoc doc = new BeautiDoc();
			doc.loadTemplate(doc.processTemplate(BeautiConfig.TEMPLATE_DIR + "/Standard.xml"));

			RealScalarParam<PositiveReal> kappa = new RealScalarParam<>(2.0, PositiveReal.INSTANCE);
			kappa.setID("kappa.s:test");
			OffsetReal offset = new OffsetReal();
			// as the OffsetReal template makes it: with a placeholder param of its own, value 0
			offset.initByName("distribution", standardNormal(), "offset", new RealScalarParam<>(1.0, Real.INSTANCE),
					"param", new RealScalarParam<>(0.0, Real.INSTANCE));
			TruncatedReal bounded = new TruncatedReal();
			bounded.initByName("param", kappa, "distribution", offset,
					"lower", new RealScalarParam<>(0.0, Real.INSTANCE),
					"upper", new RealScalarParam<>(100.0, Real.INSTANCE));
			bounded.setID("KappaPrior.s:test");
			CompoundDistribution prior = new CompoundDistribution();
			prior.initByName("distribution", bounded);
			prior.setID("prior");
			doc.registerPlugin(kappa);
			doc.registerPlugin(bounded);
			doc.registerPlugin(prior);

			InputEditor editor = doc.getInputEditorFactory().createInputEditor(
					(Input<?>) prior.pDistributions, (BEASTInterface) prior, true,
					ExpandOption.TRUE, ButtonStatus.ALL, null, doc);
			for (String id : new String[] {"lower", "upper", "offset", "mean", "sigma"}) {
				if (findTextField(editor.getComponent(), id) == null) {
					missing.add(id);
				}
			}
			List<String> texts = new ArrayList<>();
			collectButtonTexts(editor.getComponent(), texts);
			for (String text : texts) {
				if (text != null && text.startsWith("initial")) {
					initialButtons.add(text);
				}
			}
		}, error);

		if (error.get() != null) {
			error.get().printStackTrace();
			fail("test could not be run: " + error.get());
		}
		assertTrue(missing.isEmpty(), "no text field for " + missing);
		// the initial value is that of kappa; the distribution inside Offset has none of its own
		assertEquals(List.of("initial = 2.0"), initialButtons);
	}

	private static Normal standardNormal() {
		Normal normal = new Normal();
		normal.initByName("mean", new RealScalarParam<>(0.0, Real.INSTANCE),
				"sigma", new RealScalarParam<>(1.0, PositiveReal.INSTANCE));
		return normal;
	}

	/** the template IDs offered in the combo box of the editor for a wrapper's inner distribution */
	private static List<String> choices(BeautiDoc doc, BEASTInterface wrapper, Input<?> distributionInput) throws Exception {
		InputEditor editor = doc.getInputEditorFactory().createInputEditor(distributionInput, wrapper, doc);
		@SuppressWarnings("unchecked")
		ComboBox<BeautiSubTemplate> comboBox = (ComboBox<BeautiSubTemplate>) findComboBox(editor.getComponent());
		assertNotNull(comboBox, "no combo box for the inner distribution of " + wrapper.getID());
		List<String> ids = new ArrayList<>();
		for (BeautiSubTemplate t : comboBox.getItems()) {
			ids.add(t.getID());
		}
		return ids;
	}


	/**
	 * Selecting a prior that does not allow the current initial value (5.0 under
	 * Uniform[0,1]) must be reported in the panel, not only logged: the initial value
	 * button turns red and explains the problem in its tooltip, and the editor reports
	 * itself invalid to its validation listeners.
	 */
	@Test
	public void testInitialValueOutsideNewPriorIsReported() throws Exception {
		AtomicReference<Throwable> error = new AtomicReference<>();
		AtomicReference<Normal> oldPrior = new AtomicReference<>();
		AtomicReference<RealScalarParam<Real>> growthRate = new AtomicReference<>();
		AtomicReference<Button> button = new AtomicReference<>();
		List<InputEditor.ValidationStatus> statuses = new ArrayList<>();
		runOnFXThread(() -> {
			BeautiDoc doc = new BeautiDoc();
			doc.loadTemplate(doc.processTemplate(BeautiConfig.TEMPLATE_DIR + "/Standard.xml"));

			RealScalarParam<Real> rate = new RealScalarParam<>(5.0, Real.INSTANCE);
			rate.setID("growthRate.t:test");
			growthRate.set(rate);
			Normal normal = new Normal();
			normal.initByName("param", rate,
					"mean", new RealScalarParam<>(0.0, Real.INSTANCE),
					"sigma", new RealScalarParam<>(1.0, PositiveReal.INSTANCE));
			normal.setID("GrowthRatePrior.t:test");
			oldPrior.set(normal);
			CompoundDistribution prior = new CompoundDistribution();
			prior.initByName("distribution", normal);
			prior.setID("prior");
			doc.registerPlugin(rate);
			doc.registerPlugin(normal);
			doc.registerPlugin(prior);

			InputEditor editor = doc.getInputEditorFactory().createInputEditor(
					(Input<?>) prior.pDistributions, (BEASTInterface) prior, doc);
			@SuppressWarnings("unchecked")
			ComboBox<BeautiSubTemplate> comboBox = (ComboBox<BeautiSubTemplate>) findComboBox(editor.getComponent());
			assertNotNull(comboBox, "no combo box to select a distribution from");
			BeautiSubTemplate uniform = comboBox.getItems().stream()
					.filter(t -> t.getID().equals("Uniform")).findFirst().orElse(null);
			assertNotNull(uniform, "Uniform not offered for a Real parameter");
			comboBox.setValue(uniform);
			comboBox.getOnAction().handle(new ActionEvent(comboBox, comboBox));

			// BEAUti rebuilds the panel after selecting a prior; build the editor of the
			// new prior the same way, listening to its validation status
			InputEditor rebuilt = doc.getInputEditorFactory().createInputEditor(
					(Input<?>) prior.pDistributions, (BEASTInterface) prior, doc);
			ScalarDistributionInputEditor priorEditor = findEditor(rebuilt.getComponent(), ScalarDistributionInputEditor.class);
			assertNotNull(priorEditor, "no editor for the new prior");
			priorEditor.addValidationListener(new InputEditor.Base(doc) {
				@Override
				public Class<?> type() {
					return Object.class;
				}
				@Override
				public void startValidating(InputEditor.ValidationStatus state) {
					statuses.add(state);
				}
			});
			priorEditor.validateInput();
			button.set(findButtonStartingWith(rebuilt.getComponent(), "initial = "));
		}, error);

		if (error.get() != null) {
			error.get().printStackTrace();
			fail("test could not be run: " + error.get());
		}
		// the prior is replaced, and the old one no longer restricts the parameter
		assertFalse(growthRate.get().getOutputs().contains(oldPrior.get()), "old prior still attached to the parameter");
		assertFalse(growthRate.get().isValid(), "5.0 should be outside the range of Uniform[0,1]");

		assertNotNull(button.get(), "no initial value button");
		assertTrue(button.get().getStyle().contains("red"), "initial value button not marked: " + button.get().getStyle());
		String tooltip = button.get().getTooltip().getText();
		assertTrue(tooltip.contains("5.0") && tooltip.contains("outside the allowed range [0.0,1.0]"), "tooltip: " + tooltip);
		assertTrue(statuses.contains(InputEditor.ValidationStatus.IS_INVALID), "validation statuses: " + statuses);
	}


	/** first node of the given class in the node tree rooted at node, or null if there is none **/
	private static <T> T findEditor(Node node, Class<T> c) {
		if (c.isInstance(node)) {
			return c.cast(node);
		}
		if (node instanceof Parent parent) {
			for (Node child : parent.getChildrenUnmodifiable()) {
				T found = findEditor(child, c);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	}


	/** first button whose text starts with prefix in the node tree rooted at node **/
	private static Button findButtonStartingWith(Node node, String prefix) {
		if (node instanceof Button b && b.getText() != null && b.getText().startsWith(prefix)) {
			return b;
		}
		if (node instanceof Parent parent) {
			for (Node child : parent.getChildrenUnmodifiable()) {
				Button found = findButtonStartingWith(child, prefix);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
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
