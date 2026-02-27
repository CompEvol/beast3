package beastfx.app.tools;


import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;


import beast.base.core.ProgramStatus;
import beastfx.app.util.Alert;
import beastfx.app.util.FXUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.util.converter.IntegerStringConverter;

public class LogCombinerController implements Initializable {

    private final ObservableList<FileInfo> files = FXCollections.observableArrayList();
    private File outputFile = null;

    @FXML
    public TableView<FileInfo> filesTable = null;
	
	@FXML
	public ComboBox<String> fileTypeCombo;
	
	@FXML
	public CheckBox decimalCheck;
	
	@FXML
	public CheckBox renumberOutput;
	
	//@FXML
	//public CheckBox resampleCheck;
	
	@FXML
	public ComboBox<String> resampleCombo;
	
	@FXML
	public TextField resampleText;

    @FXML
    public Button addButton;

    @FXML
    public Button delButton;

    @FXML
    public Button fillDownButton;
    
    @FXML
    public TextField fileNameText;
    
    @FXML
    public Button browseButton;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		fileTypeCombo.setItems(FXCollections.observableArrayList(new String[] {"Log Files", "Tree Files"}));
		fileTypeCombo.getSelectionModel().select(0);
		
		browseButton.setOnAction(e -> {
            File file = isTreeFiles() ?
            		FXUtils.getSaveFile("Select output file...", new File(ProgramStatus.g_sDir), "Beast log and tree files", "trees", "trees") :
            		FXUtils.getSaveFile("Select output file...", new File(ProgramStatus.g_sDir), "Beast log and tree files", "log", "log");
	        if (file == null) {
	            // the dialog was cancelled...
	            return;
	        }
	        outputFile = file;
	        String fileName = file.getAbsolutePath();
	        if (fileName.lastIndexOf(File.separator) > 0) {
	        	ProgramStatus.setCurrentDir(fileName.substring(0, fileName.lastIndexOf(File.separator)));
	        }
	        fileNameText.setText(outputFile.getName());
		});
		
		
		resampleText.setDisable(true);
		resampleCombo.setItems(FXCollections.observableArrayList(new String[] {"No resampling", "Resample states at lower frequency", "Include every"}));
		resampleCombo.getSelectionModel().select(0);
		resampleCombo.setOnAction(e -> {
            resampleText.setDisable(resampleCombo.getSelectionModel().getSelectedIndex() == 0);
        });

		addButton.setOnAction(e-> {
            File[] files = isTreeFiles() ?
            		FXUtils.getLoadFiles("Select log file", new File(ProgramStatus.g_sDir), "Trace or tree log files", "trees", "trees") :
            		FXUtils.getLoadFiles("Select log file", new File(ProgramStatus.g_sDir), "Trace or tree log files", "log", "log");
            if (files != null) {
                addFiles(files);
            }			
		});
		
		filesTable.setItems(files);
		filesTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		filesTable.getSelectionModel().selectedItemProperty().addListener(
        	    (observable, oldValue, newValue) -> {
        	    	filesTableSelectionChanged();
        	    }
        );

        delButton.setDisable(true);
        delButton.setOnAction(e-> {
        	List<FileInfo> itemsToDelete = filesTable.getSelectionModel().getSelectedItems();
        	files.removeAll(itemsToDelete);
        	filesTable.refresh();
            filesTableSelectionChanged();
        });

        fillDownButton.setDisable(true);
        fillDownButton.setOnAction(e->{
        	List<Integer> selected = filesTable.getSelectionModel().getSelectedIndices();
        	if (selected.size() == 0) {
        		return;
        	}
        	int burnin = files.get(selected.get(0)).getBurnin();
        	for (int i : selected) {
        		files.get(i).setBurnin(burnin);
        	}        	
        	filesTable.refresh();
        });
        fillDownButton.setTooltip(new Tooltip("Set all burnin value the same as first one selected"));
        
        
        filesTable.setEditable(true);

        // set up table columns
        TableColumn<FileInfo, String> col = new TableColumn<>("File");
        col.setPrefWidth(500);
        col.setEditable(true);
        col.setCellValueFactory(
        	    new PropertyValueFactory<FileInfo, String>("File")
        	);
        filesTable.getColumns().add(col);

        TableColumn<FileInfo,Integer> col2 = new TableColumn<>("Burnin");
        col2.setPrefWidth(70);
        col2.setEditable(true);
        col2.setCellValueFactory(
        	    new PropertyValueFactory<FileInfo, Integer>("Burnin")
        	);
        col2.setCellFactory(
        		TextFieldTableCell.forTableColumn(new IntegerStringConverter())
        	);
        col2.setOnEditCommit(
                new EventHandler<CellEditEvent<FileInfo, Integer>>() {
					@Override
					public void handle(CellEditEvent<FileInfo, Integer> event) {
						Integer newValue = event.getNewValue();
						FileInfo tipDate = event.getRowValue();
						tipDate.setBurnin(newValue);
					}
				}
            );        
        filesTable.getColumns().add(col2);

        // initial message in table view
        filesTable.setPlaceholder(new Label("No files selected yet. Drag and drop files here\nor select the '+' button below to add files"));

        // drag/drop file support
        filesTable.setOnDragOver(new EventHandler<DragEvent>() {
            public void handle(DragEvent event) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                event.consume();
            }
        });
        
        filesTable.setOnDragDropped((DragEvent event) -> {
            Dragboard db = event.getDragboard();
        	if (db.hasFiles()) {
        		for (File file : db.getFiles()) {
                    FileInfo fileInfo = new FileInfo(file);
                    this.files.add(fileInfo);
        		}
        		filesTable.refresh();
            } else {
                event.setDropCompleted(false);
            }
            event.consume();
        });        

	}
	
    private void filesTableSelectionChanged() {
    	int selected = filesTable.getSelectionModel().getSelectedItems().size();
        delButton.setDisable(selected == 0);
        fillDownButton.setDisable(selected <= 1);
    }

    private void addFiles(File[] fileArray) {
        int sel1 = files.size();
        for (File file : fileArray) {
            FileInfo fileInfo = new FileInfo(file);

            files.add(fileInfo);

            String fileName = file.getAbsolutePath();
            if (fileName.lastIndexOf(File.separator) > 0) {
            	ProgramStatus.setCurrentDir(fileName.substring(0, fileName.lastIndexOf(File.separator)));
            }
        }

        filesTable.refresh();

        int sel2 = files.size();
        filesTable.getSelectionModel().selectRange(sel1-1, sel2);
        filesTable.refresh();
    }	
    	
    public String[] getFileNames() {
        String[] fileArray = new String[files.size()];
        for (int i = 0; i < files.size(); i++) {
            FileInfo fileInfo = files.get(i);
            fileArray[i] = fileInfo.getFile();
        }
        return fileArray;
    }

    public int[] getBurnins() {
        int[] burnins = new int[files.size()];
        for (int i = 0; i < files.size(); i++) {
            FileInfo fileInfo = files.get(i);
            burnins[i] = fileInfo.getBurnin();
        }
        return burnins;
    }

    public boolean isTreeFiles() {
        return fileTypeCombo.getSelectionModel().getSelectedIndex() == 1;
    }

    public boolean convertToDecimal() {
        return decimalCheck.isSelected();
    }

    public boolean renumberOutputStates() {
        return renumberOutput.isSelected();
    }

    public int resampleComboState() {
        return resampleCombo.getSelectionModel().getSelectedIndex();
    }

    public int getResampleFrequency() {
    	try {
    		return Integer.parseInt(resampleText.getText());
    	} catch (NumberFormatException e) {
    		Alert.showMessageDialog(null, "Could not read the number in the resample field: " + e.getMessage());
    		throw new RuntimeException(e);
    	}
    }

    public String getOutputFileName() {
        if (outputFile == null) {
        	if (fileNameText.getText() != null && fileNameText.getText().trim().length() > 0) {
        		return fileNameText.getText();
        	} else {
        		return null;
        	}
        }
        return outputFile.getPath();
    }

}
