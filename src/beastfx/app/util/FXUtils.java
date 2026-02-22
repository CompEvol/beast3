package beastfx.app.util;




import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;

import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;


import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.pkgmgmt.BEASTClassLoader;
import beastfx.app.inputeditor.BeautiPanelConfig;

public class FXUtils {


    public static File getLoadFile(String message) {
        return getLoadFile(message, null, null, (String[]) null);
    }

    public static File getSaveFile(String message) {
        return getSaveFile(message, null, null, (String[]) null);
    }

    public static File getLoadFile(String message, File defaultFileOrDir, String description, final String... extensions) {
        File[] files = getFile(message, true, defaultFileOrDir, false, description, extensions);
        if (files == null) {
            return null;
        } else {
            return files[0];
        }
    }

    public static File getSaveFile(String message, File defaultFileOrDir, String description, final String... extensions) {
        File[] files = getFile(message, false, defaultFileOrDir, false, description, extensions);
        if (files == null) {
            return null;
        } else {
            return files[0];
        }
    }

    public static File[] getLoadFiles(String message, File defaultFileOrDir, String description, final String... extensions) {
        return getFile(message, true, defaultFileOrDir, true, description, extensions);
    }

    public static File[] getSaveFiles(String message, File defaultFileOrDir, String description, final String... extensions) {
        return getFile(message, false, defaultFileOrDir, true, description, extensions);
    }

    public static File[] getFile(String message, boolean isLoadNotSave, File defaultFileOrDir, boolean allowMultipleSelection, String description, final String... extensions) {

    	FileChooser fileChooser = new FileChooser();
    	    	
    	if (defaultFileOrDir != null) {
    		if (defaultFileOrDir.isDirectory()) {
    			if (defaultFileOrDir.exists()) {
    				fileChooser.setInitialDirectory(defaultFileOrDir);
    			}
    		} else if (defaultFileOrDir.getParentFile().isDirectory()) {
    			if (defaultFileOrDir.getParentFile().exists()) {
    				fileChooser.setInitialDirectory(defaultFileOrDir.getParentFile());
    			}
    			fileChooser.setInitialFileName(defaultFileOrDir.getName());
    		}
    	}

		if (extensions != null) {
			for (String extension : extensions) {
				if (extension.equals("")) {
					extension = "*";
				}
				fileChooser.getExtensionFilters().add(
						new FileChooser.ExtensionFilter(extension, "*."+extension)
				);
			}
		}

    	fileChooser.setTitle(message);
    	
    	if (isLoadNotSave) {
    		List<File> files = fileChooser.showOpenMultipleDialog(null);
    		if (files == null) {
    			return new File[]{};
    		}
            return files.toArray(new File[]{});
    	} else {
    		File file = fileChooser.showSaveDialog(null);
    		return new File[] {file};
    	}
    }

	public static ImageView getIcon(String iconLocation) {
		return getIcon("BEAST.app", iconLocation);
	}
	
	public static ImageView getIcon(String packageName, String iconLocation) {
		try {
	    	URL url = BEASTClassLoader.getResource(packageName, iconLocation);
	    	if (url == null) {
	        	if (iconLocation != null && iconLocation.length() > 1 && iconLocation.startsWith("/")) {
	        		return getIcon(packageName, iconLocation.substring(1));
	        	}
	    	}
			// string = FXUtils.class.getResource(string).toExternalForm();
			ImageView img = new ImageView(url.toExternalForm());
			return img;
		} catch (NullPointerException e) {
			System.err.println("FXUtils:getIcon failed for " + iconLocation + " -- using black box instead");
			ImageView img = new ImageView(new WritableImage(16, 16));
			return img;
		}
	}

	
	

	public static HBox newHBox() {
		HBox box = new HBox();
        box.setSpacing(5);
        box.setPadding(new Insets(5));
        return box;
	}

	public static VBox newVBox() {
		VBox box = new VBox();
        box.setSpacing(3);
        box.setPadding(new Insets(2));
        return box;
	}
	
	
	
	public static void createHMCButton(Pane pane, BEASTInterface o, Input<?> input)  {
        if (o instanceof BeautiPanelConfig) {
        	BeautiPanelConfig cfg = (BeautiPanelConfig) o;
        	FXUtils.createHMCButton(pane, cfg.parentBEASTObjects.get(0), cfg.parentInputs.get(0));
        	return;
        }
        
        String id = o.getID();
        if (id == null) {
        	// cannot identify BEAST object, so there is no way to 
        	// identify a suitable HMC page
        	return;
        }
		if (id.lastIndexOf('.') > 0) {
			id = id.substring(0, id.lastIndexOf('.'));
		}
		
		if (!hmcPages.containsKey(id + "/" + input.getName() + "/")) {
			id = o.getClass().getSimpleName();
			if (!hmcPages.containsKey(id + "/" + input.getName() + "/")) {
				return;
			}
		}
		//System.out.print(id + "/" + input.getName() + "/ => ");
		id = hmcPages.get(id + "/" + input.getName() + "/");		
		//System.out.println(id);
		
		String HMC_BASE = getHMCBase();
		String url = HMC_BASE + "/" + id;// + "/";//.html";
		Button hmc = createHMCButton(url);
        if (input instanceof BeautiPanelConfig.FlexibleInput) {
        	BeautiPanelConfig.FlexibleInput flexInput = (BeautiPanelConfig.FlexibleInput) input;
        	hmc.setTooltip(new Tooltip(o.getDescription() + "\n" + o.getID() + "\n" + url));
        } else {
        	hmc.setTooltip(new Tooltip(input.getTipText() + "\n" + o.getID() + "\n" + url));
        }
        hmc.getTooltip().setStyle("-fx-font-size: 8pt");
        pane.getChildren().add(hmc);
	}
	
	// maps `help me choose` page ID
	private static java.util.Map<String, String> hmcPages = new HashMap<>();

    public static void processHMCPages(String hmcPages) {
		if (hmcPages == null) {
			// nothing to do
			return;
		}
		String [] strs = hmcPages.split(",");
		for (String str : strs) {
			if (str.indexOf('=') > -1) {
				String [] strs2 = str.split("=");
				String from = strs2[0].trim();
				String to = strs2[1].trim();
				FXUtils.hmcPages.put(from, to);
			} else {
				String from = str.trim();
				FXUtils.hmcPages.put(from, from);
			}
		}		
	}

	public static String getHMCBase() {
		//return "file://" + System.getProperty("user.dir") + "/hmc";
		//return "http://127.0.0.1:4000/hmc/";
		return "https://beast2-dev.github.io/hmc/hmc/";
	}

	
	public static Button createHMCButton(String templateName, String tabName) {
		String HMC_BASE = getHMCBase();
		String base = (templateName + "/" + tabName + "/").replaceAll(" ", "_");
		if (!hmcPages.containsKey(base)) {
			return null;
		}

		String url = HMC_BASE + "/" + hmcPages.get(base) + "index.html";
		url = url.replaceAll(" ", "_");
		return createHMCButton(url);
	}
	
	public static Button createHMCButton(final String url) {		
	    Button hmcButton = new Button("?");
    	hmcButton.setTooltip(new Tooltip("Click to 'help me choose'\n" + url));
    	hmcButton.getTooltip().setStyle("-fx-font-size: 8pt");
    	//hmcButton.setGraphic(FXUtils.getIcon(BEASTObjectDialog.ICONPATH + "help16.png"));
    	hmcButton.setOnAction(e->openInBrowser(url));
        String style = 
                "-fx-background-radius: 10; " +
                "-fx-min-width: 15px; " +
                "-fx-min-height: 15px; " +
                "-fx-max-width: 15px; " +
                "-fx-max-height: 15px; " +
                "-fx-font-size: 5pt";
        hmcButton.setStyle(style);
    	return hmcButton;
    }
	   
	public static void openInBrowser(String url)  {
		
		Platform.runLater(() -> {
			if (!Utils.isLinux() && Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Action.BROWSE)) {
				try {
			        Desktop desktop = Desktop.getDesktop();
					desktop.browse(new URI(url));
	            } catch (IOException | URISyntaxException e) {
	                e.printStackTrace();
	            }
	        } else {
				try {
					if (Utils.isWindows()) {
				        	Runtime rt = Runtime.getRuntime();
				        	rt.exec("rundll32 url.dll,FileProtocolHandler " + url);
					} else if (Utils.isMac()) {
						System.err.println("Opening browser " + url);
						Runtime rt = Runtime.getRuntime();
						rt.exec("open " + url);
					} else if (Utils.isLinux()) {
						Runtime rt = Runtime.getRuntime();
						String[] browsers = { "google-chrome", "firefox", "mozilla", "epiphany", "konqueror",
						                                 "netscape", "opera", "links", "lynx" };
						 
						StringBuffer cmd = new StringBuffer();
						for (int i = 0; i < browsers.length; i++)
						    if(i == 0)
						        cmd.append(String.format(    "%s \"%s\"", browsers[i], url));
						    else
						        cmd.append(String.format(" || %s \"%s\"", browsers[i], url)); 
						    // If the first didn't work, try the next browser and so on
				
						rt.exec(new String[] { "sh", "-c", cmd.toString() });
					} else {
						System.err.println("Opening webview");
						WebView webView = new WebView();
						webView.getEngine().load(url);
						Dialog dlg = new Dialog();
						dlg.getDialogPane().setContent(webView);
						dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
						dlg.setResizable(true);
						dlg.showAndWait();
					}
				} catch (Throwable e) {
					e.printStackTrace();
				}
		    }
		});
	}

	public static SimpleStringProperty startSplashScreen() {
		// see https://github.com/CompEvol/BeastFX/blob/master/src/beastfx/app/beauti/Beauti.java
		// on how to use splash screen.
		// make sure to call endSplashScreen() after the splash screen is not necessary any more.
		splashLabel = new SimpleStringProperty();
		return splashLabel;
	}
	
	public static void endSplashScreen() {
		if (splashLabel != null) {
			splashLabel = null;
		}
	}
	
    static private SimpleStringProperty splashLabel;

    public static void logToSplashScreen(String msg) {
    	if (splashLabel != null) {
    		Platform.runLater(new Runnable() {
				@Override
				public void run() {
			    	splashLabel.setValue(msg);
				}
    		});
    	}
    }

}
