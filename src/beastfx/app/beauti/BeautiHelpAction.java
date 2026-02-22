package beastfx.app.beauti;


import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;

/** Sub-classes of this class in packages will get
 * a menu item in the Help menu in BEAUti
 * 
 * Sub-classes must implement a constructor with argument BeautiDoc, e.g.,
 *  
 * MyBeautiHelpAction(BeautiDoc doc) {
 *    super("My help", "Gives help the way I like it", "myhelp", -1);
 *    this.doc = doc;
 * }
 * 
 */
public class BeautiHelpAction extends CustomMenuItem {

	public BeautiHelpAction(String name, String toolTipText, String icon, int acceleratorKey) {
		super(new Label(name));
		if (acceleratorKey > 0) {
	        setMnemonicParsing(true);
		}
		Tooltip tooltip = new Tooltip(toolTipText);
		Tooltip.install(getContent(), tooltip);		
	}


}
