package test.beastfx.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Labeled;
import javafx.scene.control.TextInputControl;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Keeps beast-fx tests from hanging on, or being disturbed by, dialogs nobody answers (#302).
 * <p>
 * The tests run JavaFX headless, so a modal dialog that a test does not expect, e.g. an
 * error message shown with {@code showAndWait()}, would wait forever. While a test runs,
 * a modal dialog open for longer than {@link #blockedSeconds()} (20 s; system property
 * {@code beast.test.dialog.seconds}) is logged with its text,
 * answered (with its cancel or close button if it has one) and the test fails. Dialogs a
 * test answers itself are open for much less time and are not touched.
 * <p>
 * After each test, dialogs still open are logged and closed, so they cannot block or fail
 * later tests in the same JVM.
 * <p>
 * Registered for every test in {@code META-INF/services}; production code is unchanged.
 */
public class DialogWatchdog implements BeforeEachCallback, AfterEachCallback {

    /** how long a dialog may stay open before it counts as blocking the test */
    static int blockedSeconds() {
        return Integer.getInteger("beast.test.dialog.seconds", 20);
    }

    private static final ScheduledExecutorService TIMER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "DialogWatchdog");
        t.setDaemon(true);
        return t;
    });

    /** when each open dialog was first seen; touched on the FX thread only */
    private static final Map<Window, Long> FIRST_SEEN = new WeakHashMap<>();

    /** dialogs this watchdog had to answer during the current test */
    private static final List<String> ANSWERED = Collections.synchronizedList(new ArrayList<>());

    private ScheduledFuture<?> check;

    @Override
    public void beforeEach(ExtensionContext context) {
        ANSWERED.clear();
        // JavaFX may not be up yet: extensions found by autodetection run before the test
        // class's own, e.g. TestFX's ApplicationExtension, which starts it
        check = TIMER.scheduleWithFixedDelay(() -> {
            try {
                Platform.runLater(DialogWatchdog::answerBlockedDialogs);
            } catch (IllegalStateException e) {
                // JavaFX is not running (yet)
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        if (check != null) {
            check.cancel(false);
            check = null;
        }
        if (fxRunning()) {
            List<String> left = new ArrayList<>();
            CountDownLatch done = new CountDownLatch(1);
            Platform.runLater(() -> {
                try {
                    for (Window w : dialogs()) {
                        left.add(describe(w));
                        answer(w);
                    }
                    FIRST_SEEN.clear();
                } finally {
                    done.countDown();
                }
            });
            done.await(10, TimeUnit.SECONDS);
            for (String d : left) {
                System.err.println("DialogWatchdog: closed a dialog left open by " + context.getDisplayName() + ": " + d);
            }
        }
        if (!ANSWERED.isEmpty()) {
            List<String> answered = new ArrayList<>(ANSWERED);
            ANSWERED.clear();
            throw new AssertionError("A dialog nobody answered blocked the test for " + blockedSeconds()
                    + " s, and was closed:\n  " + String.join("\n  ", answered));
        }
    }

    /** the dialogs answered so far in this test, which are then forgotten; for DialogWatchdogTest */
    static List<String> takeAnswered() {
        synchronized (ANSWERED) {
            List<String> answered = new ArrayList<>(ANSWERED);
            ANSWERED.clear();
            return answered;
        }
    }

    /** whether the JavaFX toolkit is up; tests without JavaFX are left alone */
    private static boolean fxRunning() {
        try {
            Platform.runLater(() -> { });
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    private static void answerBlockedDialogs() {
        long now = System.nanoTime();
        for (Window w : dialogs()) {
            long first = FIRST_SEEN.computeIfAbsent(w, x -> now);
            if (now - first > TimeUnit.SECONDS.toNanos(blockedSeconds())) {
                String d = describe(w);
                System.err.println("DialogWatchdog: answering a dialog nobody answered: " + d);
                ANSWERED.add(d);
                answer(w);
                FIRST_SEEN.remove(w);
            }
        }
    }

    /** the open modal dialogs */
    private static List<Window> dialogs() {
        List<Window> dialogs = new ArrayList<>();
        for (Window w : new ArrayList<>(Window.getWindows())) {
            if (w.isShowing() && w instanceof Stage s && s.getModality() != Modality.NONE) {
                dialogs.add(w);
            }
        }
        return dialogs;
    }

    /** presses the dialog's cancel/close button, else its first button, else hides it */
    private static void answer(Window w) {
        if (w.getScene() != null && w.getScene().getRoot() instanceof DialogPane pane && !pane.getButtonTypes().isEmpty()) {
            ButtonType choice = pane.getButtonTypes().get(0);
            for (ButtonType b : pane.getButtonTypes()) {
                if (b.getButtonData() == ButtonData.CANCEL_CLOSE || b.getButtonData() == ButtonData.NO) {
                    choice = b;
                    break;
                }
            }
            Node button = pane.lookupButton(choice);
            if (button instanceof javafx.scene.control.Button bt) {
                bt.fire();
            }
        }
        if (w.isShowing()) {
            w.hide();
        }
    }

    /** the title, header and all text in a dialog, for the log */
    static String describe(Window w) {
        StringBuilder b = new StringBuilder();
        if (w instanceof Stage s && s.getTitle() != null) {
            b.append('[').append(s.getTitle()).append("] ");
        }
        if (w.getScene() != null) {
            Parent root = w.getScene().getRoot();
            if (root instanceof DialogPane pane) {
                if (pane.getHeaderText() != null) {
                    b.append(pane.getHeaderText()).append(" | ");
                }
                if (pane.getContentText() != null && !pane.getContentText().isEmpty()) {
                    b.append(pane.getContentText()).append(" | ");
                }
                if (pane.getContent() != null) {
                    appendText(pane.getContent(), b);
                }
            } else {
                appendText(root, b);
            }
        }
        return b.toString().replace('\n', ' ').trim();
    }

    private static void appendText(Node node, StringBuilder b) {
        if (node instanceof TextInputControl t) {
            b.append(t.getText()).append(" | ");
        } else if (node instanceof Labeled l && l.getText() != null && !l.getText().isEmpty()) {
            b.append(l.getText()).append(" | ");
        }
        if (node instanceof Parent p) {
            for (Node child : p.getChildrenUnmodifiable()) {
                appendText(child, b);
            }
        }
    }
}
