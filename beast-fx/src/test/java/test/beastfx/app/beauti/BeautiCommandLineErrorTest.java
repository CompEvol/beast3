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
    public void validArgumentsWriteTheXML() {
        // null means: the XML was written, there is no document to show
        assertNull(assertTimeoutPreemptively(LIMIT, () -> BeautiTabPane.initialise(new String[] {
                "-template", TEMPLATE_DIR + "/Standard.xml", "-nex", NEXUS_DIR + "dna.nex",
                "-out", out(), "-exitaction", "writexml"})));
        File f = new File(out());
        assertTrue(f.exists() && f.length() > 0, "no XML written to " + f);
    }
}
