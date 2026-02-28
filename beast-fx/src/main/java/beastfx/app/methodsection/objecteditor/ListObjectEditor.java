package beastfx.app.methodsection.objecteditor;

import java.util.List;

import beast.base.core.Input;
import beastfx.app.inputeditor.BeautiDoc;

public class ListObjectEditor extends ObjectEditor.Base {

	public ListObjectEditor(BeautiDoc doc) {
		super(doc);
	}

    /**
     * return type of the list *
     */
	Class<?> getBaseType() {return null;}

	@Override
	public Class<?> getType() {
		return List.class;
	}

	@Override
	public String toHTML(Object o, Input<?> input) {
		return "not implemented yet";
	}
}
