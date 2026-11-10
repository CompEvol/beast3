package test.beastfx.app.beauti;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

import beastfx.app.inputeditor.BeautiDoc;

/**
 * BEAUti 2.8 can't open analyses made with BEAST 2.7 or earlier. It must refuse them with a
 * message the user can act on, not the parser's type-mismatch errors.
 */
public class Beast2XMLRejectedTest {

    @Test
    public void versionCheck() {
        assertTrue(BeautiDoc.isBeforeVersion2_8("2.7"));
        assertTrue(BeautiDoc.isBeforeVersion2_8("2.0"));
        assertTrue(BeautiDoc.isBeforeVersion2_8(" 2.6 "));
        assertTrue(BeautiDoc.isBeforeVersion2_8("1.10"));
        assertFalse(BeautiDoc.isBeforeVersion2_8("2.8"));
        assertFalse(BeautiDoc.isBeforeVersion2_8("2.10"));
        assertFalse(BeautiDoc.isBeforeVersion2_8("3.0"));
        assertFalse(BeautiDoc.isBeforeVersion2_8(null));
        assertFalse(BeautiDoc.isBeforeVersion2_8("unknown"));
    }

    @Test
    public void beauti27FileIsRejectedWithAClearMessage() throws Exception {
        // the top element as BEAUti 2.7.9 writes it
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>"
                + "<beast beautitemplate='Standard' beautistatus='' namespace=\"beast.core:beast.base.inference\""
                + " required=\"BEAST.base v2.7.9\" version=\"2.7\">"
                + "<map name=\"Gamma\" >beast.base.inference.distribution.Gamma</map>"
                + "</beast>";
        File file = File.createTempFile("beauti27", ".xml");
        file.deleteOnExit();
        Files.writeString(file.toPath(), xml);

        BeautiDoc doc = new BeautiDoc();
        BeautiDoc.Beast2XMLException e = assertThrows(BeautiDoc.Beast2XMLException.class, () -> doc.loadXML(file));
        assertTrue(e.getMessage().contains("made with BEAST 2.7"), e.getMessage());
        assertTrue(e.getMessage().contains("BEAUti 2.8 can only open analyses made with BEAST 2.8 or later"), e.getMessage());
    }
}
