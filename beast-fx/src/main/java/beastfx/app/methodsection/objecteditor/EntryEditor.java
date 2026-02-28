package beastfx.app.methodsection.objecteditor;


import beast.base.core.BEASTObjectStore;
import beast.base.core.Input;
import beastfx.app.inputeditor.BeautiDoc;

public class EntryEditor extends ObjectEditor.Base {

	public EntryEditor(BeautiDoc doc) {
		super(doc);
	}

	@Override
	public Class<?> getType() {
		return Double.class;
	}
	
	@Override
	public Class<?>[] getTypes() {
		return new Class[]{Double.class, Float.class, String.class, Integer.class, Long.class};
	}
	
	@Override
	public String toHTML(Object o, Input<?> input) {
		String source = BEASTObjectStore.getId(o) + " " + input.getName();
		String text = input.get() + "";
		return "<td>" + input.getName() + "</td><td> <input size='5' onkeyup='doIt(value,\"" + source + "\")' value='" + text +"'/></td>\n";
	}

}
