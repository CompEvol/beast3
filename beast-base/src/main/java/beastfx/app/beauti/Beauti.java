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
    	URL url = BEASTClassLoader.getSystemClassLoader().getResource("beast/pkgmgmt/icons/beauti.png");
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
            if (newState == Worker.State.SUCCEEDED) {
                initStage.toFront();
                FadeTransition fadeSplash = new FadeTransition(Duration.seconds(1.234), splashLayout);
                fadeSplash.setFromValue(1.0);
                fadeSplash.setToValue(0.0);
                fadeSplash.setOnFinished(actionEvent -> initStage.hide());
                fadeSplash.play();

                initCompletionHandler.complete();
            } // todo add code to gracefully handle other task states.
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