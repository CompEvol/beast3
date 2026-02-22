package beastfx.app.inputeditor;


import beastfx.app.util.TreeFile;

public class LogFileListInputEditor extends FileListInputEditor {

	public LogFileListInputEditor(BeautiDoc doc) {
		super(doc);
	}
	
    public LogFileListInputEditor() {
		super();
	}

	@Override
    public Class<?> baseType() {
		return TreeFile.class;
    }
}
