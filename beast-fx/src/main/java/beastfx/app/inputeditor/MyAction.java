package beastfx.app.inputeditor;

import java.awt.Toolkit;
import java.awt.image.BufferedImage;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;

import beastfx.app.util.Utils;
import beast.base.core.Log;
import javafx.event.ActionEvent;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;


/**
 * Base class used for definining actions with a name, tool tip text, possibly
 * an icon and accelerator key.
 */
public class MyAction extends MenuItem {

    /**
     * path for icons
     */

//    public MyAction(String name, String toolTipText, String icon, int acceleratorKey) {
//        super(new Label(name));
//        try {
//        	init(name, toolTipText, icon, KeyStroke.getKeyStroke(acceleratorKey, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));        
//        } catch (Throwable  e) {
//        	e.printStackTrace();
//        }
//    } // c'tor
//
//    public MyAction(String name, String toolTipText, String icon, String acceleratorKey) {
//        super(new Label(name));
//        try {
//        	init(name, toolTipText, icon, KeyStroke.getKeyStroke(acceleratorKey));
//        } catch (Throwable  e) {
//        	e.printStackTrace();
//        }
//    } // c'tor

    public MyAction(String name, String toolTipText, String icon, KeyCodeCombination acceleratorKeystroke) {
        super(name);
//        super(new Label(name));
//        setText(name);
        init(name, toolTipText, icon, acceleratorKeystroke);
    }
    
    private void init(String name, String toolTipText, String icon, KeyCodeCombination keyCodeCombination) {
		Tooltip tooltip = new Tooltip(toolTipText);
		//Tooltip.install(getContent(), tooltip);		
		Tooltip.install(getGraphic(), tooltip);		

		try {
        	// setTooltip(new Tooltip(toolTipText));
//        	putValue(Action.SHORT_DESCRIPTION, toolTipText);
//        	putValue(Action.LONG_DESCRIPTION, toolTipText);
//        	if (acceleratorKeystroke != null && acceleratorKeystroke.getKeyCode() >= 0) {
//        		putValue(Action.ACCELERATOR_KEY, acceleratorKeystroke);
//        	}
//        	putValue(Action.MNEMONIC_KEY, new Integer(name.charAt(0)));
        
        	if (keyCodeCombination != null) {
        		setAccelerator(keyCodeCombination);
        	}
			
			if (!Utils.isMac() && icon != null) {
	        	Image openIcon = new Image(getClass().getResourceAsStream(BEASTObjectDialog.ICONPATH + icon + ".png"));
	        	ImageView openView = new ImageView(openIcon);
	        	openView.setFitWidth(20);
	        	openView.setFitHeight(20);
	        	setGraphic(openView);
	        }
        } catch (Throwable  e) {
        	Log.warning(icon != null ? icon + " missing?" : e.getMessage());
        	// e.printStackTrace();
        }
		
		setOnAction(e -> actionPerformed(e));
    } // c'tor



    /*
      * Place holder. Should be implemented by derived classes. (non-Javadoc)
      * @see
      * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent
      * )
      */
    public void actionPerformed(ActionEvent ae) {}

} // class MyAction
