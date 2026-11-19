package test.beastfx.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import beastfx.app.util.Alert;
import javafx.application.Platform;
import javafx.stage.Stage;

/**
 * A BEAUti error message shown with showAndWait() in a test that does not expect it is
 * answered by DialogWatchdog after a while, with its full text in the log, instead of
 * hanging the test run (#302).
 */
@ExtendWith(ApplicationExtension.class)
public class DialogWatchdogTest {

    @Start
    public void start(Stage stage) {
        // only the JavaFX toolkit is required
    }

    @BeforeEach
    void shortWait() {
        System.setProperty("beast.test.dialog.seconds", "2");
    }

    @AfterEach
    void defaultWait() {
        System.clearProperty("beast.test.dialog.seconds");
    }

    @Test
    void blockingMessageDialogIsAnsweredAndReported() throws Exception {
        CountDownLatch returned = new CountDownLatch(1);
        Platform.runLater(() -> {
            // as BEASTObjectInputEditor does when selecting a template fails
            Alert.showMessageDialog(null, "Could not select beastObject: java.lang.NullPointerException boom");
            returned.countDown();
        });
        assertTrue(returned.await(30, TimeUnit.SECONDS), "the dialog was never answered");
        List<String> answered = DialogWatchdog.takeAnswered();
        assertEquals(1, answered.size(), answered.toString());
        assertTrue(answered.get(0).contains("Could not select beastObject: java.lang.NullPointerException boom"),
                answered.get(0));
    }
}
