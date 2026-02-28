package beastfx.app.inputeditor;

public class IntegerInputEditor extends InputEditor.Base {

    public IntegerInputEditor(BeautiDoc doc) {
        super(doc);
    }

    public IntegerInputEditor() {
    	super();
	}

	@Override
    public Class<?> type() {
        return Integer.class;
    }

} // class IntegerInputEditor
