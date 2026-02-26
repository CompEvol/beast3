package beast.base.spec.inference.parameter;

import beast.base.spec.domain.Int;
import beast.base.spec.domain.PositiveInt;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream; 
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;
public class IntScalarParamTest {

	@Test
	void testInitAndValidateRejectsInvalidValue() {
	    assertThrows(IllegalArgumentException.class, () -> {
	        new IntScalarParam(-1, PositiveInt.INSTANCE);
	    });
	}

	@Test
	void testSetRejectsInvalidValue() {
	    IntScalarParam param = new IntScalarParam(1, PositiveInt.INSTANCE);
	    assertThrows(IllegalArgumentException.class, () -> param.set(-5));
	}

	@Test
	void testStoreAndRestore() {
	    IntScalarParam param = new IntScalarParam(2, Int.INSTANCE);
	    param.store();
	    param.set(5);
	    assertEquals(5, param.get());
	    param.restore();
	    assertEquals(2, param.get());
	}

	@Test
	void testCopyIsIndependent() {
	    IntScalarParam original = new IntScalarParam(4, Int.INSTANCE);
	    IntScalarParam copy = (IntScalarParam) original.copy();
	    assertNotNull(copy);
	    assertEquals(4, copy.get());
	    copy.set(2);
	    assertEquals(4, original.get());
	    assertEquals(2, copy.get());
	}

	@Test
	void testAssignMethods() {
	    IntScalarParam src = new IntScalarParam(7, PositiveInt.INSTANCE);
	    src.setID("src");
	    // ensure storedValue
	    src.store();   // storedValue = 7
	    src.set(8);    // current value = 8

	    // assignFromFragile should copy current value only
	    IntScalarParam fragileTarget = new IntScalarParam(1, PositiveInt.INSTANCE);
	    fragileTarget.assignFromFragile(src);
	    assertEquals(8, fragileTarget.get());

	    // assignFrom should copy ID, domain, current value and storedValue
	    IntScalarParam fullTarget = new IntScalarParam(1, Int.INSTANCE);
	    fullTarget.assignFrom(src);
	    assertEquals("src", fullTarget.getID());
	    assertEquals(src.getDomain().getClass(), fullTarget.getDomain().getClass());
	    assertEquals(8, fullTarget.get());
	    // restoring fullTarget should revert to storedValue copied from source (7)
	    fullTarget.restore();
	    assertEquals(src.storedValue, fullTarget.get());

	    // assignTo should copy ID/domain/value to the target passed in
	    IntScalarParam toTarget = new IntScalarParam(1, Int.INSTANCE);
	    src.assignTo(toTarget);
	    assertEquals("src", toTarget.getID());
	    assertEquals(src.getDomain().getClass(), toTarget.getDomain().getClass());
	    assertEquals(src.get(), toTarget.get());
	}

	@Test
	void testFromXMLStringVariants() {
	    IntScalarParam param = new IntScalarParam(1, Int.INSTANCE);
	    assertThrows(IllegalArgumentException.class, () -> param.fromXML("shape", "1"));
	    param.fromXML(null, "42");
	    assertEquals(42, param.get());
	}

	@Test
	void testLogInitCloseOutput() {
	    IntScalarParam param = new IntScalarParam(314, Int.INSTANCE);
	    param.setID("myIntParam");
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    PrintStream ps = new PrintStream(baos);
	    param.init(ps);
	    param.log(0, ps);
	    param.close(ps);
	    String out = baos.toString();
	    assertTrue(out.contains("myIntParam"));
	    assertTrue(out.contains("314"));
	}

	@Test
	void testGetDomainBeforeInit() {
	    IntScalarParam param = new IntScalarParam();
	    // default domain input is Int.INSTANCE
	    assertEquals(Int.class, param.getDomain().getClass());
	}

	@Test
	void testToStringNotEmpty() {
	    IntScalarParam param = new IntScalarParam(123, Int.INSTANCE);
	    String s = param.toString();
	    assertNotNull(s);
	    assertFalse(s.isEmpty());
	    assertTrue(s.contains("123") || s.matches(".*123.*"));
	}
}
