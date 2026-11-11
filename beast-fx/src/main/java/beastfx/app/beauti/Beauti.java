package beastfx.app.beauti;



import java.net.URL;

import beast.base.core.ProgramStatus;
import beast.pkgmgmt.BEASTClassLoader;
import beast.pkgmgmt.PackageManager;
import beastfx.app.inputeditor.BeautiDoc;
import beastfx.app.util.FXUtils;
import beastfx.app.util.Utils;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.concurrent.*;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.*;
import javafx.util.Duration;

/**
 * This class takes care of starting a thread to check for new packages
 * as well as managing the splash screen at startup
 * 
 * Splash screen handling adapted from https://gist.github.com/jewelsea/2305098
 */ 
public class Beauti extends Application {

    private BorderPane splashLayout;
    private Label progressText;
    private static final int SPLASH_WIDTH = 180;
    private static final int SPLASH_HEIGHT = 180;

    public static void main(String[] args) {
    	ProgramStatus.name = "BEAUti";

        // check for new packages in the background
        new Thread() {
        	public void run() {
        		try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
        		String statuString = Utils.getBeautiProperty("package.update.status");
        		if (statuString == null) {
        			statuString = PackageManager.UpdateStatus.AUTO_CHECK_AND_ASK.toString(); 
        		}
        		PackageManager.UpdateStatus updateStatus = PackageManager.UpdateStatus.valueOf(statuString);
        		PackageManager.updatePackages(updateStatus, true);
        	};
        }.start();
        
    	launch(Beauti.class, args);
    }

    @Override
    public void init() {
    	URL url = ClassLoader.getSystemClassLoader().getResource("beast/pkgmgmt/icons/beauti.png");
		ImageView splash = new ImageView(url.toExternalForm());
        
        progressText = new Label("Initialising BEAUti . . .");
        progressText.setMinWidth(SPLASH_WIDTH);
        splashLayout = new BorderPane();
        splashLayout.setCenter(splash);
        splashLayout.setBottom(progressText);
        progressText.setAlignment(Pos.CENTER);
        splashLayout.setStyle(
                "-fx-padding: 5; " +
                "-fx-background-color: lightgray; " +
                "-fx-border-width:2; " +
                "-fx-border-color: " +
                    "linear-gradient(" +
                        "to bottom, " +
                        "lightblue, " +
                        "derive(blue, 50%)" +
                    ");"
        );
        splashLayout.setEffect(new DropShadow());
    }

    @Override
    public void start(final Stage initStage) throws Exception {
        final Task<BeautiDoc> friendTask = new Task<BeautiDoc>() {
        	
            @Override
            protected BeautiDoc call() throws InterruptedException {
                FXUtils.logToSplashScreen("Initialising BEAUti . . .");
        		String [] args = getParameters().getRaw().toArray(new String[] {}); 
                BeautiDoc doc = BeautiTabPane.initialise(args);
                FXUtils.logToSplashScreen("Setting up window");
                Thread.sleep(50);
                return doc;
            }
        };

        showSplash(
                initStage,
                friendTask,
                () -> showMainStage(friendTask.valueProperty())
        );
        new Thread(friendTask).start();
    }

    private void showMainStage(
            ReadOnlyObjectProperty<BeautiDoc> friends
    ) {
    	BeautiDoc doc = friends.get();
    	Stage primaryStage = new Stage(StageStyle.DECORATED);
		String [] args = getParameters().getRaw().toArray(new String[] {});		
		BeautiTabPane.main2(args, primaryStage, doc);
        
		primaryStage.show();
        
        FXUtils.endSplashScreen();
    }

    private void showSplash(
            final Stage initStage,
            Task<?> task,
            InitCompletionHandler initCompletionHandler
    ) {
    	progressText.textProperty().bind(FXUtils.startSplashScreen());
    	
        task.stateProperty().addListener((observableValue, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED && task.getValue() == null) {
                // the arguments asked for the XML to be written (-exitaction writexml),
                // and that is done: there is no document to show
                initStage.hide();
                System.exit(0);
            } else if (newState == Worker.State.FAILED || newState == Worker.State.CANCELLED) {
                startUpFailed(initStage, task.getException());
            } else if (newState == Worker.State.SUCCEEDED) {
                initStage.toFront();
                FadeTransition fadeSplash = new FadeTransition(Duration.seconds(1.234), splashLayout);
                fadeSplash.setFromValue(1.0);
                fadeSplash.setToValue(0.0);
                fadeSplash.setOnFinished(actionEvent -> initStage.hide());
                fadeSplash.play();

                initCompletionHandler.complete();
            }
        });

        Scene splashScene = new Scene(splashLayout, Color.TRANSPARENT);
        final Rectangle2D bounds = Screen.getPrimary().getBounds();
        initStage.setScene(splashScene);
        initStage.setX(bounds.getMinX() + bounds.getWidth() / 2 - SPLASH_WIDTH / 2);
        initStage.setY(bounds.getMinY() + bounds.getHeight() / 2 - SPLASH_HEIGHT / 2);
        initStage.initStyle(StageStyle.TRANSPARENT);
        initStage.setAlwaysOnTop(true);
        initStage.show();
    }

    /**
     * BEAUti could not start, for example because a file named on the command line
     * cannot be read. Report the problem and exit with a non-zero status. Without this
     * the splash screen stays on top of the screen for ever.
     * Called on the JavaFX application thread.
     */
    private void startUpFailed(Stage initStage, Throwable problem) {
        // the splash is always on top: remove it before anything else is shown
        initStage.hide();
        String message = "BEAUti could not start";
        if (problem != null) {
            problem.printStackTrace();
            message += ": " + (problem.getMessage() != null ? problem.getMessage() : problem.toString());
        }
        System.err.println(message);
        if (!isBatchRun() && !java.awt.GraphicsEnvironment.isHeadless()) {
            // somebody is looking at the screen
            try {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.ERROR, message, ButtonType.OK);
                alert.setHeaderText("BEAUti could not start");
                alert.setResizable(true);
                alert.showAndWait();
            } catch (RuntimeException e) {
                // the message is on the console already
            }
        }
        System.exit(1);
    }

    /** @return true when the arguments ask for the XML to be written without showing BEAUti */
    private boolean isBatchRun() {
        java.util.List<String> args = getParameters().getRaw();
        int i = args.indexOf("-exitaction");
        return i >= 0 && i + 1 < args.size() && args.get(i + 1).equals("writexml");
    }

    public interface InitCompletionHandler {
        void complete();
    }
    

    @Deprecated 
    // do not use since this can cause an IllegalStateException: Toolkit not initialized
    // when called outside BEAUti. use `ProgramStatus.name.equals("BEAUti")` instead.
    static public boolean isInBeauti() {
    	return ProgramStatus.name.equals("BEAUti");
    	// return BEAUtiIntances > 0;
    }
}