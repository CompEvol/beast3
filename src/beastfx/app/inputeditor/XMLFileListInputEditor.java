package beastfx.app.inputeditor;


import beastfx.app.util.TreeFile;

public class XMLFileListInputEditor extends FileListInputEditor {

	public XMLFileListInputEditor(BeautiDoc doc) {
		super(doc);
	}
	
    public XMLFileListInputEditor() {
		super();
	}

	@Override
    public Class<?> baseType() {
		return TreeFile.class;
    }
}
