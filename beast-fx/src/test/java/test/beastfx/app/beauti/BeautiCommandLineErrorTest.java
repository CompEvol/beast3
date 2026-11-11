package test.beastfx.app.beauti;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testfx.framework.junit5.Start;

import beastfx.app.beauti.BeautiTabPane;
import javafx.stage.Stage;

/**
 * BEAUti started with command line arguments that cannot be processed (issue #264).
 * <p>
 * At start up the arguments are processed by {@link BeautiTabPane#initialise(String[])} on a
 * background thread, while the splash screen shows. A failure used to open a dialog there, which
 * is not possible outside the JavaFX application thread, so the error was lost and the splash
 * stayed up for ever. Now the failure is an exception, which {@code Beauti} reports before it
 * exits with a non-zero status.
 * <p>
 * The tests call {@code initialise} from the test thread, which is not the JavaFX application
 * thread either.
 */
public class BeautiCommandLineErrorTest extends BeautiBase {

    private static final Duration LIMIT = Duration.ofSeconds(60);

    @TempDir
    Path dir;

    @Start
    public void start(Stage stage) {
        // nothing to show: the JavaFX toolkit only needs to be running
    }

    private String out() {
        return dir.resolve("out.xml").toString();
    }

    private Throwable initialiseFails(String... args) {
        return assertTimeoutPreemptively(LIMIT,
                () -> assertThrows(RuntimeException.class, () -> BeautiTabPane.initialise(args)),
                "BEAUti hangs on arguments it cannot process");
    }

    @Test
    public void templateThatDoesNotExistIsReported() {
        // "Standard" is not a file: it has to be the path of Standard.xml
        Throwable e = initialiseFails("-template", "Standard", "-nex", NEXUS_DIR + "dna.nex",
                "-out", out(), "-exitaction", "writexml");
        assertTrue(e.getMessage() != null && e.getMessage().contains("Standard"), "message: " + e.getMessage());
        assertFalse(new File(out()).exists());
    }

    @Test
    public void alignmentThatDoesNotExistIsReported() {
        initialiseFails("-template", TEMPLATE_DIR + "/Standard.xml", "-nex", dir.resolve("no-such-file.nex").toString(),
                "-out", out(), "-exitaction", "writexml");
        assertFalse(new File(out()).exists());
    }

    @Test
    public void argumentWithoutValueIsReported() {
        Throwable e = initialiseFails("-nex", NEXUS_DIR + "dna.nex", "-template");
        assertTrue(e instanceof IllegalArgumentException, e.toString());
        assertTrue(e.getMessage().contains("-template"), "message: " + e.getMessage());
    }

    @Test
    public void unknownArgumentIsReported() {
        Throwable e = initialiseFails("-nosuchargument");
        assertTrue(e instanceof IllegalArgumentException, e.toString());
        assertTrue(e.getMessage().contains("-nosuchargument"), "message: " + e.getMessage());
    }

    @Test
    public void unknownExitActionIsReported() {
        Throwable e = initialiseFails("-exitaction", "nosuchaction");
        assertTrue(e instanceof IllegalArgumentException, e.toString());
    }

    @Test
    public void exitActionMergeIsReportedAsNotImplemented() {
        Throwable e = initialiseFails("-template", TEMPLATE_DIR + "/Standard.xml", "-nex", NEXUS_DIR + "dna.nex",
                "-exitaction", "merge");
        assertTrue(e.getMessage() != null && e.getMessage().contains("merge"), "message: " + e.getMessage());
    }

    @Test
    public void everyArgumentWithAValueReportsWhenItIsMissing() {
        for (String arg : new String[] {"-xml", "-template", "-nex", "-fasta_nucleotide", "-xmldata", "-exitaction", "-out"}) {
            Throwable e = initialiseFails(arg);
            assertTrue(e instanceof IllegalArgumentException, arg + ": " + e);
            assertTrue(e.getMessage().contains(arg + " needs a value"), arg + ": " + e.getMessage());
        }
    }

    @Test
    public void xmlNotMadeByBeautiDoesNotNeedADialog() {
        // an example XML has no beautitemplate attribute. BEAUti asks for confirmation in a dialog,
        // which cannot be shown on this thread: it must warn and go on, not fail on the dialog
        String xml = new File(NEXUS_DIR).getParentFile().toPath().resolve("testHKY.xml").toString();
        assertTrue(new File(xml).exists(), xml);
        try {
            assertTimeoutPreemptively(LIMIT, () -> BeautiTabPane.initialise(new String[] {"-xml", xml, "-exitaction", "usexml"}));
        } catch (RuntimeException e) {
            // loading a hand-written XML may fail for other reasons, but not because of the dialog
            String text = e + " " + e.getCause();
            assertFalse(text.contains("FX application thread"), text);
        }
    }

    /** -xml with an XML that BEAUti wrote, as at start up: the document is loaded (issue #271) */
    @Test
    public void xmlWrittenByBeautiCanBeOpenedFromTheCommandLine() throws Exception {
        assertNull(assertTimeoutPreemptively(LIMIT, () -> BeautiTabPane.initialise(new String[] {
                "-template", TEMPLATE_DIR + "/Standard.xml", "-nex", NEXUS_DIR + "dna.nex",
                "-out", out(), "-exitaction", "writexml"})));
        assertTrue(new File(out()).length() > 0, "no XML written");
        beastfx.app.inputeditor.BeautiDoc doc = assertTimeoutPreemptively(LIMIT,
                () -> BeautiTabPane.initialise(new String[] {"-xml", out(), "-exitaction", "usexml"}));
        assertTrue(doc != null && doc.alignments.size() == 1, "the alignment of the XML is not loaded");
        assertTrue(doc.pluginmap.containsKey("Tree.t:dna"), "the tree of the XML is not loaded");
    }

    @Test
    public void validArgumentsWriteTheXML() {
        // null means: the XML was written, there is no document to show
        assertNull(assertTimeoutPreemptively(LIMIT, () -> BeautiTabPane.initialise(new String[] {
                "-template", TEMPLATE_DIR + "/Standard.xml", "-nex", NEXUS_DIR + "dna.nex",
                "-out", out(), "-exitaction", "writexml"})));
        File f = new File(out());
        assertTrue(f.exists() && f.length() > 0, "no XML written to " + f);
    }
}
