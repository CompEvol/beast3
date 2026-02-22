package beastfx.app.inputeditor;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

public class ActionPanel extends HBox {

	protected Button addButton;
	protected Button delButton;
	
	public ActionPanel() {
		delButton = new Button("-");
		getChildren().add(delButton);
		addButton = new Button("+");
		getChildren().add(addButton);
	}
	
	public void setAddAction(EventHandler<ActionEvent> action) {
		addButton.setOnAction(action);
	}

	public void setRemoveAction(EventHandler<ActionEvent> action) {
		delButton.setOnAction(action);
	}
}
