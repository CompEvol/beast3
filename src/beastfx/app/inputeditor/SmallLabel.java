package beastfx.app.inputeditor;



import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;

/**
 * Miniature round label
 */
public class SmallLabel extends Label {
    public String circleColor = "blue";

    static private String style = 
            "-fx-background-radius: 20em; " +
            "-fx-min-width: 15px; " +
            "-fx-min-height: 15px; " +
            "-fx-max-width: 15px; " +
            "-fx-max-height: 15px; " +
            "-fx-background-color: ";
    
    public SmallLabel(String label, String circleColor) {
        super(label);
        setText("");
        setStyle(style + circleColor);

        this.circleColor = circleColor;
        super.setVisible(true);
    } // c'tor

    public void setColor(String circleColor) {
        setStyle(style + circleColor);
    }

    public void setTooltip(String text) {
	    final Tooltip tooltip = new Tooltip();
	    tooltip.setText(text);
    	setTooltip(tooltip);
    }

} // class SmallButton
