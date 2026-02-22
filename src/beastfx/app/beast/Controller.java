package beastfx.app.beast;


import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import beagle.BeagleFlag;
import beagle.BeagleInfo;
import beast.base.core.Log;
import beast.base.util.Randomizer;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class Controller implements Initializable {
	Dialog<String> dialog = null;
	TextArea textView;

	@FXML
	private TextField inputFile;

	@FXML
	private TextField seed;


	@FXML
	private Button runButton;
	@FXML
	private Button beagleInfoButton;
	
	@FXML
	private ChoiceBox<Integer> threads;
	@FXML
	private ChoiceBox<Integer> instances;

	@FXML
	private ChoiceBox<String> logFileMode;
	@FXML
	private ChoiceBox<Log.Level> logLevel;
	@FXML
	private ChoiceBox<String> precision;
	@FXML
	private ChoiceBox<String> scaling;
	@FXML
	private ChoiceBox<String> beagle;

	private final String[] logFileModes = new String[] { "default: only write new log files", "overwrite: overwrite log files",
			"resume: appends log to existing files (if any)" };
	private final String[] precisions = new String[] {"Double", "Single"};
	private final String[] scalings = new String[] {"Default", "None", "Dynamic", "Always"};
	private final String[] beagles = new String[] {"Automatic", "java", "CPU", "SSE", "GPU"};

	private Stage stage;

	private Thread beastThread = null;

	void setStage(Stage stage) {
		this.stage = stage;
	}

	@FXML
	void chooseFile() {
		FileChooser fc = new FileChooser();
		fc.setTitle("Choose BEAST input file");

		fc.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("BEAST input files", "*.xml", "*.json"),
				new FileChooser.ExtensionFilter("All files", "*.*"));
		File file = fc.showOpenDialog(stage);
		if (file != null) {
			inputFile.setText(file.getPath());
		}
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		this.seed.setText(Randomizer.nextLong() + "");
		//this.useBeagle.setSelected(true);
		this.inputFile.setText("");
		this.logFileMode.setItems(FXCollections.observableArrayList(logFileModes));
		logFileMode.setValue(logFileModes[0]);

		logLevel.setItems(FXCollections.observableArrayList(Log.Level.values()));
		logLevel.setValue(Log.Level.info);

		precision.setItems(FXCollections.observableArrayList(precisions));
		precision.setValue(precisions[0]);
		
		scaling.setItems(FXCollections.observableArrayList(scalings));
		scaling.setValue(scalings[0]);

		beagle.setItems(FXCollections.observableArrayList(beagles));
		beagle.setValue(beagles[0]);
		
		BeastMain.printTitle();

		
		Set<Integer> set = IntStream.range(1, 21).boxed().collect(Collectors.toSet());
		threads.setItems(FXCollections.observableArrayList(set));
		threads.setValue(1);
		
		instances.setItems(FXCollections.observableArrayList(set));
		instances.setValue(1);
		
		beagle.setOnAction(e->{
			String beagle = this.beagle.getSelectionModel().getSelectedItem();
			if (beagle.equals(beagles[0]) || beagle.equals(beagles[1])) {
				precision.setDisable(true);
				scaling.setDisable(true);
			} else {
				precision.setDisable(false);
				scaling.setDisable(false);
			}
		});
		precision.setDisable(true);
		scaling.setDisable(true);
		
		
		new Thread() {
			public void run() {
				try {
					sleep(2000);
					// clear backlog if any
					logToView("    ", "red");
				} catch (InterruptedException e) {
				}
			};
		}.start();
	}

	@FXML
	void quit() {
		Log.warning("Quiting BEAST");
		System.exit(0);
	}
	
	@FXML
	void run() {
		if (runButton.getText().equals("Quit")) {
			System.exit(0);
		}
		
		beagleInfoButton.setText("Close dialog");
		
		if (beastThread == null) {
			logToView("Running file " + inputFile.getText(), "green");

			final List<String> MCMCargs = getMCMCargs();
			final BeastMCMC beastMCMC = new BeastMCMC();

			beastThread = new Thread() {
				public void run() {
					try {
						// set all the settings...
						beastMCMC.parseArgs(MCMCargs.toArray(new String[0]));
						beastMCMC.run();
					} catch (Exception e) {
						e.printStackTrace();
					}
					Platform.runLater(new Runnable() {
						public void run() { 
							beastThread = null;
							closeDialog();
							textView.setScrollTop(Double.MAX_VALUE);
							//runButton.setText("Quit");
						}
					});
				};
			};
			beastThread.start();
			runButton.setText("Stop");
		} else {
			beastThread.stop();
			beastThread = null;
			closeDialog();
			// runButton.setText("Quit");
		}
	}

	private List<String> getMCMCargs() {
		List<String> MCMCArgs = new ArrayList<>();
		MCMCArgs.add("-seed");
		MCMCArgs.add(seed.getText());
		MCMCArgs.add("-threads");
		MCMCArgs.add(threads.getSelectionModel().getSelectedItem() + "");
        System.setProperty("beast.instance.count", instances.getSelectionModel().getSelectedItem() +"");

		String beagle = this.beagle.getSelectionModel().getSelectedItem();
		if (!beagle.equals(beagles[0])) {
			if (beagle.equals(beagles[1])) {
	            System.setProperty("java.only", "true");
			} else {
				long beagleFlags = 0;
				if (beagle.equals(beagles[2])) {
		            beagleFlags |= BeagleFlag.PROCESSOR_CPU.getMask();
				} else if (beagle.equals(beagles[3])) {
		            beagleFlags |= BeagleFlag.VECTOR_SSE.getMask();
				} else {
		            beagleFlags |= BeagleFlag.PROCESSOR_GPU.getMask();
					if (precision.getSelectionModel().getSelectedItem().equals(precisions[1])) {
			            beagleFlags |= BeagleFlag.PRECISION_SINGLE.getMask();
					} else {
			            beagleFlags |= BeagleFlag.PRECISION_DOUBLE.getMask();
					}
				}
				
				
				switch (scaling.getSelectionModel().getSelectedItem()) {
				case "Default":
		            beagleFlags |= BeagleFlag.SCALING_AUTO.getMask();
					break;
				case "None":
		            beagleFlags |= BeagleFlag.SCALING_MANUAL.getMask();
					break;
				case "Dynamic":
		            beagleFlags |= BeagleFlag.SCALING_DYNAMIC.getMask();
					break;
				case "Always":
		            beagleFlags |= BeagleFlag.SCALING_ALWAYS.getMask();
					break;
				}
	            System.setProperty("beagle.preferred.flags", Long.toString(beagleFlags));
	        }
		}

		
		switch (this.logFileMode.getSelectionModel().getSelectedIndex()) {
		case 0:
			MCMCArgs.add("-batch");
			break;
		case 1:
			MCMCArgs.add("-overwrite");
			break;
		case 2:
			MCMCArgs.add("-resume");
			break;
		}

		Log.setLevel(logLevel.getSelectionModel().getSelectedItem());
		
		MCMCArgs.add(inputFile.getText());
		
		// ensure output directory = where input file resides
		try {
			File file = new File(inputFile.getText());
			System.setProperty("file.name.prefix", file.getParentFile().getAbsolutePath() + File.separator);
		} catch (Throwable t) {
			// file probably does not exist: ignore, since this will be picked up later when opening the file
		}

		return MCMCArgs;
	}

	class Message {
		public Message(String data, String style) {
			this.data = data;
			this.style = style;
		}
		
		String data; 
		String style;
	};
	List<Message> backLog = new ArrayList<>();
	
	synchronized void logToView(String _data, String _style) {
		try {
			textView.setStyle("-fx-fill:" + _style + ";");
			Log.info(_data);
			textView.setStyle("-fx-fill:black;");
		} catch (NullPointerException | ConcurrentModificationException e) {
			// can happen due to some racing condition
			// ignore, and suppress output so not to confuse the user
		}
	}

	
	@FXML
	void showBeagleInfo() {
		if (beagleInfoButton.getText().equals("Beagle Info")) {
			BeagleInfo.printResourceList();
		}
		closeDialog();
		// runButton.setDisable(true);
	}

	private void closeDialog() {
		if (dialog.getDialogPane().getButtonTypes().size() == 0) {
			// need a button to be able to close the dialog
			dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL);
		}
		dialog.close();
	}

}
