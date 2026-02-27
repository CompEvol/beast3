package beastfx.app.inputeditor;

import beastfx.app.util.TreeFile;

public class OutFileListInputEditor extends FileListInputEditor {
	
	public OutFileListInputEditor(BeautiDoc doc) {
		super(doc);
	}
	
    public OutFileListInputEditor() {
		super();
	}

	@Override
    public Class<?> baseType() {
		return TreeFile.class;
    }
}
