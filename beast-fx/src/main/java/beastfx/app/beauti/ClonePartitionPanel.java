package beastfx.app.beauti;

import beastfx.app.inputeditor.BeautiPanel;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;


public class ClonePartitionPanel extends VBox {

    final BeautiPanel beautiPanel;
    final ComboBox<Object> cloneFromComboBox;
    final Button okButton = new Button("OK");

	public ClonePartitionPanel(BeautiPanel beautiPanel) {
        this.beautiPanel = beautiPanel;

        ObservableList<String> listModel = beautiPanel.listModel;
        Object[] models = new Object[listModel.size()];
        for(int i=0; i < listModel.size(); i++){
            models[i] = listModel.get(i);
        }

        cloneFromComboBox = new ComboBox<>();
        cloneFromComboBox.getItems().addAll(models);
        // has to be editable
        cloneFromComboBox.setEditable(true);
        // change the editor's document
        new S11InitialSelection(cloneFromComboBox);

        init();
    }


    public void init() {
        FlowPane pane = new FlowPane();

        Label label = new Label("Clone from");
        pane.getChildren().add(label);

        pane.getChildren().add(cloneFromComboBox);
        okButton.setId("ok");
        okButton.setTooltip(new Tooltip("Click to clone configuration from the above selected partition " +
                "into all selected partitions on the left."));
        okButton.setOnAction(e -> {
                clonePartitions();
            });
        pane.getChildren().add(okButton);
        getChildren().add(pane);
    } // init

    protected void clonePartitions() {
        String sourceId = cloneFromComboBox.getValue().toString();

        for (String targetId : beautiPanel.listOfPartitions.getSelectionModel().getSelectedItems()) {
             beautiPanel.cloneFrom(sourceId, targetId);
        }
    }
}
