package beastfx.app.inputeditor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import beast.base.spec.domain.Real;
import beast.base.spec.inference.parameter.BoolVectorParam;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.inference.parameter.RealVectorParam;

/**
 * Tests for {@link PriorEditorUtil#valueToString}, which gives the value of a parameter in the
 * invalid initial value message. It is in this package because the method is package private.
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
}
