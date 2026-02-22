package beastfx.app.methodsection;



import static javafx.concurrent.Worker.State.FAILED;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import javafx.application.*;
import javafx.beans.value.*;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.*;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import netscape.javascript.JSObject;
import javafx.concurrent.Worker.State;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import beastfx.app.beauti.BeautiTabPane;
import beastfx.app.inputeditor.BeautiAlignmentProvider;
import beastfx.app.inputeditor.BeautiConfig;
import beastfx.app.inputeditor.BeautiDoc;
import beastfx.app.inputeditor.BeautiDoc.DOC_STATUS;
import beastfx.app.methodsection.implementation.BEASTObjectMethodsText;
import beastfx.app.util.Utils;
import beast.base.core.BEASTInterface;
import beast.base.inference.MCMC;
import beast.base.core.Log;
import beast.base.evolution.alignment.Alignment;
import beast.base.spec.evolution.tree.MRCAPrior;
import beast.pkgmgmt.Package;
import beast.pkgmgmt.PackageManager;
import beast.base.parser.XMLParser;
import beast.base.parser.XMLParserException;
import beast.base.parser.XMLProducer;


public class XML2HTMLPaneFX extends Application {

	private WebEngine engine;
	double zoom = 1.0;
	
	static BeautiDoc beautiDoc;
	BeautiTabPane beauti;
	String html;
	List<Phrase> m;
	XML2Text xml2textProducer;
	
	File tmpFile = null;
	File file = null;

	XML2HTMLPaneFX thisPane;
	// ModelEditor me = new ModelEditor(false);
	Stage mainStage;

	public XML2HTMLPaneFX() {
		thisPane = this;
	}

	public void processArgs(String [] args) throws Exception {
		MCMC mcmc = null;
		if (args.length > 0) {
			File file = new File(args[0]);
			beautiDoc.setFileName(file.getAbsolutePath());
			String xml = BeautiDoc.load(file);
			int i = xml.indexOf("beautitemplate=");
			if (i > 0) {
				i += 15;
				char c = xml.charAt(i);
				i++;
				int start = i;
				while (xml.charAt(i) != c) {
					i++;
				}
				String template = xml.substring(start, i);
				if (!template.endsWith("xml")) {
					template = template + ".xml";
				}
				beautiDoc.loadNewTemplate(template);
			} else {
				beautiDoc.loadNewTemplate("Standard.xml");
			}
			
			
			XMLParser parser = new XMLParser();
			mcmc = (MCMC) parser.parseFile(file);
			this.file = file;
		} else {
			mcmc = (MCMC) beautiDoc.mcmc.get();
		}

		beautiDoc.mcmc.setValue(mcmc, beautiDoc);
		for (BEASTInterface o : InputFilter.getDocumentObjects(beautiDoc.mcmc.get())) {
			if (o != null) {
				beautiDoc.registerPlugin(o);
			}
		}
		beautiDoc.determinePartitions();
		BEASTObjectMethodsText.setBeautiCFG(beautiDoc.beautiConfig);
		
		MethodsText.initNameMap();
		initialise((MCMC) beautiDoc.mcmc.get(), true);		
	}
	
		  
	@Override
	public void start(javafx.stage.Stage stage) throws Exception {		
		mainStage = stage;
		WebView view = new WebView();
		view.setPrefHeight(1024);
		view.setContextMenuEnabled(false);
		engine = view.getEngine();

//		engine.locationProperty().addListener(new ChangeListener<String>() {
//			@Override
//			public void changed(ObservableValue<? extends String> ov, String oldValue, final String newValue) {
//				System.out.println("changed:");
//				System.out.println(newValue);
//				if (me.handleCmd(newValue, beautiDoc, null)) {
//					refresh();
//				}
//				if (file != null) {
//					mainStage.setTitle(file.getPath());
//					createFileMenu();
//				}
//			}
//		});

		engine.getLoadWorker().exceptionProperty().addListener(new ChangeListener<Throwable>() {

			@Override
			public void changed(ObservableValue<? extends Throwable> o, Throwable old, final Throwable value) {
				if (engine.getLoadWorker().getState() == FAILED) {
					Alert alert = new Alert(AlertType.INFORMATION);
					alert.setTitle("Error Dialog");
					alert.setHeaderText("Loading error...");
					alert.setContentText((value != null) ? engine.getLocation() + "\n" + value.getMessage()
					: engine.getLocation() + "\nUnexpected error.");

					alert.showAndWait();
				}
			}
		});

		
		 // process page loading
        engine.getLoadWorker().stateProperty().addListener(
            (ObservableValue<? extends State> ov, State oldState, 
                State newState) -> {
                    if (newState == State.SUCCEEDED) {
                        JSObject win
                                = (JSObject) engine.executeScript("window");
                        win.setMember("myObject", thisPane);// new MyObject());
                    }
        });
        
		view.setOnKeyReleased((KeyEvent e) -> {
			System.out.println(e);
			if (e.getText().equals("-")) {
				zoomOut();
			}
			if (e.getText().equals("+")) {
				zoomIn();
			}
		});
				
		MenuBar menubar= createMenu();
		
		if (Utils.isMac()) {
			menubar.useSystemMenuBarProperty().set(true);
		}
		BorderPane vb = new BorderPane();
		vb.setTop(menubar);
		vb.setCenter(view); 

		Scene scene = new Scene(vb, 768, 668);

		stage.setScene(scene);
		stage.setTitle("Loading...");
		stage.show();
		
		engine.loadContent("<html><body style='position:absolute;left:35%;top:30%;'>"
				+ "<img src='data:image/png;base64," 
				+ ImageUtil.getIcon("methods/beasy.png", "png")+ "'>"
				+ "<p><center>Loading...<body></html>");
		
		new Thread() {
			@Override
			public void run() {
				if (beautiDoc == null) {
					beautiDoc = new BeautiDoc();
				}
				beautiDoc.beautiConfig = new BeautiConfig();
				beautiDoc.beautiConfig.initAndValidate();
				beauti = new BeautiTabPane(beautiDoc);

				List<String> args = getParameters().getRaw();
				try {
					processArgs(args.toArray(new String[]{}));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();

	};	
	
	Menu fileMenu;
	Menu modeMenu;
	private MenuBar createMenu() {
		// create a menu 
        fileMenu = new Menu("_File");
        // createFileMenu();
//        Menu workDirMenu = new Menu("Set working dir");
//        fileMenu.getItems().add(workDirMenu);
//        List<AbstractAction> workDirMenuActions = getWorkDirActions();
//        for (AbstractAction a : workDirMenuActions) {
//        	workDirMenu.add(a);
//        }
//        templateMenu.addSeparator();
//        templateMenu.add(a_template);
//        fileMenu.add(a_managePackages);
//        fileMenu.add(a_clearClassPath);
//        fileMenu.add(a_appLauncher);

        Menu editMenu = new Menu("_Edit"); 
        addMenu("Export", "Ctrl+E", editMenu, event ->{export();});  
        
        modeMenu = new Menu("Mode");
             
        Menu helpMenu = new Menu("_Help");
                
        addMenu("About", "Meta+A", helpMenu, event ->{about();});  

        MenuBar mb = new MenuBar();         
        mb.getMenus().add(fileMenu); 
        mb.getMenus().add(editMenu); 
        mb.getMenus().add(modeMenu); 
        mb.getMenus().add(helpMenu); 
  
        return mb;
	}
	
    private void about() {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("About Beasy Magic Methods Section");
        Map<String, Package> packageMap = new TreeMap<>(Comparator.comparing(String::toLowerCase));
        PackageManager.addInstalledPackages(packageMap);
        Package pkg = packageMap.get("beasy");
		alert.setHeaderText("Beasy Magic Methods Section\nVersion " + pkg.getInstalledVersion());
		alert.setContentText("Complaints and suggestions to Remco Bouckaert: "
				+ "r.bouckaert@auckland.ac.nz or raise an issue in the "
				+ "beasy project at: https://github.com/rbouckaert/beasy");
		alert.showAndWait();
	}

	private void createFileMenu() {
		boolean isMac = Utils.isMac();

		// first clear menu
   		fileMenu.getItems().clear();

   		
        addMenu("Load", isMac?"Meta+L":"Ctrl+L", fileMenu, event ->{load();});
        addMenu("Save", isMac?"Meta+S":"Ctrl+S", fileMenu, event ->{save();});
        addMenu("Save As", null, fileMenu, event ->{saveAs();});

        fileMenu.getItems().add(new SeparatorMenuItem());
        addAlignmentProviderMenus(fileMenu);
        fileMenu.getItems().add(new SeparatorMenuItem());

        Menu templateMenu = new Menu("Template");
        fileMenu.getItems().add(templateMenu);
        List<TemplateMenu> templateActions = getTemplateActions();
        for (TemplateMenu a : templateActions) {
            templateMenu.getItems().add(a);
        }

        fileMenu.getItems().add(new SeparatorMenuItem());
        addMenu("Quit", isMac?"Meta+Q":"Ctrl+Q", fileMenu, event ->{System.exit(0);});
        
        
        
        
        modeMenu.getItems().clear();
        CheckMenuItem clockRateMenu = new CheckMenuItem("Automatic set clock rate");
        clockRateMenu.setSelected(beautiDoc.autoSetClockRate);
        clockRateMenu.setOnAction(e -> {
        		beautiDoc.autoSetClockRate = ((CheckMenuItem)e.getSource()).isSelected();
        		refresh();
        	});
        modeMenu.getItems().add(clockRateMenu);

        CheckMenuItem allowLinkingMenu = new CheckMenuItem("Allow parameter linking");
        allowLinkingMenu.setSelected(beautiDoc.allowLinking);
        allowLinkingMenu.setOnAction(e -> {
        		beautiDoc.allowLinking = ((CheckMenuItem)e.getSource()).isSelected();
        		refresh();
        	});
        modeMenu.getItems().add(allowLinkingMenu);
        

        CheckMenuItem autoUpdateFixMeanSubstRate = new CheckMenuItem("Automatic set fix mean substitution rate flag");
        autoUpdateFixMeanSubstRate.setSelected(beautiDoc.autoUpdateFixMeanSubstRate);
        autoUpdateFixMeanSubstRate.setOnAction(e -> {
        		beautiDoc.autoUpdateFixMeanSubstRate = ((CheckMenuItem)e.getSource()).isSelected();
        		refresh();
        	});
        modeMenu.getItems().add(autoUpdateFixMeanSubstRate);
   
	}

	private void addAlignmentProviderMenus(Menu fileMenu) {
        List<BeautiAlignmentProvider> providers = beautiDoc.beautiConfig.alignmentProvider;
        for (BeautiAlignmentProvider provider : providers) {
        	MenuItem action = new MenuItem();
        	action.setOnAction(e -> addPartition(provider));
            //String providerInfo = provider.toString().replaceAll("Add ", "Add partition for ");
            //action.putValue(Action.SHORT_DESCRIPTION, providerInfo);
            //action.putValue(Action.LONG_DESCRIPTION, providerInfo);
            // TODO: add tooltip text
            action.setText(provider.toString());
        	fileMenu.getItems().add(action);
        }
	}
	
	void addPartition(BeautiAlignmentProvider provider) {
        try {
//            setCursor(new Cursor(Cursor.WAIT_CURSOR));

            // get user-specified alignments
	        List<BEASTInterface> beastObjects = provider.getAlignments(beautiDoc);
	        if (beastObjects != null) {
		        for (BEASTInterface o : beastObjects) {
		        	if (o instanceof Alignment) {
		        		try {
		        			BeautiDoc.createTaxonSet((Alignment) o, beautiDoc);
		        		} catch(Exception ex) {
		        			ex.printStackTrace();
		        		}
		        	}
		        }
	        }

	        beautiDoc.scrubAll(true, true);
	        // beautiDoc.fireDocHasChanged();
            
	        if (beastObjects != null) {
		        for (BEASTInterface o : beastObjects) {
		        	if (o instanceof MRCAPrior) {
		        		beautiDoc.addMRCAPrior((MRCAPrior) o);
		        	}
		        }
	        }
            // a_save.setEnabled(true);
            // a_saveas.setEnabled(true);
        } catch (Exception exx) {
            exx.printStackTrace();
            
			Alert alert = new Alert(AlertType.INFORMATION);
			alert.setTitle("Warning Dialog");
			alert.setHeaderText("Something went wrong importing the alignment:\n");
			alert.setContentText(exx.getMessage());

			TextArea textArea = new TextArea(exx.getMessage());
			textArea.setEditable(false);
			textArea.setWrapText(true);

			textArea.setMaxWidth(Double.MAX_VALUE);
			textArea.setMaxHeight(Double.MAX_VALUE);

			alert.getDialogPane().setExpandableContent(textArea);
			alert.getDialogPane().setExpanded(true);
			alert.showAndWait();
        }
//        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));

	}
	
	private List<TemplateMenu> getTemplateActions() {
        List<TemplateMenu> actions = new ArrayList<>();
        List<String> beastDirectories = PackageManager.getBeastDirectories();
        for (String dirName : beastDirectories) {
            File dir = new File(dirName + "/" + BeautiConfig.TEMPLATE_DIR);
            getTemplateActionForDir(dir, actions);
        }
        return actions;
    }

    private void getTemplateActionForDir(File dir, List<TemplateMenu> actions) {
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File template : files) {
                    if (template.getName().toLowerCase().endsWith(".xml")) {
                        try {
                            String xml2 = BeautiDoc.load(template.getAbsolutePath());
                            if (xml2.contains("templateinfo=")) {
                            	String fileName = template.getName();
                                fileName = fileName.substring(0, fileName.length() - 4);
                                boolean duplicate = false;
                            	for (TemplateMenu action : actions) {
                            		String name = action.getText();
                            		if (name.equals(fileName)) {
                            			duplicate = true;
                            		}
                            	}
                            	if (!duplicate) {
                            		TemplateMenu menu = new TemplateMenu(template);
                            		actions.add(menu);
                            	}
                            }
                        } catch (Exception e) {
                        	Log.warning.println(e.getMessage());
                        }
                    }
                }
            }
        }
    }

    public class TemplateMenu extends CustomMenuItem {

        String fileName;
        String templateInfo;

        public TemplateMenu(File file) {
            super();
            fileName = file.getAbsolutePath();
            String fileSep = System.getProperty("file.separator");
            if (fileSep.equals("\\")) {
                fileSep = "\\";
            }
            int i = fileName.lastIndexOf(fileSep) + 1;
            String name = fileName.substring(
                    i, fileName.length() - 4);
            
            setText(name);
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory
                        .newInstance();
                Document doc = factory.newDocumentBuilder().parse(file);
                doc.normalize();
                // get name and version of add-on
                Element template = doc.getDocumentElement();
                templateInfo = template.getAttribute("templateinfo");
                if (templateInfo == null || templateInfo.length() == 0) {
                    templateInfo = "switch to " + name + " template";
                }
                // TODO: install tool tip
                Tooltip.install(this.getContent(), new Tooltip(templateInfo));
                // setToolTip(templateInfo);
                // Tooltip.install(getStyleableNode(), new Tooltip(templateInfo));
            } catch (Exception e) {
                // ignore
            }
            setOnAction(e -> {loadTemplate(this);});
        }        
    }
    
    void loadTemplate(TemplateMenu a) {
        try {
            if (beautiDoc.validateModel() == DOC_STATUS.NO_DOCUMENT) {
            	beautiDoc.loadNewTemplate(a.fileName);
//              createFileMenu();
				refresh();
            } else {
    			Alert alert = new Alert(AlertType.INFORMATION);
    			alert.setTitle("Switching templates");
    			alert.setHeaderText("Are you sure:");
    			alert.setContentText("Changing templates means the information input so far will be lost. "
                        + "Are you sure you want to change templates?");
    			if (alert.showAndWait().get() == ButtonType.OK) {
    				beauti.isInitialising = true;
    				beautiDoc.loadNewTemplate(a.fileName);
                    createFileMenu();
    				refresh();
    			}
    		}
        } catch (Exception ex) {
            ex.printStackTrace();
			Alert alert = new Alert(AlertType.INFORMATION);
			alert.setTitle("Switching templates");
			alert.setHeaderText("Something went wrong loading the template:");
			alert.setContentText(ex.getMessage());
			alert.showAndWait();
        }
    }
    
	private void addMenu(String name, String accelerator, Menu menu, EventHandler<ActionEvent> event) {
        MenuItem menuItem = new MenuItem(name);
        menuItem.setOnAction(event);
        if (accelerator != null) {
        	menuItem.setAccelerator(KeyCombination.valueOf(accelerator));
        }
        menu.getItems().add(menuItem);		
	}

	private void export() {
		ChoiceDialog<CitationPhrase.mode> dialog = new ChoiceDialog<>(CitationPhrase.CitationMode, 
				CitationPhrase.mode.values());
		dialog.setTitle("Export Dialog");
		dialog.setHeaderText("Export methods section");
		dialog.setContentText("Choose format:");

		// Traditional way to get the response value.
		Optional<CitationPhrase.mode> result = dialog.showAndWait();
		if (result.isPresent()){
		    System.out.println("Your choice: " + result.get());
			CitationPhrase.mode mode = result.get();
			StringSelection stringSelection;
			try {
				stringSelection = new StringSelection(getText(mode));
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				clipboard.setContents(stringSelection, null);				
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
	}
	
	private void refresh() {
		beautiDoc.determinePartitions();
		beautiDoc.scrubAll(false, false);
		CitationPhrase.citations.clear();

		MethodsText.clear();
		try {
			initialise((MCMC) beautiDoc.mcmc.get(), false);
			load(html);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void load() {
		 FileChooser fileChooser = new FileChooser();
		 fileChooser.setTitle("Save BEAST File");
		 fileChooser.getExtensionFilters().addAll(
		         new FileChooser.ExtensionFilter("XML Files", "*.xml"),
		         new FileChooser.ExtensionFilter("All Files", "*.*"));
		 File selectedFile = fileChooser.showOpenDialog(mainStage);
		 if (selectedFile != null) {
			 try {
				beautiDoc.loadXML(selectedFile);
				refresh();
			} catch (IOException | XMLParserException | SAXException | ParserConfigurationException e) {
				e.printStackTrace();
			}
		 }		
	}
	
	private void save() {
	    if (!beautiDoc.getFileName().equals("")) {
	        saveFile(beautiDoc.getFileName());
	    } else {
	        saveAs();
	    }
	}
	
	private void saveAs() {
		 FileChooser fileChooser = new FileChooser();
		 fileChooser.setTitle("Save BEAST File");
		 fileChooser.getExtensionFilters().addAll(
		         new FileChooser.ExtensionFilter("XML Files", "*.xml"),
		         new FileChooser.ExtensionFilter("All Files", "*.*"));
		 File selectedFile = fileChooser.showSaveDialog(mainStage);
		 if (selectedFile != null) {
			 saveFile(selectedFile.getPath());
		 }
	}

	private void saveFile(String path) {
	    XMLProducer producer = new XMLProducer();
	    String xml = producer.toXML(beautiDoc.mcmc.get());
		try {
	        FileWriter outfile = new FileWriter(path);
	        outfile.write(xml);
	        outfile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public final static String header = "<!DOCTYPE html>\n" +
			"<html>\n" +
			"<style>\n" +
			".reference {font-size:10pt;color:#aaa;}\n" +
			".tipdates {display:inline;}\n" +
			"a{color:#555;text-decoration:none;background-color:#fafafa;}\n" + 
			".pe {color:#555;background-color:#fafafa;}\n" + 
			".para {color:#555;background-color:#fafafa;}\n" + 
			"select{color:#555;font-weight:normal;-webkit-appearance:none;background-color:#fafafa;border-width:5pt;}\n" + 
			"a:hover{background-color:#aaa;}\n" + 
			"select:hover{background-color:#aaa;}\n" +
			"</style>\n" +
			"<body style='font: 12pt arial, sans-serif;margin: 50pt 100pt 50pt 100pt;'>"
			//+ "<input type='button' onclick='window.myObject.doIt(\"ok\");' value='Click me'/>\n"
			;

	public final static String footer = "<p><a style='font-size:10pt;color:#aaf;' "
			+ "href=\"/cmd=AddPrior\">Add other prior</a>\n"
			+ "<p><center><img src='data:image/png;base64," 
				+ ImageUtil.getIcon("methods/beasy.png", "png")+ "'></center>";
	
	public void initialise(MCMC mcmc, boolean update) throws Exception {		
		xml2textProducer = new XML2Text(beautiDoc);
		xml2textProducer.initialise((MCMC) beautiDoc.mcmc.get());
		m = xml2textProducer.getPhrases();
		
		html = header + Phrase.toHTML(beautiDoc, m) + footer + "</body>\n</html>";
		
        if (update) {
        	updateState(html);
        } else {
        	load(html);
        }

        FileWriter outfile = new FileWriter("/tmp/index.html");
        outfile.write(html);
        outfile.close();
	}

	
	/**
	 * change html text and enable/disable buttons (where appropriate) *
	 */
	void updateState(Object page) {
		if (page instanceof String) {

			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					load((String) page);
					//engine.loadContent((String) page);
				}

			});
			zoom(zoom);
		} else if (page instanceof URL) {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					String tmp = null;
					try {
						tmp =  new URL("" + page).toExternalForm();
					} catch (MalformedURLException e) {
						try {
							tmp =  new URL("http://" + page).toExternalForm();
						} catch (MalformedURLException e2) {
							return;
						}
					}

					engine.load(tmp);
				}
			});
			zoom(zoom);
		}
	} // updateState

	public void load(String page) {
		try {
			if (tmpFile == null) {
				tmpFile = File.createTempFile("index", ".html");
				System.err.println(tmpFile.getPath());
			}
			FileWriter outfile = new FileWriter(tmpFile);
			outfile.write(html);
			outfile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// the following does not appear to be
		// able the handle select.onchanged events
		// but loading from file does -- not sure why
//		engine.loadContent((String)page, "text/html");
//		engine.setJavaScriptEnabled(true);
		engine.load("file://" + tmpFile);
	}

	
	private void zoomIn() {
		zoom(zoom * 1.1);
	}

	private void zoomOut() {
		zoom(zoom / 1.1);

	}

	void zoom(double _zoom) {
		this.zoom = _zoom;

		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				engine.executeScript("document.body.style.zoom=" + zoom);
			}
		});
	}
	
	public String getText(CitationPhrase.mode mode) throws Exception {		
		CitationPhrase.CitationMode = mode;
		xml2textProducer = new XML2Text(beautiDoc);
		xml2textProducer.initialise((MCMC) beautiDoc.mcmc.get());
		m = xml2textProducer.getPhrases();
		return Phrase.toString(m);
	}

//    public void doIt(String str) {
//        System.out.println("doIt(" + str + ") called");
//		if (me.handleCmd("/cmd=" + str, beautiDoc, null)) {
//			beautiDoc.determinePartitions();
//			beautiDoc.scrubAll(false, false);
//			CitationPhrase.citations.clear();
//
//			MethodsText.clear();
//			try {
//				initialise((MCMC) beautiDoc.mcmc.get(), false);
//				load(html);
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		}
//
//    }
//    
//    public void doIt(String str, String source) {
//        System.out.println("doIt(" + str + ") called with source = " + source);
//        me.handleCmd("/cmd=Text value=\""+str+"\" source=\"" + source + "\"", beautiDoc, null);
//    }

    public static void main(String[] args) throws Exception {		
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		System.setProperty("com.apple.mrj.application.apple.menu.about.name", "XML2HTMLPandFX");
		
		launch(XML2HTMLPaneFX.class, args);
	}

	public static void launchForDoc(BeautiDoc doc) {
		beautiDoc = doc;
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		System.setProperty("com.apple.mrj.application.apple.menu.about.name", "XML2HTMLPandFX");

		launch(XML2HTMLPaneFX.class, new String[]{});		
	}

}
