package test.beastfx.app.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import beastfx.app.util.Alert;
import javafx.application.Platform;
import javafx.scene.control.ButtonType;
import javafx.scene.Scene;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Alert's dialogs: no HTML tags shown, closing one without a choice is not an error and never
 * reads as Yes or No, and a dialog is centred over the window it is shown for.
 */
@ExtendWith(ApplicationExtension.class)
public class AlertTest {

    @Start
    public void start(Stage stage) {
        // only the JavaFX toolkit is required
    }

    /** the open dialog's pane, waiting for it to appear */
    private static Window openDialog() throws Exception {
        for (int i = 0; i < 100; i++) {
            CompletableFuture<Window> found = new CompletableFuture<>();
            Platform.runLater(() -> found.complete(Window.getWindows().stream()
                    .filter(w -> w.isShowing() && w.getScene() != null && w.getScene().getRoot() instanceof DialogPane)
                    .findFirst().orElse(null)));
            Window w = found.get(5, TimeUnit.SECONDS);
            if (w != null) {
                return w;
            }
            Thread.sleep(50);
        }
        return null;
    }

    @Test
    void confirmDialogShowsNoHtmlAndClosingIsClosedOption() throws Exception {
        AtomicReference<ButtonType> result = new AtomicReference<>();
        CountDownLatch returned = new CountDownLatch(1);
        Platform.runLater(() -> {
            result.set(Alert.showConfirmDialog(null, "<html>first line<br>second line</html>", "Header",
                    Alert.YES_NO_OPTION));
            returned.countDown();
        });
        Window dialog = openDialog();
        assertNotNull(dialog, "no dialog shown");
        CompletableFuture<String> text = new CompletableFuture<>();
        Platform.runLater(() -> text.complete(((DialogPane) dialog.getScene().getRoot()).getContentText()));
        String content = text.get(5, TimeUnit.SECONDS);
        assertFalse(content.contains("<"), content);
        assertTrue(content.contains("first line\nsecond line"), content);

        // closed without choosing a button, e.g. by the window manager
        Platform.runLater(dialog::hide);
        assertTrue(returned.await(10, TimeUnit.SECONDS), "showConfirmDialog did not return");
        assertEquals(Alert.CLOSED_OPTION, result.get());
    }

    /**
     * A Yes/No/Cancel dialog closed without a choice is Cancel: BEAUti's "Save before closing?"
     * must not take it for No and quit without saving
     */
    @Test
    void closingYesNoCancelIsCancel() throws Exception {
        AtomicReference<ButtonType> result = new AtomicReference<>();
        CountDownLatch returned = new CountDownLatch(1);
        Platform.runLater(() -> {
            result.set(Alert.showConfirmDialog(null, "Save?", "Save before closing?", Alert.YES_NO_CANCEL_OPTION));
            returned.countDown();
        });
        Window dialog = openDialog();
        assertNotNull(dialog, "no dialog shown");
        Platform.runLater(dialog::hide);
        assertTrue(returned.await(10, TimeUnit.SECONDS), "showConfirmDialog did not return");
        assertEquals(Alert.CANCEL_OPTION, result.get());
    }

    /** a dialog for a node is centred over that node's window, wherever the window is */
    @Test
    void dialogIsCentredOverItsWindow() throws Exception {
        AtomicReference<Stage> owner = new AtomicReference<>();
        StackPane root = new StackPane();
        CountDownLatch shown = new CountDownLatch(1);
        Platform.runLater(() -> {
            Stage stage = new Stage();
            stage.setScene(new Scene(root, 800, 600));
            stage.setX(300);
            stage.setY(200);
            stage.show();
            owner.set(stage);
            shown.countDown();
        });
        assertTrue(shown.await(10, TimeUnit.SECONDS));
        CountDownLatch returned = new CountDownLatch(1);
        Platform.runLater(() -> {
            Alert.showConfirmDialog(root, "Centred?", "Header", Alert.YES_NO_OPTION);
            returned.countDown();
        });
        Window dialog = openDialog();
        assertNotNull(dialog, "no dialog shown");
        CompletableFuture<double[]> centres = new CompletableFuture<>();
        Platform.runLater(() -> {
            Stage o = owner.get();
            centres.complete(new double[] {o.getX() + o.getWidth() / 2, dialog.getX() + dialog.getWidth() / 2});
        });
        double[] c = centres.get(5, TimeUnit.SECONDS);
        Platform.runLater(() -> {
            dialog.hide();
            owner.get().hide();
        });
        assertTrue(returned.await(10, TimeUnit.SECONDS), "showConfirmDialog did not return");
        assertEquals(c[0], c[1], 2, "dialog centre x " + c[1] + ", window centre x " + c[0]);
    }
}
