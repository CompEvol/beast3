package beastfx.app.inputeditor;



import java.net.URL;

import javax.imageio.ImageIO;

import beastfx.app.util.Alert;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;


/**
 * @author Alexei Drummond
 */
public class SmallButton extends Button {

	public enum ButtonType {roundRect, square, toolbar}
    
    public SmallButton(String label, boolean isEnabled) {
        this(label, isEnabled, ButtonType.square);
        setIcon(label);
    }

	public SmallButton(String label, boolean isEnabled, ButtonType buttonType) {
        super(label);
		setDisable(!isEnabled);
        setButtonType(buttonType);
        setIcon(label);
    }

	private void setIcon(String label) {
        if (label.equals("e")) {
        	setText("");
//            String style = 
//                    "-fx-background-radius: 2; " +
//                    "-fx-min-width: 20pt; " +
//                    "-fx-min-height: 20pt; " +
//                    "-fx-max-width: 20pt; " +
//                    "-fx-max-height: 20pt; " +
//                    "-fx-font-size: 5pt;";
//        	setStyle(style);
//        	setPadding(new Insets(10, 0, 0, 5));
            // getTooltip().setStyle("-fx-font-size: 8pt");

            //Image image = new Image(getClass().getResource("icon/edit.png").toString());
            //ImageView icon = new ImageView(image);
        	//setGraphic(icon);
            //setBorder(BorderFactory.createEmptyBorder());
            //setStyle("-fx-border:0 0 0 0 ");
        }
	}

    public void setButtonType(ButtonType buttonType) {
    	// RRB TODO not sure what to do here
        //setClientProperty("Button.buttonType", buttonType.toString());    
    }

    public void setImg(java.awt.Image image) {
    	setGraphic(new ImageView(Alert.jswingIconToImage(image)));
    }
    
    public void setImg(Image image) {
    	setGraphic(new ImageView(image));
    }

}
