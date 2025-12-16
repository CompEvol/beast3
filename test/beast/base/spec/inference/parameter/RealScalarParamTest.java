package beast.base.spec.inference.parameter;

import beast.base.parser.XMLProducer;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.distribution.Normal;
import beast.base.spec.inference.distribution.TruncatedReal;
import test.beast.BEASTTestCase;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class RealScalarParamTest {

	@Test
	void testXML() {

		RealScalarParam param = new RealScalarParam(1.0, PositiveReal.INSTANCE);
		param.initAndValidate();

		XMLProducer xmlProducer = new XMLProducer();
		String xml = xmlProducer.toXML(param);

		System.out.println(xml);

		assertFalse(xml.contains("id=\"PositiveReal\""));
		assertTrue(xml.contains("domain=\"PositiveReal\""), "XML contains 'domain=\"PositiveReal\".");

	}

	@Test
	void testBounds() {
		RealScalarParam param = new RealScalarParam(1.0, PositiveReal.INSTANCE);
		param.setID("param");

		Normal normal = new Normal(param, new RealScalarParam(0, Real.INSTANCE),
				new RealScalarParam(1, PositiveReal.INSTANCE));
		normal.setID("normal");
		assertEquals(0.0, param.getLower(), BEASTTestCase.PRECISION);
		assertEquals(Double.POSITIVE_INFINITY, param.getUpper(), BEASTTestCase.PRECISION);
	}

	@Test
	void testBounds2() {
		RealScalarParam param = new RealScalarParam(1.0, PositiveReal.INSTANCE);
		param.setID("param");

		// base dist has no param
		Normal normal = new Normal(null, new RealScalarParam(0, Real.INSTANCE),
				new RealScalarParam(1, PositiveReal.INSTANCE));
		normal.setID("normal");

		// now wrap the Normal in a TruncatedRealDistribution to specify narrower bounds
		TruncatedReal truncated = new TruncatedReal(normal, 1.0, 2.0);
		truncated.setID("truncated");
		truncated.initByName("param", param);

		assertEquals(1.0, truncated.getLower(), BEASTTestCase.PRECISION);
		assertEquals(2.0, truncated.getUpper(), BEASTTestCase.PRECISION);

		assertEquals(1.0, param.getLower(), BEASTTestCase.PRECISION);
		assertEquals(2.0, param.getUpper(), BEASTTestCase.PRECISION);
	}

	/*
	 * - Purpose: ensure initAndValidate() (via constructor) rejects values outside
	 * the domain. 
	 * - Assertion: constructing with a Negative value for PositiveReal
	 * should throw IllegalArgumentException.
	 */
	@Test
	void testInitAndValidateRejectsInvalidValue() {
		assertThrows(IllegalArgumentException.class, () -> {
			new RealScalarParam(-1.0, PositiveReal.INSTANCE);
		});
	}

	/*
	 * - Purpose: setting an invalid value after construction should throw. 
	 * - Assertion: set(...) on a PositiveReal param with a negative value throws.
	 */
	@Test
	void testSetRejectsInvalidValue() {
		RealScalarParam param = new RealScalarParam(1.0, PositiveReal.INSTANCE);
		assertThrows(IllegalArgumentException.class, () -> param.set(-0.5));
	}

	/*
	 * - Purpose: store() and restore() round-trip the value (and setEverythingDirty
	 * behaviour). 
	 * - Assertions: after store(), changing value then restore() resets
	 * to stored value.
	 */
	@Test
	void testStoreAndRestore() {
		RealScalarParam param = new RealScalarParam(2.0, Real.INSTANCE);
		param.store();
		param.set(5.0);
		assertEquals(5.0, param.get(), 1e-12);
		param.restore();
		assertEquals(2.0, param.get(), 1e-12);
	}

	/*
	 * - Purpose: verify scale(...) and scaleOne(...) multiply the stored value and
	 * return expected counts. 
	 * - Assertions: scale returns 1 and multiplies value;
	 * scaleOne multiplies value too.
	 */
	@Test
	void testScaleAndScaleOne() {
		RealScalarParam param = new RealScalarParam(3.0, Real.INSTANCE);
		assertEquals(1, param.scale(2.0));
		assertEquals(6.0, param.get(), 1e-12);
		param.scaleOne(0, 0.5);
		assertEquals(3.0, param.get(), 1e-12);
	}

	/*
	 * - Purpose: copy() produces a separate object; modifying copy doesn't change
	 * original. 
	 * - Assertions: copied.get() equals original.get() initially;
	 * modifying copy's value does not affect original.
	 */
	@Test
	void testCopyIsIndependent() {
		RealScalarParam original = new RealScalarParam(4.0, Real.INSTANCE);
		RealScalarParam copy = (RealScalarParam) original.copy();
		assertNotNull(copy);
		assertEquals(4.0, copy.get(), 1e-12);
		copy.set(2.0);
		assertEquals(4.0, original.get(), 1e-12);
		assertEquals(2.0, copy.get(), 1e-12);
	}

	/*
	 * - Purpose: check the three assign methods transfer expected fields. 
	 * - Assertions: 
	 * 	- assignFromFragile copies only the value. 
	 * 	- assignFrom copies ID/domain/value/storedValue. 
	 *  - assignTo copies ID/index/domain/value.
	 */
	@Test
	void testAssignMethods() {
		RealScalarParam src = new RealScalarParam(7.0, PositiveReal.INSTANCE);
		src.setID("src");
		src.store(); // storedValue = 7.0
		src.set(8.0); // change current value

		RealScalarParam fragileTarget = new RealScalarParam(1.0, PositiveReal.INSTANCE);
		fragileTarget.assignFromFragile(src);
		assertNotEquals("src", fragileTarget.getID());
		assertEquals(8.0, fragileTarget.get(), 1e-12);

		RealScalarParam fullTarget = new RealScalarParam(1.0, Real.INSTANCE);
		fullTarget.assignFrom(src);
		assertEquals("src", fullTarget.getID());
		assertEquals(src.getDomain().getClass(), fullTarget.getDomain().getClass());
		assertEquals(8.0, fullTarget.get(), 1e-12);
		// storedValue copied as well
		fullTarget.restore();
		assertEquals(src.storedValue, fullTarget.get(), 1e-12); // storedValue is package-protected; if inaccessible,
																// assert storedValue via behavior

		RealScalarParam toTarget = new RealScalarParam(1.0, Real.INSTANCE);
		src.assignTo(toTarget);
		assertEquals("src", toTarget.getID());
		assertEquals(src.getDomain().getClass(), toTarget.getDomain().getClass());
		assertEquals(src.get(), toTarget.get(), 1e-12);
	}
//   (If storedValue is not accessible in test package, verify by storing, changing, restoring sequence to confirm storedValue was copied.)

	/*
	 * - Purpose: verify the fromXML(shape, values...) helper throws on non-null
	 * shape, and sets value when shape null. 
	 * - Assertions: IllegalArgumentException when shape != null; valid parse when shape null.
	 */
	@Test
	void testFromXMLStringVariants() {
		RealScalarParam param = new RealScalarParam(1.0, Real.INSTANCE);
		assertThrows(IllegalArgumentException.class, () -> param.fromXML("shape", "1.0"));
		param.fromXML(null, "2.5");
		assertEquals(2.5, param.get(), 1e-12);
	}

	/*
	 * - Purpose: ensure init() prints ID, log() prints value, and close() does not
	 * throw. 
	 * - Assertions: capture PrintStream output and inspect content.
	 */
	@Test
	void testLogInitClose() {
		RealScalarParam param = new RealScalarParam(3.14, Real.INSTANCE);
		param.setID("myParam");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos);
		param.init(ps);
		param.log(0, ps);
		param.close(ps);
		String out = baos.toString();
		assertTrue(out.contains("myParam"));
		assertTrue(out.contains("3.14"));
	}

	/*
	 * - Purpose: getDomain() returns domainTypeInput default before init. 
	 * - Assertions: new RealScalarParam() (no init) returns Real.INSTANCE (the
	 * default domain) from getDomain().
	 */
	@Test
	void testGetDomainBeforeInit() {
		RealScalarParam param = new RealScalarParam();
		assertEquals(Real.class, param.getDomain().getClass());
	}

	/*
	 * parameter utils formatting 
	 * - Purpose: ensure toString() delegates and returns a reasonable representation
	 * (non-null/non-empty).
	 */
	@Test
	void testToStringNotEmpty() {
		RealScalarParam param = new RealScalarParam(1.23, Real.INSTANCE);
		String s = param.toString();
		assertNotNull(s);
		assertFalse(s.isEmpty());
		assertTrue(s.contains("1.23") || s.matches(".*1\\.23.*")); // tolerant check
	}

}