package beastfx.app.treeannotator;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

import beastfx.app.treeannotator.TreeAnnotator0.HeightsSummary;
import beastfx.app.treeannotator.TreeAnnotator0.Target;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

public class Controller0 implements Initializable {

	@FXML
	public CheckBox lowMemory; // = new CheckBox();

	@FXML
	public TextField burninPercentage; // = new TextField("10");

	@FXML
	public TextField posteriorLimit; // = new TextField("10");

	@FXML
	public TextField targetTreeFile; // = new TextField();

	@FXML
	public TextField inputTreeFile; // = new TextField();

	@FXML
	public TextField outputFile; // = new TextField();

	@FXML
	public ComboBox<String> nodeHeights; // = new ComboBox<>();

	@FXML
	public ComboBox<String> treeType; // = new ComboBox<>();

	@FXML
	public Button targetTreeFileButton; // = new Button();

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		nodeHeights.getItems().addAll("Common Ancestor heights", "Median heights", "Mean heights",
				"Keep target heights");
		nodeHeights.setValue("Common Ancestor heights");

		treeType.getItems().addAll("Maximum clade credibility tree", "Maximum sum of clade credibilities",
				"User target tree");
		treeType.setValue("Maximum clade credibility tree");
	}

	public void run(ActionEvent e) {
//		System.out.println("nodeHeights: " + nodeHeights.getValue());
//		System.out.println("treeType: " + treeType.getValue() + " " + treeType.getItems());
//		System.out.println("lowMemory: " + lowMemory.isSelected());
//		System.out.println("burninPercentage: " + burninPercentage.getText());
//		System.out.println("posteriorLimit: " + posteriorLimit.getText());
//		System.out.println("targetTreeFile: " + targetTreeFile.getText());
//		System.out.println("inputTreeFile: " + inputTreeFile.getText());
//		System.out.println("outputFile: " + outputFile.getText());

//		Alert alert = new Alert(AlertType.INFORMATION, "Running tree annotator", ButtonType.CLOSE);
//		alert.showAndWait();
	}

	public void quit(ActionEvent e) {
	}

	public void chooseTargetTreeFile(ActionEvent e) {
		FileChooser f = new FileChooser();
		File file = f.showOpenDialog(null);
		if (file != null) {
			targetTreeFile.setText(file.getPath());
		}
	}

	public void chooseInputTreeFile(ActionEvent e) {
		FileChooser f = new FileChooser();
		File file = f.showOpenDialog(null);
		if (file != null) {
			inputTreeFile.setText(file.getPath());
		}
	}

	public void chooseOutputFile(ActionEvent e) {
		FileChooser f = new FileChooser();
		File file = f.showSaveDialog(null);
		if (file != null) {
			outputFile.setText(file.getPath());
		}
	}

	public void refresh(ActionEvent e) {
		if (!treeType.getValue().equals("User target tree")) {
			targetTreeFile.setDisable(true);
			targetTreeFileButton.setDisable(true);
		} else {
			targetTreeFile.setDisable(false);
			targetTreeFileButton.setDisable(false);
		}
	}

	public int getBurninPercentage() {
		try {
			return Integer.parseInt(burninPercentage.getText());
		} catch (Exception e) {
			return 10;
		}
	}

	public double getPosteriorLimit() {
		try {
			return Double.parseDouble(posteriorLimit.getText());
		} catch (Exception e) {
			return 0.0;
		}
	}

	public Target getTargetOption() {
		for (Target t : Target.values()) {
			if (t.toString().equals(treeType.getValue())) {
				return t;
			}
		}
		return null;
	}

	public HeightsSummary getHeightsOption() {
		for (HeightsSummary t : HeightsSummary.values()) {
			if (t.toString().equals(nodeHeights.getValue())) {
				return t;
			}
		}
		return null;
	}

	public String getTargetFileName() {
		return targetTreeFile.getText();
	}

	public String getInputFileName() {
		return inputTreeFile.getText();
	}

	public String getOutputFileName() {
		return outputFile.getText();
	}

	public boolean useLowMem() {
		return lowMemory.isSelected();
	}
}
