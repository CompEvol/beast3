package beastfx.app.inputeditor;


import beastfx.app.util.TreeFile;

public class TreeFileListInputEditor extends FileListInputEditor {
	public TreeFileListInputEditor(BeautiDoc doc) {
		super(doc);
	}
	
    public TreeFileListInputEditor() {
		super();
	}

	@Override
    public Class<?> baseType() {
		return TreeFile.class;
    }
}
