package beastfx.app.beauti;

import java.util.List;

import beastfx.app.inputeditor.BeautiDoc;
import beastfx.app.inputeditor.ListInputEditor;
import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.inference.StateNode;



public class StateNodeListInputEditor extends ListInputEditor {

	public StateNodeListInputEditor(BeautiDoc doc) {
		super(doc);
	}
	
	public StateNodeListInputEditor() {
		super();
	}

	@Override
	public Class<?> type() {
		return List.class;
	}
	
	@Override
	public Class<?> baseType() {
		return StateNode.class;
	}
	
	@Override
	public void init(Input<?> input, BEASTInterface beastObject, int itemNr, ExpandOption isExpandOption, boolean addButtons) {
		m_buttonStatus = ButtonStatus.NONE;
		super.init(input, beastObject, itemNr, isExpandOption, addButtons);
	}

}
