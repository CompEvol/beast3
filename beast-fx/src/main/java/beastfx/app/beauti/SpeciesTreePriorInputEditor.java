package beastfx.app.beauti;


import beastfx.app.inputeditor.BEASTObjectInputEditor;
import beastfx.app.inputeditor.BeautiDoc;
import javafx.scene.layout.Pane;
import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.evolution.speciation.SpeciesTreePrior;



public class SpeciesTreePriorInputEditor extends BEASTObjectInputEditor {

	public SpeciesTreePriorInputEditor() {
		super();
	}
	public SpeciesTreePriorInputEditor(BeautiDoc doc) {
		super(doc);
	}

	@Override
	public Class<?> type() {
		return SpeciesTreePrior.class;
	}
	
	@Override
	public void init(Input<?> input, BEASTInterface beastObject, int itemNr, ExpandOption isExpandOption, boolean addButtons) {
		super.init(input, beastObject, itemNr, isExpandOption, addButtons);
	}

    @Override
	protected void addComboBox(Pane box, Input<?> input, BEASTInterface beastObject) {
    	m_bAddButtons = true;
    	String label = "Species Tree Population Size";
    	addInputLabel(label, label);
    	m_bAddButtons = false;
    }
}
