package beastfx.app.inputeditor;


import javafx.scene.control.TabPane;
import javafx.scene.layout.Pane;

// this is part of beastfx.app.inputeditor to ensure 
// the inputeditor (java) package does not depend on the beauti (java) package
abstract public class BeautiTabPane extends TabPane {

	public Pane frame;
	
    abstract public void autoSetClockRate(boolean flag);
    abstract public void allowLinking(boolean flag);
    abstract public void autoUpdateFixMeanSubstRate(boolean flag);
    abstract public BeautiPanel getCurrentPanel();
	public abstract void refreshPanel();

}
