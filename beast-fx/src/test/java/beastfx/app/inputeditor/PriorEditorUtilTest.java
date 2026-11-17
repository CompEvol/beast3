package beastfx.app.inputeditor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import beast.base.spec.domain.Int;
import beast.base.spec.domain.NonNegativeReal;
import beast.base.spec.domain.PositiveInt;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
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
}
