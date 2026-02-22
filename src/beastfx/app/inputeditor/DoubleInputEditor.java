package beastfx.app.inputeditor;

public class DoubleInputEditor extends InputEditor.Base {

    public DoubleInputEditor(BeautiDoc doc) {
        super(doc);
    }
    //public DoubleInputEditor() {}

    public DoubleInputEditor() {
    	super();
	}

	@Override
    public Class<?> type() {
        return Double.class;
    }
} // class DoubleInputEditor
