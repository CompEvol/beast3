package beastfx.app.inputeditor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import beast.base.spec.domain.Int;
import beast.base.spec.domain.NonNegativeInt;
import beast.base.spec.domain.NonNegativeReal;
import beast.base.spec.domain.PositiveInt;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.domain.UnitInterval;
import beast.base.spec.inference.distribution.Geometric;
import beast.base.spec.inference.distribution.Normal;
import beast.base.spec.inference.distribution.Poisson;
import beast.base.spec.inference.distribution.TruncatedInt;
import beast.base.spec.inference.distribution.TruncatedReal;
import beast.base.spec.inference.parameter.IntScalarParam;
import beast.base.spec.inference.parameter.BoolVectorParam;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.inference.parameter.RealVectorParam;

/**
 * Tests for {@link PriorEditorUtil#valueToString}, which gives the value of a parameter in the
 * invalid initial value message, and {@link PriorEditorUtil#supportOutsideDomainMessage}. It is
 * in this package because the methods are package private.
 */
public class PriorEditorUtilTest {

	@Test
	void vectorHasNoTrailingSpace() {
		RealVectorParam<Real> freqs = new RealVectorParam<>(new double[]{0.3, 0.3, 0.3, 0.3}, Real.INSTANCE);
		freqs.setID("freqParameter.s:anolis");
		// toString() (ParameterUtils.paramToString) is left as it is, trailing space included
		assertTrue(freqs.toString().endsWith(" "), "toString() is not changed: «" + freqs + "»");
		assertEquals("freqParameter.s:anolis{4}: 0.3 0.3 0.3 0.3", PriorEditorUtil.valueToString(freqs, 5));
		assertEquals("The value of freqParameter.s:anolis{4}: 0.3 0.3 0.3 0.3 is not valid.",
				"The value of " + PriorEditorUtil.valueToString(freqs, 5) + " is not valid.");
	}

	@Test
	void longVectorIsShortened() {
		double[] values = new double[10];
		Arrays.fill(values, 1.0);
		RealVectorParam<Real> rates = new RealVectorParam<>(values, Real.INSTANCE);
		rates.setID("rates");
		// the shape still gives the full size
		assertEquals("rates{10}: 1.0 1.0 1.0 1.0 1.0 ...", PriorEditorUtil.valueToString(rates, 5));
		// as many values as shown, or fewer: all of them, no "..."
		assertEquals("rates{10}: 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0 1.0", PriorEditorUtil.valueToString(rates, 10));
		assertEquals(rates.toString().trim(), PriorEditorUtil.valueToString(rates, 20));
	}

	@Test
	void boolVectorIsShortened() {
		BoolVectorParam selected = new BoolVectorParam(new boolean[]{true, false, true});
		selected.setID("isSelected");
		assertEquals("isSelected{3}: true false ...", PriorEditorUtil.valueToString(selected, 2));
	}

	@Test
	void scalarIsUnchanged() {
		RealScalarParam<Real> mu = new RealScalarParam<>(5.0, Real.INSTANCE);
		mu.setID("mu");
		assertEquals("mu: 5.0", PriorEditorUtil.valueToString(mu, 1));
	}

	/**
	 * BEAUti's check that a prior's support fits the parameter's domain compares integer
	 * domains with their inclusive bounds, as the runtime does: Bounded(Poisson, [0, 10]) on
	 * a PositiveInt parameter is reported, [1, 10] is not.
	 */
	@Test
	void boundedPoissonOnPositiveInt() {
		for (int lower : new int[] {0, 1}) {
			IntScalarParam<PositiveInt> k = new IntScalarParam<>(3, PositiveInt.INSTANCE);
			k.setID("k");
			Poisson poisson = new Poisson(null, new RealScalarParam<>(2.0, NonNegativeReal.INSTANCE));
			TruncatedInt bounded = new TruncatedInt();
			bounded.initByName("distribution", poisson, "lower", new IntScalarParam<>(lower, Int.INSTANCE),
					"upper", new IntScalarParam<>(10, Int.INSTANCE), "param", k);
			String message = PriorEditorUtil.supportOutsideDomainMessage(bounded);
			if (lower == 0) {
				assertNotNull(message, "a prior from 0 on PositiveInt must be reported");
				assertTrue(message.contains("at least 1"), message);
			} else {
				assertNull(message);
			}
		}
	}

	/** real domains keep their bound as it is: a prior from 0 fits PositiveReal, one from -1 does not */
	@Test
	void boundedNormalOnPositiveReal() {
		for (double lower : new double[] {0.0, -1.0}) {
			RealScalarParam<PositiveReal> kappa = new RealScalarParam<>(2.0, PositiveReal.INSTANCE);
			kappa.setID("kappa");
			Normal normal = new Normal(null, new RealScalarParam<>(0.0, Real.INSTANCE),
					new RealScalarParam<>(1.0, PositiveReal.INSTANCE));
			TruncatedReal bounded = new TruncatedReal();
			bounded.initByName("distribution", normal, "lower", new RealScalarParam<>(lower, Real.INSTANCE),
					"upper", new RealScalarParam<>(10.0, Real.INSTANCE), "param", kappa);
			String message = PriorEditorUtil.supportOutsideDomainMessage(bounded);
			if (lower < 0) {
				assertNotNull(message);
			} else {
				assertNull(message, message);
			}
		}
	}

	/** a template for the given class, enough for closestDomainPerClass (not initialised) */
	private static BeautiSubTemplate template(String id, String className) {
		BeautiSubTemplate t = new BeautiSubTemplate();
		t.setID(id);
		t.classInput.setValue(className, t);
		return t;
	}

	private static List<String> offered(Class<?> paramDomain, List<BeautiSubTemplate> templates, List<Class<?>> domains) {
		return PriorEditorUtil.closestDomainPerClass(paramDomain, templates, domains).stream().map(BeautiSubTemplate::getID).toList();
	}

	/**
	 * Of several templates for the same class, the one whose domain is closest to the
	 * parameter's is offered: the same domain, then a narrower one, then a broader one.
	 * Other classes are not affected.
	 */
	/**
	 * Geometric with p = 0 gives every count probability 0: the initial value is within the
	 * prior's range, but BEAUti reports it, as MCMC cannot start from it.
	 */
	@Test
	void geometricWithPZeroIsReported() {
		RealScalarParam<UnitInterval> p = new RealScalarParam<>(0.3, UnitInterval.INSTANCE);
		IntScalarParam<NonNegativeInt> k = new IntScalarParam<>(2, NonNegativeInt.INSTANCE);
		k.setID("k");
		Geometric geometric = new Geometric(k, p);
		assertNull(PriorEditorUtil.invalidInitialValueMessage(geometric));
		p.set(0.0);
		String message = PriorEditorUtil.invalidInitialValueMessage(geometric);
		assertNotNull(message, "p = 0 not reported");
		assertTrue(message.contains("probability 0") && message.contains("k"), message);
	}

	@Test
	void closestDomainPerClass() {
		BeautiSubTemplate iid = template("IID", "IID");
		BeautiSubTemplate iidNonNegative = template("IID.nonNegative", "IID");
		BeautiSubTemplate iidUnit = template("IID.unitInterval", "IID");
		BeautiSubTemplate dirichlet = template("Dirichlet", "Dirichlet");
		List<BeautiSubTemplate> all = List.of(iid, dirichlet, iidNonNegative, iidUnit);
		List<Class<?>> domains = List.of(Real.class, UnitInterval.class, NonNegativeReal.class, UnitInterval.class);

		assertEquals(List.of("IID", "Dirichlet"), offered(Real.class, all, domains));
		assertEquals(List.of("Dirichlet", "IID.nonNegative"), offered(NonNegativeReal.class, all, domains));
		// broader: NonNegativeReal is one step from PositiveReal, Real two
		assertEquals(List.of("IID.nonNegative"), offered(PositiveReal.class,
				List.of(iid, iidNonNegative), List.of(Real.class, NonNegativeReal.class)));
		assertEquals(List.of("Dirichlet", "IID.unitInterval"), offered(UnitInterval.class, all, domains));
		// narrower before broader: for a NonNegativeReal parameter, UnitInterval before Real
		assertEquals(List.of("IID.unitInterval"), offered(NonNegativeReal.class,
				List.of(iid, iidUnit), List.of(Real.class, UnitInterval.class)));
		// equally close templates are all offered
		BeautiSubTemplate iid2 = template("IID.normal", "IID");
		assertEquals(List.of("IID", "IID.normal"), offered(Real.class,
				List.of(iid, iid2), List.of(Real.class, Real.class)));
	}
}
