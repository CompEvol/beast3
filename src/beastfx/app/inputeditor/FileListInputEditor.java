package beastfx.app.inputeditor;





import java.awt.Color;
import javax.swing.UIManager;
import javax.swing.border.Border;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.BorderPane;

import beastfx.app.util.Utils;
import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.core.ProgramStatus;
import beastfx.app.util.FXUtils;


/** for opening files for reading
 * use OutFile when you need a file for writing
 */
public class FileListInputEditor extends ListInputEditor {

	final static String SEPARATOR = Utils.isWindows() ? "\\\\" : "/";
	
	
	public class File0 {
		SimpleStringProperty name;
		File file;
		File0(File file) {
			this.file = file;
			name = new SimpleStringProperty(file.getPath());
		}
		
		public String getFile() {
			return name.get();
		}

		public void setFile(String fname) {
			name.set(fname);
			file = new File(fname);
		}
	}

	TableView<File0> filesTable = null;
    // private FilesTableModel filesTableModel = null;
    private List<File> files;
    private ObservableList<File0> files0;

	@Override
	public Class<?> type() {
		return List.class;
	}
	
    @Override
    public Class<?> baseType() {
		return File.class;
    }


	public FileListInputEditor(BeautiDoc doc) {
		super(doc);
	}

	public FileListInputEditor() {
		super();
	}

	@Override
	public void init(Input<?> input, BEASTInterface beastObject, int itemNr, ExpandOption isExpandOption, boolean addButtons) {
        m_bAddButtons = addButtons;
        m_bExpandOption = isExpandOption;
        m_input = input;
        m_beastObject = beastObject;
		// super.init(input, beastObject, itemNr, isExpandOption, addButtons);
		pane = FXUtils.newHBox();
		Object o = input.get();
		if (o instanceof List) {
			List<File0> o2 = new ArrayList<>();
			files = (List<File>) o;
			for (File f : files) {
				o2.add(new File0(f));
			}
			files0 = FXCollections.observableArrayList(o2);
		}
		pane.getChildren().add(fileListPanel());
		
		filesTable.setItems(files0);
				
		getChildren().add(pane);
	}

	@Override
	protected void setValue(Object o) {
		String file = o.toString();
		if (file.equals("")) {
			return;
		}
		String fileSep = System.getProperty("file.separator");
		String origFile = null;
		try {
			origFile = ((File) m_input.get()).getAbsolutePath();
		} catch (Exception e) {
			origFile = null;
		}
		if (origFile != null && origFile.indexOf(fileSep) >= 0 && file.indexOf(fileSep) < 0) {
			if (origFile.contains(origFile)) {
				file = origFile.substring(0, origFile.lastIndexOf(fileSep) + 1) + file;
			}
		}
		m_input.setValue(file, m_beastObject);	
   	}
	

	static File getDefaultFile(File file) {
		File defaultFile;
		if (file.exists()) {
			defaultFile = file;
			if (defaultFile.getParent() == null) {
				defaultFile = new File(ProgramStatus.g_sDir);
				if (defaultFile.isDirectory()) {
					defaultFile = new File(ProgramStatus.g_sDir + FileListInputEditor.SEPARATOR + file.getName());
				} else {
					defaultFile = new File(new File(ProgramStatus.g_sDir).getParent() + FileListInputEditor.SEPARATOR + file.getName());
				}
			}
		} else {
			defaultFile = new File(ProgramStatus.g_sDir);
			if (defaultFile.isDirectory()) {
				defaultFile = new File(ProgramStatus.g_sDir + FileListInputEditor.SEPARATOR + file.getName());
			} else {
				defaultFile = new File(new File(ProgramStatus.g_sDir).getParent() + FileListInputEditor.SEPARATOR + file.getName());
			}
		}
		return defaultFile;
	}
	
	/** to be overridded by file editors that produce specific file types **/
	protected File newFile(File file) {
		return file;
	}

	
	
	private ActionPanel actionPanel1;

    public Pane fileListPanel() {

    	BorderPane panel = new BorderPane();//new BorderLayout());
        // panel.setOpaque(false);

        // Taxon Sets
        // filesTableModel = new FilesTableModel();
        filesTable = new TableView();//filesTableModel);
        filesTable.setPrefWidth(500);
        filesTable.setEditable(true);

        TableColumn col = new TableColumn<>("Files");
        col.setPrefWidth(500);
        col.setCellValueFactory(
        	    new PropertyValueFactory<File0,String>("File")
        	);
        filesTable.getColumns().add(col);
        
//        filesTable.getColumnModel().getColumn(0).setCellRenderer(
//                new TableRenderer(SwingConstants.LEFT, new Insets(0, 4, 0, 4)));
//        filesTable.getColumnModel().getColumn(0).setPreferredWidth(120);
        //filesTable.getColumnModel().getColumn(0).setPreferredWidth(80);

        // This causes superfluous TabelModel.setValue events to fire.
        // Is this still needed?  I guess we'll see...
        //TableEditorStopper.ensureEditingStopWhenTableLosesFocus(filesTable);

        filesTable.getSelectionModel().selectedIndexProperty().
        	addListener(e ->filesTableSelectionChanged());

        ScrollPane scrollPane1 = new ScrollPane(filesTable);
                //ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                //ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        //scrollPane1.setMaxSize(10000, 10);
        scrollPane1.setPrefSize(500, 285);

        actionPanel1 = new ActionPanel();
        actionPanel1.setAddAction(addFileAction);
        actionPanel1.setRemoveAction(removeFileAction);
        actionPanel1.delButton.setDisable(true);

        FlowPane controlPanel1 = new FlowPane(Orientation.HORIZONTAL);
        controlPanel1.getChildren().add(actionPanel1);

        // panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        Label label = new Label(formatName(m_input.getName()) + ":");
        label.setTooltip(new Tooltip(m_input.getTipText()));
        panel.setTop(label);//, BorderLayout.NORTH);
        panel.setCenter(scrollPane1);//, BorderLayout.CENTER);
        panel.setBottom(actionPanel1);//, BorderLayout.SOUTH);
        scrollPane1.setTooltip(new Tooltip(m_input.getTipText()));

        Color focusColor = UIManager.getColor("Focus.color");
        Border focusBorder = BorderFactory.createMatteBorder(2, 2, 2, 2, focusColor);
//        new FileDrop(null, scrollPane1, focusBorder, new FileDrop.Listener() {
//            @Override
//			public void filesDropped(java.io.File[] files) {
//                addFiles(files);
//            }   // end filesDropped
//        }); // end FileDrop.Listener
        
        filesTable.setOnDragOver(new EventHandler<DragEvent>() {
            public void handle(DragEvent event) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                event.consume();
            }
        });
        
        filesTable.setOnDragDropped((DragEvent event) -> {
            Dragboard db = event.getDragboard();
        	if (db.hasFiles()) {
            	List<File> files = db.getFiles();
            	addFiles(files.toArray(new File[] {}));
        		filesTable.refresh();
            } else {
                event.setDropCompleted(false);
            }
            event.consume();
        });        
        return panel;
    }


    private void filesTableSelectionChanged() {
        if (filesTable.getSelectionModel().getSelectedIndices().size() == 0) {
            actionPanel1.delButton.setDisable(true);
        } else {
            actionPanel1.delButton.setDisable(false);
        }
    }

    private void addFiles(File[] fileArray) {
        int sel1 = files.size();
        for (File file : fileArray) {        	
			try {
	        	File File = (File) baseType().getConstructor(String.class).newInstance(file.getAbsolutePath());
	            files.add(File);
	            files0.add(new File0(file));
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				e.printStackTrace();
			};

            String fileName = file.getAbsolutePath();
            if (fileName.lastIndexOf(File.separator) > 0) {
            	ProgramStatus.setCurrentDir(fileName.substring(0, fileName.lastIndexOf(File.separator)));
            }
        }

        // filesTableModel.fireTableDataChanged();

        int sel2 = files.size() - 1;
        filesTable.getSelectionModel().selectRange(sel1, sel2);

    }

    EventHandler<ActionEvent> addFileAction = new EventHandler<ActionEvent>() {

    	@Override
    	public void handle(ActionEvent event) {
            File[] files = FXUtils.getLoadFiles("Select file", new File(ProgramStatus.g_sDir), "XML, trace or tree log files", "log", "trees", "xml");
            if (files != null) {
                addFiles(files);
            }
    		
    	};
	};

    EventHandler<ActionEvent> removeFileAction = new EventHandler<ActionEvent>() {

    	@Override
    	public void handle(ActionEvent event) {
            int row = filesTable.getSelectionModel().getSelectedIndex();
            if (row != -1) {
                files.remove(row);
                filesTable.getItems().remove(row);
            }

            if (row >= files.size()) row = files.size() - 1;
            if (row >= 0) {
                filesTable.getSelectionModel().select(row);
            }
        }
    };



}
