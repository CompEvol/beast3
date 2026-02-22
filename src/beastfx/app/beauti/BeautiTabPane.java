package beastfx.app.beauti;






import javafx.event.ActionEvent;
import javafx.scene.Cursor;
import javafx.scene.Scene;

import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Dialog;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import beastfx.app.beauti.theme.Default;
import beastfx.app.inputeditor.BEASTObjectDialog;
import beastfx.app.inputeditor.BEASTObjectPanel;
import beastfx.app.inputeditor.BeautiAlignmentProvider;
import beastfx.app.inputeditor.BeautiConfig;
import beastfx.app.inputeditor.BeautiDoc;
import beastfx.app.inputeditor.BeautiDocListener;
import beastfx.app.inputeditor.BeautiPanel;
import beastfx.app.inputeditor.BeautiPanelConfig;
import beastfx.app.inputeditor.MyAction;
import beastfx.app.methodsection.MethodsText;
import beastfx.app.methodsection.Phrase;
import beastfx.app.methodsection.XML2HTMLPaneFX;
import beastfx.app.methodsection.XML2Text;
import beastfx.app.util.Alert;
import beastfx.app.util.FXUtils;
import beastfx.app.inputeditor.BeautiDoc.ActionOnExit;
import beastfx.app.inputeditor.BeautiDoc.DOC_STATUS;
import beastfx.app.tools.AppLauncher;
import beastfx.app.util.Utils;
import beast.base.core.BEASTInterface;
import beast.base.core.BEASTVersion2;
import beast.base.core.Log;
import beast.base.core.ProgramStatus;
import beast.base.evolution.alignment.Alignment;
import beast.base.inference.MCMC;
import beast.base.parser.XMLParserException;
import beast.base.spec.evolution.tree.MRCAPrior;
import beast.pkgmgmt.BEASTClassLoader;
import beast.pkgmgmt.PackageManager;
import beast.pkgmgmt.Utils6;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.*;


public class BeautiTabPane extends beastfx.app.inputeditor.BeautiTabPane implements BeautiDocListener {
    static final String BEAUTI_ICON = BEASTObjectDialog.ICONPATH + "/beauti.png";

    /**
     * File extension for Beast specifications
     */
    static public final String FILE_EXT = ".xml";
    static public final String FILE_EXT2 = ".json";
    static final String fileSep = System.getProperty("file.separator");

    /**
     * document in document-view pattern. BTW this class is the view
     */
    public BeautiDoc doc;
    public Stage frame;

    /**
     * currently selected tab *
     */
    public BeautiPanel currentTab;

    public boolean[] isPaneIsVisible;
    public BeautiPanel[] panels;

    /**
     * menu for file handling, importing partitions, etc.
     */
	Menu fileMenu;
    /**
     * menu for switching templates *
     */
    Menu templateMenu;
    /**
     * menu for making showing/hiding tabs *
     */
    Menu viewMenu;

    CheckMenuItem autoSetClockRate;
    CheckMenuItem allowLinking;
    CheckMenuItem autoUpdateFixMeanSubstRate;

    

    /**
     * flag indicating beauti is in the process of being set up and panels
     * should not sync with current model *
     */
    public boolean isInitialising = true;

    public BeautiTabPane(BeautiDoc doc) {
        isPaneIsVisible = new boolean[doc.beautiConfig.panels.size()];
        Arrays.fill(isPaneIsVisible, true);
        // m_panels = new BeautiPanel[NR_OF_PANELS];
        this.doc = doc;
        this.doc.addBeautiDocListener(this);
        doc.setBeauti(this);
        BEAUtiIntances++;
    }

    void setTitle() {
        ((Stage)getScene().getWindow()).setTitle("BEAUti 3: " + this.doc.getTemplateName() + " "
                + doc.getFileName());
    }

    void toggleVisible(int panelNr) {
        if (isPaneIsVisible[panelNr]) {
            isPaneIsVisible[panelNr] = false;
            int tabNr = tabNrForPanel(panelNr);
            getTabs().remove(tabNr);
        } else {
            isPaneIsVisible[panelNr] = true;
            int tabNr = tabNrForPanel(panelNr);
            BeautiPanelConfig panel = doc.beautiConfig.panels.get(panelNr);
            getTabs().add(tabNr, panels[panelNr]);
            getTabs().get(tabNr).setTooltip(new Tooltip(panel.tipTextInput.get()));
            getSelectionModel().select(tabNr);
        }
    }

    int tabNrForPanel(int panelNr) {
        int k = 0;
        for (int i = 0; i < panelNr; i++) {
            if (isPaneIsVisible[i]) {
                k++;
            }
        }
        return k;
    }

    MenuItem a_new = new ActionNew();
    public MenuItem a_load = new ActionLoad();
    MenuItem a_template = new ActionTemplate();
    MenuItem a_managePackages = new ActionManagePacakges();
    MenuItem a_clearClassPath = new ActionClearClassPath();
    
    public MenuItem a_save = new ActionSave();
    MenuItem a_saveas = new ActionSaveAs();
    MenuItem a_close = new ActionClose();
    MenuItem a_quit = new ActionQuit();
    MenuItem a_viewall = new ActionViewAllPanels();
    MenuItem a_appLauncher = new ActionLaunch();

    MenuItem a_help = new ActionHelp();
    MenuItem a_msgs = new ActionMsgs();
    MenuItem a_citation = new ActionCitation();
    MenuItem a_about = new ActionAbout();
    MenuItem a_viewModel = new ActionViewModel();

    @Override
    public void docHasChanged() throws NoSuchMethodException, SecurityException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        setUpPanels();
        setUpViewMenu();
        setTitle();
    }

    class ActionSave extends MyAction {
        

        public ActionSave() {
            super("Save", "Save Model", "save", new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN));
            setDisable(true);
        } // c'tor

        public ActionSave(String name, String toolTipText, String icon,
        		KeyCodeCombination acceleratorKey) {
            super(name, toolTipText, icon, acceleratorKey);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
            DOC_STATUS docStatus = doc.validateModel();
            if (docStatus != DOC_STATUS.DIRTY) {
                if (docStatus == DOC_STATUS.NO_DOCUMENT)
                    Alert.showMessageDialog(null,
                            "The model is empty, there is nothing to save. Set up a model (by importing data) before saving.");

                return;
            }

            if (!doc.getFileName().equals("")) {
                saveFile(doc.getFileName());
                // m_doc.isSaved();
            } else {
                if (saveAs()) {
                    // m_doc.isSaved();
                }
            }
        } // actionPerformed

    } // class ActionSave

    class ActionSaveAs extends ActionSave {
        public ActionSaveAs() {
            super("Save As", "Save Model As", "saveas", null);
            setDisable(true);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
            saveAs();
        } // actionPerformed
    } // class ActionSaveAs

    boolean saveAs() {
        if (doc.validateModel() == DOC_STATUS.NO_DOCUMENT) {
            Alert.showMessageDialog(null,
                    "The model is empty, there is nothing to save. Set up a model (by importing data) before saving.");
            return false;
        }
        String fileSep = System.getProperty("file.separator");
        if (fileSep.equals("\\")) {
            fileSep = "\\\\";
        }
        String defaultFile = ProgramStatus.g_sDir + (doc.getFileName().equals("") ? "" : fileSep + new File(doc.getFileName()).getName());
        File file = FXUtils.getSaveFile("Save Model As", new File(
                defaultFile), null, FILE_EXT, FILE_EXT2);
        if (file != null) {
            doc.setFileName(file.getAbsolutePath());// fc.getSelectedFile().toString();
            if (doc.getFileName().lastIndexOf(File.separator) > 0) {
            	ProgramStatus.setCurrentDir(doc.getFileName().substring(0,
                        doc.getFileName().lastIndexOf(File.separator)));
            }
            if (!doc.getFileName().toLowerCase().endsWith(FILE_EXT) && !doc.getFileName().toLowerCase().endsWith(FILE_EXT2)) {
                doc.setFileName(doc.getFileName().concat(FILE_EXT));
            }
            // remove double dots in file name (xyz..xml) introduced in windows
            if (doc.getFileName().toLowerCase().endsWith("." + FILE_EXT)) {
            	String filename = doc.getFileName();
            	filename = filename.substring(0, filename.length() - FILE_EXT.length() - 1);
                doc.setFileName(filename + FILE_EXT);            	
            }
            saveFile(doc.getFileName());
            setTitle();
            return true;
        }
        return false;
    } // saveAs

    public void saveFile(String fileName) {
        try {
            if (currentTab != null) {
                currentTab.config.sync(currentTab.partitionIndex);
            } else {
                panels[0].config.sync(0);
            }
            doc.save(fileName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    } // saveFile

    class ActionNew extends MyAction {
        

        public ActionNew() {
            super("New", "Start new analysis", "new", new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN));
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
        	Stage stage = new Stage();
            main2(new String[0], stage);        			
            stage.show();
        }
    }

    class ActionLoad extends MyAction {
        

        public ActionLoad() {
            super("Load", "Load Beast File", "open", new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN));
        }

        public ActionLoad(String name, String toolTipText, String icon,
                          KeyCodeCombination acceleratorKey) {
            super(name, toolTipText, icon, acceleratorKey);
        }

        @Override
		public void actionPerformed(ActionEvent ae) {
            File file = FXUtils.getLoadFile("Load Beast XML File",
                    new File(ProgramStatus.g_sDir), "Beast XML files", "xml");//, "BEAST json file", "json");
            if (file != null) {
                setCursor(Cursor.WAIT);
                doc.newAnalysis();
                doc.setFileName(file.getAbsolutePath());
                if (doc.getFileName().lastIndexOf(File.separator) > 0) {
                	ProgramStatus.setCurrentDir(doc.getFileName().substring(0,
                            doc.getFileName().lastIndexOf(File.separator)));
                }
                try {
                	// TODO: deal with json files
                    doc.loadXML(new File(doc.getFileName()));
                    a_save.setDisable(false);
                    a_saveas.setDisable(false);
                    setTitle();
                } catch (Exception e) {
                    e.printStackTrace();
                    Alert.showMessageDialog(
                            null,
                            "Something went wrong loading the file: "
                                    + e.getMessage());
                }
            }
            setCursor(Cursor.DEFAULT);
        } // actionPerformed
    }

    class ActionTemplate extends MyAction {
        

        public ActionTemplate() {
            super("Other Template", "Load Beast Analysis Template From File",
                    "template", null);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
            setCursor(Cursor.WAIT);
            File file = beastfx.app.util.FXUtils
                    .getLoadFile("Load Template XML File");
            if (file != null) {
                String fileName = file.getAbsolutePath();
                try {
                    doc.loadNewTemplate(fileName);
                } catch (Exception e) {
                    e.printStackTrace();
                    Alert.showMessageDialog(
                            null,
                            "Something went wrong loading the template: "
                                    + e.getMessage());
                }
            }
            createFileMenu();
            setCursor(Cursor.DEFAULT);
        } // actionPerformed
    } // ActionTemplate

    class ActionManagePacakges extends MyAction {
        

        public ActionManagePacakges() {
            super("Manage Packages", "Manage Packages", "package", null);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
        	//JPackageDialog panel = new JPackageDialog();
        	Dialog dlg = JPackageDialog.asDialog(frame.getScene().getRoot());
        	ThemeProvider.loadStyleSheet(dlg.getDialogPane().getScene());
        	dlg.showAndWait();
            // refresh template menu item
            templateMenu.getItems().removeAll(templateMenu.getItems());
            List<MyAction> templateActions = getTemplateActions();
            for (MenuItem a : templateActions) {
                templateMenu.getItems().add(a);
            }
            templateMenu.getItems().add(new SeparatorMenuItem());
            templateMenu.getItems().add(a_template);
            setCursor(Cursor.DEFAULT);
        } // actionPerformed
    }
    
    class ActionLaunch extends MyAction {
        

        public ActionLaunch() {
            super("Launch Apps", "Launch BEAST Apps supplied by packages", "launch", null);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
        	AppLauncher.main(new String[]{});
        } // actionPerformed
    }

    class ActionClearClassPath extends MyAction {
        

        public ActionClearClassPath() {
            super("Clear Class Path", "Clear class path, so it will be refreshed next time BEAUti starts. Only useful when installing packages by hand.", "ccp", null);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
        	Utils6.saveBeautiProperty("package.path", null);
        	Alert.showMessageDialog(null, "The class path was cleared.\n"
        			+ "Next time you start BEAUti, the class path will be re-established.\n"
        			+ "This is only useful when you install packages by han.d\n"
        			+ "Otherwise, this is harmless, but onlys potentially slows restarting BEAUti.");
        } // actionPerformed
    }
    

    class ActionClose extends ActionSave {
        public ActionClose() {
            super("Close", "Close Window", "close", new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN));
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
            // if (!m_doc.m_bIsSaved) {
            if (!quit()) {
                return;
            }
            Stage stage = (Stage) getScene().getWindow();
            stage.close();            
        }
    } // class ActionClose

    class ActionQuit extends ActionSave {
        public ActionQuit() {
            super("Exit", "Exit Program", "exit", new KeyCodeCombination(KeyCode.Q, KeyCombination.SHORTCUT_DOWN));
            //putValue(Action.MNEMONIC_KEY, new Integer('x'));
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
            // if (!m_doc.m_bIsSaved) {
            if (!quit()) {
                return;
            }
            System.exit(0);
        }
    } // class ActionQuit

    boolean quit() {
        if (doc.validateModel() == DOC_STATUS.DIRTY) {
            ButtonType result = Alert.showConfirmDialog(null,
                    "Do you want to save the Beast specification?",
                    "Save before closing?", Alert.YES_NO_CANCEL_OPTION);
            Log.err.println("result=" + result);
            if (result == Alert.CANCEL_OPTION) {
                return false;
            }
            if (result == Alert.YES_OPTION) {
                if (!saveAs()) {
                    return false;
                }
            }
        }
        return true;
    }

    ViewPanelCheckBoxMenuItem[] m_viewPanelCheckBoxMenuItems;

    class ViewPanelCheckBoxMenuItem extends CheckMenuItem {
        int m_iPanel;

        ViewPanelCheckBoxMenuItem(int panelIndex) {
            super("Show "
                    + doc.beautiConfig.panels.get(panelIndex).nameInput.get()
                    + " panel");
            setSelected(
                    doc.beautiConfig.panels.get(panelIndex).isVisibleInput.get());
            m_iPanel = panelIndex;
            if (m_viewPanelCheckBoxMenuItems == null) {
                m_viewPanelCheckBoxMenuItems = new ViewPanelCheckBoxMenuItem[doc.beautiConfig.panels
                        .size()];
            }
            m_viewPanelCheckBoxMenuItems[panelIndex] = this;
        } // c'tor

        void doAction() {
            toggleVisible(m_iPanel);
        }
    }

    MyAction a_viewMethodSection = new MyAction("Show methods section", "Show text representation of the current model, which can be used as methods section.", "ccp", null) {

		@Override
		public void actionPerformed(ActionEvent ae) {
			XML2Text xml2text = new XML2Text(doc);
			try {
				MethodsText.initNameMap();
				String text = xml2text.initialise((MCMC) doc.mcmc.get());
				List<Phrase> m = xml2text.getPhrases();
				String html = XML2HTMLPaneFX.header + Phrase.toHTML(doc, m) + XML2HTMLPaneFX.footer + "</body>\n</html>";
				WebView webView = new WebView();
				webView.getEngine().loadContent(html);
				Dialog dlg = new Dialog();
				dlg.getDialogPane().setContent(webView);
				dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
				dlg.showAndWait();
			} catch (Exception e) {
				e.printStackTrace();
				Alert.showMessageDialog(null, e.getMessage());
			}
		} // actionPerformed
	
    };
    
    /**
     * makes all panels visible *
     */
    class ActionViewAllPanels extends MyAction {
        

        public ActionViewAllPanels() {
            super("View all", "View all panels", "viewall", null);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
            for (int panelNr = 0; panelNr < isPaneIsVisible.length; panelNr++) {
                if (!isPaneIsVisible[panelNr]) {
                    toggleVisible(panelNr);
                    m_viewPanelCheckBoxMenuItems[panelNr].setSelected(true);
                }
            }
        } // actionPerformed
    } // class ActionViewAllPanels

    class ActionAbout extends MyAction {
        

        public ActionAbout() {
            super("About", "Help about", "about", null);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
            BEASTVersion2 version = new BEASTVersion2();
            Alert.showMessageDialog(null, version.getCredits(),
                    "About Beauti " + version.getVersionString() + 
                    " Java version " + System.getProperty("java.version"), Alert.PLAIN_MESSAGE,
                    Utils.getIcon("BEAST.app", BEAUTI_ICON));
        }
    } // class ActionAbout

    class ActionHelp extends MyAction {
        

        public ActionHelp() {
            super("Help", "Help on current panel", "help", null);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
            setCursor(Cursor.WAIT);
            FXUtils.openInBrowser(FXUtils.getHMCBase()+"..");
            setCursor(Cursor.DEFAULT);
        }
    } // class ActionHelp

    class ActionMsgs extends MyAction {
        

        public ActionMsgs() {
            super("Messages", "Show information, warning and error messages", "msgs", null);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
        	String msgs = null;
        	if (BeautiDoc.baos == null) {
        		msgs = 
        				"Error and warning messages are printed to Stdout and Stderr\n" +
        				"To show them here, start BEAUti with the -capture argument.";
        		
        	} else {
	        	msgs = BeautiDoc.baos.toString();
        	}
	    	javafx.scene.control.TextArea textArea = new javafx.scene.control.TextArea(msgs);
	    	textArea.setPrefRowCount(40);
	    	textArea.setPrefColumnCount(50);
	    	textArea.setEditable(true);
	    	Alert.showMessageDialog(frame.getScene().getRoot(), textArea, "Messages", Alert.WARNING_MESSAGE);
        }
    }

    class ActionCitation extends MyAction implements ClipboardOwner {
        

        public ActionCitation() {
            super("Citation",
                    "Show appropriate citations and copy to clipboard",
                    "citation", null);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
            String citations = doc.mcmc.get().getCitations();
            try {
                StringSelection stringSelection = new StringSelection(
                        citations);
                Clipboard clipboard = Toolkit.getDefaultToolkit()
                        .getSystemClipboard();
                clipboard.setContents(stringSelection, this);
            } catch (Exception e) {
                e.printStackTrace();
            }
	    	javafx.scene.control.TextArea textArea = new javafx.scene.control.TextArea(
	    			citations +
                    "\nCitations copied to clipboard");
	    	textArea.setPrefRowCount(30);
	    	textArea.setPrefColumnCount(70);
	    	textArea.setEditable(true);
            
            Alert.showMessageDialog(frame.getScene().getRoot(), textArea, "Messages", Alert.INFORMATION_MESSAGE);

        } // getCitations

        @Override
        public void lostOwnership(Clipboard clipboard, Transferable contents) {
            // do nothing
        }
    } // class ActionAbout

    class ActionViewModel extends MyAction {
        

        public ActionViewModel() {
            super("View model", "View model graph", "model", null);
        } // c'tor

        @Override
		public void actionPerformed(ActionEvent ae) {
//            Stage frame = new Stage();
//            frame.setTitle("Model Builder");
//            SwingNode pane = new SwingNode();
//            ModelBuilder modelBuilder = new ModelBuilder();
//            modelBuilder.init();
//            JPanel panel = new JPanel();
//            panel.setLayout(new BorderLayout());
//            panel.add(modelBuilder, BorderLayout.CENTER);
//            panel.add(modelBuilder.m_jTbTools2, BorderLayout.NORTH);
//            pane.setContent(panel);
//            modelBuilder.setEditable(false);
//            modelBuilder.m_doc.init(doc.mcmc.get());
//            modelBuilder.setDrawingFlag();
//            panel.setPreferredSize(new Dimension(600, 800));
//            StackPane pane2 = new StackPane();
//            pane2.getChildren().add(pane);
//            Scene scene = new Scene(pane2);
//            frame.setScene(scene);
        	// TODO: make the above work
        	Alert.showMessageDialog(null, "ModelBuilder is not implemented yet");
        }
    } // class ActionViewModel

    public void refreshPanel() {
        try {
            BeautiPanel panel = (BeautiPanel)getSelectionModel().getSelectedItem();
            if (panel != null) {
                this.doc.determinePartitions();
                panel.updateList();
                panel.refreshPanel();
            }
            requestFocus();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    public MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        fileMenu = new Menu("_File");
        fileMenu.setId("File");
        fileMenu.setMnemonicParsing(true);
        menuBar.getMenus().add(fileMenu);
        createFileMenu();

        Menu modeMenu = new Menu("Mode");
        modeMenu.setId("Mode");
        menuBar.getMenus().add(modeMenu);
        modeMenu.setMnemonicParsing(true);

        autoSetClockRate = new CheckMenuItem("Automatic set clock rate");
        autoSetClockRate.setId("autoSetClockRate");
        autoSetClockRate.setSelected(this.doc.autoSetClockRate);
        autoSetClockRate.setOnAction(ae -> {
                doc.autoSetClockRate = autoSetClockRate.isSelected();
                refreshPanel();
            });
        modeMenu.getItems().add(autoSetClockRate);

        allowLinking = new CheckMenuItem("Allow parameter linking");
        allowLinking.setId("allowLinking");
        allowLinking.setSelected(this.doc.allowLinking);
        allowLinking.setOnAction(ae -> {
                doc.allowLinking = allowLinking.isSelected();
                doc.determineLinks();
                refreshPanel();
            });
        modeMenu.getItems().add(allowLinking);

        autoUpdateFixMeanSubstRate = new CheckMenuItem("Automatic set fix mean substitution rate flag");
        autoUpdateFixMeanSubstRate.setId("autoUpdateFixMeanSubstRate");
        autoUpdateFixMeanSubstRate.setSelected(this.doc.autoUpdateFixMeanSubstRate);
        autoUpdateFixMeanSubstRate.setOnAction(ae -> {
                doc.autoUpdateFixMeanSubstRate = autoUpdateFixMeanSubstRate.isSelected();
                refreshPanel();
            });
        modeMenu.getItems().add(autoUpdateFixMeanSubstRate);

        viewMenu = new Menu("_View");
        viewMenu.setId("View");
        menuBar.getMenus().add(viewMenu);
        viewMenu.setMnemonicParsing(true);
        setUpViewMenu();

        Menu helpMenu = new Menu("_Help");
        helpMenu.setId("Help");
        menuBar.getMenus().add(helpMenu);
        helpMenu.setMnemonicParsing(true);
        helpMenu.getItems().add(a_help);
        helpMenu.getItems().add(a_msgs);
        helpMenu.getItems().add(a_citation);
        helpMenu.getItems().add(a_viewModel);
        addCustomHelpMenus(helpMenu);
        if (!Utils.isMac() || Utils6.isMajorLower(Utils6.JAVA_1_8)) {
            helpMenu.getItems().add(a_about);
        } else {
            helpMenu.getItems().add(a_about);
        }

        return menuBar;
    } // makeMenuBar

    // Find sub-classes of BeautiHelpAction and add custom help menu items
    // for these classes
    private void addCustomHelpMenus(Menu helpMenu) {
        String helpClass = "beastfx.app.beauti.BeautiHelpAction";
        Set<String> helpActions = Utils.loadService(BeautiHelpAction.class);
        
        if (helpActions.size() > 1) {
            helpMenu.getItems().add(new SeparatorMenuItem());
            for (String className : helpActions) {
            	if (!className.equals(helpClass)) {
		            try {
		            	Class<?> _class = BEASTClassLoader.forName(className);
		                Constructor<?> con = _class.getConstructor(BeautiDoc.class);
		                BeautiHelpAction helpAction = (BeautiHelpAction) con.newInstance(doc);
		                helpMenu.getItems().add(helpAction);
		            } catch (Throwable t) {
		            	t.printStackTrace();
		            }
            	}
            }
            helpMenu.getItems().add(new SeparatorMenuItem());
        }
    }

	private void createFileMenu() {
    	// first clear menu
   		fileMenu.getItems().clear();

        fileMenu.getItems().add(a_new);
        fileMenu.getItems().add(a_load);
        fileMenu.getItems().add(new SeparatorMenuItem());
        addAlignmentProviderMenus(fileMenu);
        fileMenu.getItems().add(new SeparatorMenuItem());
        templateMenu = new Menu("Template");
        templateMenu.setId("Template");
        fileMenu.getItems().add(templateMenu);
        List<MyAction> templateActions = getTemplateActions();
        for (MenuItem a : templateActions) {
            templateMenu.getItems().add(a);
        }
        Menu workDirMenu = new Menu("Set working dir");
        fileMenu.getItems().add(workDirMenu);
        List<MenuItem> workDirMenuActions = getWorkDirActions();
        for (MenuItem a : workDirMenuActions) {
        	workDirMenu.getItems().add(a);
        }
        templateMenu.getItems().add(new SeparatorMenuItem());
        templateMenu.getItems().add(a_template);
        fileMenu.getItems().add(a_managePackages);
        fileMenu.getItems().add(a_clearClassPath);
        fileMenu.getItems().add(a_appLauncher);
        fileMenu.getItems().add(new SeparatorMenuItem());
        fileMenu.getItems().add(a_save);
        fileMenu.getItems().add(a_saveas);
        if (!Utils.isMac()) {
            fileMenu.getItems().add(new SeparatorMenuItem());
            fileMenu.getItems().add(a_close);
            fileMenu.getItems().add(a_quit);
        }
	}

	private void addAlignmentProviderMenus(Menu fileMenu) {
        List<BeautiAlignmentProvider> providers = doc.beautiConfig.alignmentProvider;

        for (BeautiAlignmentProvider provider : providers) {
            String providerInfo = provider.toString().replaceAll("Add ", "Add partition for ");

        	MenuItem action = new MyAction(provider.toString(), providerInfo, null, null) {

				@Override
				public void actionPerformed(ActionEvent e) {
		            try {
		                setCursor(Cursor.WAIT);

		                // get user-specified alignments
				        List<BEASTInterface> beastObjects = provider.getAlignments(doc);
				        if (beastObjects != null) {
					        for (BEASTInterface o : beastObjects) {
					        	if (o instanceof Alignment) {
					        		try {
					        			BeautiDoc.createTaxonSet((Alignment) o, doc);
					        		} catch(Exception ex) {
					        			ex.printStackTrace();
					        		}
					        	}
					        }
				        }

		                doc.connectModel();
		                doc.fireDocHasChanged();
		                
				        if (beastObjects != null) {
					        for (BEASTInterface o : beastObjects) {
					        	if (o instanceof MRCAPrior) {
				        			doc.addMRCAPrior((MRCAPrior) o);
					        	}
					        }
				        }
		                a_save.setDisable(false);
		                a_saveas.setDisable(false);
		            } catch (Exception exx) {
		                exx.printStackTrace();

		                String text = "Something went wrong importing the alignment:\n";
		                javafx.scene.control.TextArea textArea = new javafx.scene.control.TextArea(text);
		                //textArea.setColumns(30);
		                textArea.setWrapText(true);
		                //textArea.setWrapStyleWord(true);
		                textArea.appendText(exx.getMessage());
		                textArea.setPrefSize(textArea.getPrefWidth(), 1);
		                // textArea.setOpaque(false);
		                Alert.showMessageDialog(null, textArea,
		                        "Error importing alignment",
		                        Alert.WARNING_MESSAGE);
		            }
		            setCursor(Cursor.DEFAULT);
				}
			};
        	fileMenu.getItems().add(action);
        }
	}

	
	void setUpViewMenu() {
        m_viewPanelCheckBoxMenuItems = null;
        viewMenu.getItems().clear();
        for (int panelIndex = 0; panelIndex < doc.beautiConfig.panels.size(); panelIndex++) {
            final ViewPanelCheckBoxMenuItem viewPanelAction = new ViewPanelCheckBoxMenuItem(
                    panelIndex);
            viewPanelAction.setOnAction(ae -> {
                    viewPanelAction.doAction();
                });
            viewMenu.getItems().add(viewPanelAction);
        }
        viewMenu.getItems().add(new SeparatorMenuItem());
        viewMenu.getItems().add(a_viewall);
        
        viewMenu.getItems().add(new SeparatorMenuItem());
        viewMenu.getItems().add(a_viewMethodSection);
        viewMenu.getItems().add(new SeparatorMenuItem());

        // TODO: do we still need zoom?
//        MyAction zoomIn = new MyAction("Zoom in", "Increase font size of all components", null, new KeyCodeCombination(KeyCode.EQUALS)) {
//
//			@Override
//        	public void actionPerformed(ActionEvent ae) {
//				int size = UIManager.getFont("Label.font").getSize();
//            	Utils.setFontSize(size + 1);
//            	Utils.saveBeautiProperty("fontsize", (size + 1) + "");
//        		refreshPanel();
//        	}
//        };
//        MyAction zoomOut = new MyAction("Zoom out", "Decrease font size of all components", null, new KeyCodeCombination(KeyCode.MINUS)) {
//
//			@Override
//        	public void actionPerformed(ActionEvent ae) {
//				int size = UIManager.getFont("Label.font").getSize();
//            	Utils.setFontSize(Math.max(size - 1, 4));
//            	Utils.saveBeautiProperty("fontsize", Math.max(size - 1, 4) + "");
//        		refreshPanel();
//        	}
//        };
//        viewMenu.getItems().add(zoomIn);
//        viewMenu.getItems().add(zoomOut);

        List<String> themes = new ArrayList<>();
        themes.addAll(ThemeProvider.getThemeMap().keySet());
        String currentTheme = Utils.getBeautiProperty("theme");
        if (currentTheme == null) {
        	currentTheme = new Default().getThemeName();
        }
        Collections.sort(themes);
        
        List<RadioMenuItem> items = new ArrayList<>();
    	for (String name: themes) {
    		ThemeProvider provider = ThemeProvider.getThemeProvider(name);
    		if (name != null & name.length() > 0) {
    			RadioMenuItem item = new RadioMenuItem("Theme " + name);
    			Tooltip tooltip = new Tooltip("Choose " + name + " theme to skin BEAUti");
    			Tooltip.install(item.getGraphic(), tooltip);
				item.setOnAction(i -> {
					if (provider.loadMyStyleSheet(frame.getScene())) {
						Utils.saveBeautiProperty("theme", name);
						try {
							docHasChanged();
							refreshPanel();
						} catch (NoSuchMethodException | SecurityException | ClassNotFoundException
								| InstantiationException | IllegalAccessException | IllegalArgumentException
								| InvocationTargetException e) {
							e.printStackTrace();
						}
					}
				});
				viewMenu.getItems().add(item);
				item.setSelected(currentTheme.equals(name));
    		}
    	}
    }

    class TemplateAction extends MyAction {
        String m_sFileName;
        String templateInfo;

        public TemplateAction(File file) {
            super("xx", file.getAbsolutePath(), null, null);
            m_sFileName = file.getAbsolutePath();
            String fileSep = System.getProperty("file.separator");
            if (fileSep.equals("\\")) {
                fileSep = "\\";
            }
            int i = m_sFileName.lastIndexOf(fileSep) + 1;
            String name = m_sFileName.substring(
                    i, m_sFileName.length() - 4);
            //putValue(Action.NAME, name);
            setText(name);
            setId(name.replaceAll(" ", ""));
            //((Label)getContent()).setText(name);
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
        		Tooltip tooltip = new Tooltip(templateInfo);
//        		Tooltip.install(getContent(), tooltip);		
            } catch (Exception e) {
                // ignore
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                if (doc.validateModel() == DOC_STATUS.NO_DOCUMENT) {
                    doc.loadNewTemplate(m_sFileName);
                } else if (Alert.showConfirmDialog(frame.getScene().getRoot(),
                        "Changing templates means the information input so far will be lost. "
                                + "Are you sure you want to change templates?",
                        "Are you sure?", Alert.YES_NO_CANCEL_OPTION) == Alert.YES_OPTION) {
                    doc.loadNewTemplate(m_sFileName);
                }
                createFileMenu();
            } catch (Exception ex) {
                ex.printStackTrace();
                Alert.showMessageDialog(
                        null,
                        "Something went wrong loading the template: "
                                + ex.getMessage());
            }
        }

    }

	private List<MyAction> getTemplateActions() {
        List<MyAction> actions = new ArrayList<>();
        List<String> beastDirectories = PackageManager.getBeastDirectories();
        for (String dirName : beastDirectories) {
            File dir = new File(dirName + "/" + BeautiConfig.TEMPLATE_DIR);
            getTemplateActionForDir(dir, actions);
        }
        return actions;
    }

    private void getTemplateActionForDir(File dir, List<MyAction> actions) {
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
                            	for (MyAction action : actions) {
                            		//String name = ((Label)action.getContent()).getText();
                            		String name = action.getText();
                            		if (name.equals(fileName)) {
                            			duplicate = true;
                            		}
                            	}
                            	if (!duplicate) {
                            		actions.add(new TemplateAction(template));
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

    private List<MenuItem> getWorkDirActions() {
        List<MenuItem> actions = new ArrayList<>();
        List<String> beastDirectories = PackageManager.getBeastDirectories();
        Set<String> doneDirs = new HashSet<>();
        for (String dir : beastDirectories) {
        	if (!doneDirs.contains(dir)) {
	        	doneDirs.add(dir);
	        	String exampledir = dir + File.separator+ "examples";
	        	if (new File(exampledir).exists()) {
		            String workDirInfo = "Set working directory to " + dir;
		            String name = dir;
		            if (name.indexOf(File.separator) >= 0) {
		            	name = dir.substring(dir.lastIndexOf(File.separator) + 1);
		            }
		            MenuItem menu = new MenuItem(name);
	        		menu.setOnAction(e->ProgramStatus.setCurrentDir(dir));
//	        		Tooltip tooltip = new Tooltip(workDirInfo);
//	        		Tooltip.install(menu.getContent(), tooltip);
		            actions.add(menu);
	        	}
        	}
        }
        return actions;
    }

    void setMenuVisibiliy(String parentName, MenuItem c) {
    	
        String name = "";
        if (c instanceof Menu) {
            name = ((Menu) c).getText();
        } else if (c instanceof MenuItem) {
            name = ((MenuItem) c).getText();
        }
        if (name == null) {
            c.setVisible(false);
            return;
        }
        if (name.length() > 0
                && doc.beautiConfig.menuIsInvisible(parentName + name)) {
            c.setVisible(false);
        }
        if (c instanceof Menu) {
            for (MenuItem x : ((Menu) c).getItems()) {
                setMenuVisibiliy(parentName + name
                        + (name.length() > 0 ? "." : ""), x);
            }
        } else if (c instanceof Menu) {
            for (int i = 0; i < ((Menu) c).getItems().size(); i++) {
                setMenuVisibiliy(parentName, ((Menu) c).getItems().get(i));
            }
        }
    }

    public void setUpPanels() throws NoSuchMethodException, SecurityException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    	if (isInitialising) {
    		//return;
    	}
        isInitialising = true;
        // remove any existing tabs
        if (getTabs().size() > 0) {
        	getTabs().clear();
            isPaneIsVisible = new boolean[doc.beautiConfig.panels.size()];
            Arrays.fill(isPaneIsVisible, true);
        }
        for (int panelIndex = 0; panelIndex < doc.beautiConfig.panels.size(); panelIndex++) {
            BeautiPanelConfig panelConfig = doc.beautiConfig.panels.get(panelIndex);
            isPaneIsVisible[panelIndex] = panelConfig.isVisibleInput.get();
        }
        // add panels according to BeautiConfig
        panels = new BeautiPanel[doc.beautiConfig.panels.size()];
        for (int panelIndex = 0; panelIndex < doc.beautiConfig.panels.size(); panelIndex++) {
            BeautiPanelConfig panelConfig = doc.beautiConfig.panels.get(panelIndex);
            panels[panelIndex] = new BeautiPanel(panelIndex, panelConfig.getName(), this.doc, panelConfig);
            getTabs().add(panels[panelIndex]);
            getTabs().get(panelIndex).setTooltip(new Tooltip(panelConfig.getTipText()));
            // getTabs().get(panelIndex).setText(panelConfig.getName());
            getTabs().get(panelIndex).setId(panelConfig.getName().replaceAll(" ",""));
            getTabs().get(panelIndex).onSelectionChangedProperty().set(e -> {
            	for (int i = 0; i < panels.length; i++) {
            		panels[i].setHMCVisible(false);
            	}
            	((BeautiPanel)e.getSource()).setHMCVisible(true);
            });
        }
        setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);

        for (int panelIndex = doc.beautiConfig.panels.size() - 1; panelIndex >= 0; panelIndex--) {
            if (!isPaneIsVisible[panelIndex]) {
                getTabs().remove(panelIndex);
            }
        }
        isInitialising = false;
    }

    /**
     * record number of frames. If the last frame is closed, exit the app. *
     */
    static int BEAUtiIntances = 0;
    @Deprecated // do not use since this causes an IllegalStateException: Toolkit not initialized
    // when called outside BEAUti. use `ProgramStatus.name.equals("BEAUti")` instead.
    static public boolean isInBeauti() {
    	return ProgramStatus.name.equals("BEAUti");
    	// return BEAUtiIntances > 0;
    }

    private static String usage() {
        return "java Beauti [options]\n" + "where options can be one of the following:\n"
                + "-template [template file] : BEAUti template to be used. Default " + BeautiConfig.TEMPLATE_DIR + "/Standard.xml\n"
        		+ "-nex [nexus data file] : nexus file to be read using template, multiple -nex arguments are allowed\n"
                + "-xmldat [beast xml file] : as -nex but with BEAST 1 or 2 xml file instead of nexus file\n"
                + "-xml [beast file] : BEAST 2 XML file to be loaded\n"
                + "-exitaction [writexml|usetemplate|usexml] : what to do after processing arguments\n"
                + "-out [output file name] : file to be written\n"
                + "-capture : captures stdout and stderr and make them available under Help/Messages menu\n"
                + "-v, -version : print version\n"
                + "-h, -help : print this help message\n";
    }

   

    public static BeautiDoc initialise(String[] args) {
    	ProgramStatus.name = "BEAUti";

    	ByteArrayOutputStream baos = null;
        for (String arg : args) {
        	if (arg.equals("-v") || arg.equals("-version")) {
                System.out.println((new BEASTVersion2()).getVersionString());
                System.exit(0);
        	}
        	if (arg.equals("-h") || arg.equals("-help")) {
                System.out.println(usage());
                System.exit(0);
        	}
        	if (arg.equals("-capture")) {
        		final PrintStream beautiLog = System.err;
            	baos = new ByteArrayOutputStream() {
            		@Override
            		public synchronized void write(byte[] b, int off, int len) {
            			super.write(b, off, len);
            			beautiLog.write(b, off, len);
            		};

            		@Override
            		public synchronized void write(int b) {
            			super.write(b);
            			beautiLog.write(b);
            		};

            		@Override
            		public void write(byte[] b) throws java.io.IOException {
            			super.write(b);
            			beautiLog.write(b);
            		};

            		@Override
            		public void flush() throws java.io.IOException {
            			super.flush();
            			beautiLog.flush();
            		};

            		@Override
            		public void close() throws IOException {
            			super.close();
            			beautiLog.close();
            		}
            	};

            	PrintStream p = new PrintStream(baos);
            	System.setOut(p);
            	System.setErr(p);
            	Log.err = p;
            	Log.warning = p;
            	Log.info = p;
            	Log.debug = p;
            	Log.trace = p;
        	}
        }

        BeautiDoc.baos = baos;

        BeautiDoc doc;
        try {
			PackageManager.loadExternalJars();
            int i=0;
	        while (i < args.length) {
	        	if (i < args.length-1 && args[i].equals("-version_file")) {
	        		args[i] = "";
                    i++;
	        		while (i < args.length && !args[i].startsWith("-")) {
	        			BEASTClassLoader.addServices(args[i]);
		        		args[i] = "";
                        i++;
	        		}
	        	} else if (i < args.length-1 && args[i].equals("-packagedir")) {
	        		args[i] = "";
	        		i++;
	                String dir = args[i];
	                System.setProperty("beast.user.package.dir", dir);
	        		args[i] = "";
                    i++;
	            }
                i++;
	        }
			if (Utils.isMac()) {
			  	Utils.loadUIManager();
			}
			BEASTObjectPanel.init();

            // make sure templates know we are in BEAUti while parsing arguments 
            BeautiTabPane.BEAUtiIntances++;

            doc = new BeautiDoc();
        	if (doc.parseArgs(args) == ActionOnExit.WRITE_XML) {
			    return null;
			}
            // reset instances
            BeautiTabPane.BEAUtiIntances--;

        } catch (XMLParserException | SAXException | IOException | ParserConfigurationException e) {
			e.printStackTrace();
			return null;
		}

        return doc;
    }    
    
    public static BeautiTabPane main2(String[] args, Stage primaryStage) {
    	BeautiDoc doc = initialise(args);
    	return main2(args, primaryStage, doc);    	
    }
    
    public static BeautiTabPane main2(String[] args, Stage primaryStage, BeautiDoc doc) {
    	
    	ProgramStatus.name = "BEAUti";
    	
    	// retrieve previously stored working directory
    	String currentDir = Utils.getBeautiProperty("currentDir");
    	if (currentDir != null && currentDir.length() > 1) {
    		ProgramStatus.g_sDir = currentDir;
    	}
    	
        try {


            final BeautiTabPane beauti = new BeautiTabPane(doc);

            beauti.setUpPanels();

            beauti.currentTab = beauti.panels[0];
            beauti.currentTab.setHMCVisible(true);

            beauti.getSelectionModel().selectedItemProperty().addListener(e -> {
                    if (beauti.currentTab == null) {
                        beauti.currentTab = beauti.panels[0];
                    }
                    if (beauti.currentTab != null) {
                        if (!beauti.isInitialising) {
                            beauti.currentTab.config
                                    .sync(beauti.currentTab.partitionIndex);
                        }
                        BeautiPanel panel = (BeautiPanel) beauti.getSelectionModel().getSelectedItem();
                        beauti.currentTab = panel;
                        beauti.refreshPanel();
                    }
            });

            beauti.setVisible(true);
            beauti.refreshPanel();
            Stage frame = primaryStage;
            primaryStage.setTitle("BEAUti");
            beauti.frame = primaryStage;

            frame.setTitle("BEAUti 3: " + doc.getTemplateName()
                    + " " + doc.getFileName());
            beauti.frame = frame;
        	URL url = BEASTClassLoader.getSystemClassLoader().getResource("beast/pkgmgmt/icons/beauti.png");
    		ImageView icon = new ImageView(url.toExternalForm());
            if (icon != null) {
                frame.getIcons().add(icon.getImage());
            }

            MenuBar menuBar = beauti.createMenuBar();
            if (Utils.isMac() && !(System.getProperty("beast.is.junit.testing") != null)) {
            	menuBar.useSystemMenuBarProperty().set(true);
            }

            if (doc.getFileName() != null || doc.alignments.size() > 0) {
                beauti.a_save.setDisable(false);
                beauti.a_saveas.setDisable(false);
            }

            BorderPane vb = new BorderPane();
            vb.setTop(menuBar);
            vb.setCenter(beauti);
            vb.setPrefSize(1024, 768);
            
            Scene scene = new Scene(vb);
            ThemeProvider.loadStyleSheet(scene);
            
            frame.setScene(scene);
            frame.setX(BEAUtiIntances * 10);
            frame.setY(10 + BEAUtiIntances * 10);

            // check file needs to be save on closing main frame
            frame.setOnCloseRequest(e-> {
                    if (!beauti.quit()) {
                        e.consume();
                        return;
                    }
                    Stage frame0 = (Stage) e.getSource();
                    frame0.close();
                    BEAUtiIntances--;
                    if (BEAUtiIntances == 0) {
                        System.exit(0);
                    }
            });

            
            
            primaryStage.widthProperty().addListener((obs, oldVal, newVal) -> {
                beauti.currentTab.setWidth(oldVal, newVal);
            });
            primaryStage.heightProperty().addListener((obs, oldVal, newVal) -> {
                beauti.currentTab.setHeight(oldVal, newVal);
            });

            beauti.setId("BeautiTabPane");
                                  
            return beauti;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    } // main2

    
    public CheckMenuItem get_autoSetClockRate() {
    	return autoSetClockRate;
    }
    
    public CheckMenuItem get_allowLinking() {
    	return allowLinking;
    }
    
    public CheckMenuItem get_autoUpdateFixMeanSubstRate() {
    	return autoUpdateFixMeanSubstRate;
    }
    

	@Override
	public void autoSetClockRate(boolean flag) {
		autoSetClockRate.setSelected(flag);
	}

	@Override
	public void allowLinking(boolean flag) {
		allowLinking.setSelected(flag);		
	}

	@Override
	public void autoUpdateFixMeanSubstRate(boolean flag) {
		autoUpdateFixMeanSubstRate.setSelected(flag);		
	}

	@Override
	public BeautiPanel getCurrentPanel() {
		return currentTab;
	}

	
	
	
	

} // class Beauti

