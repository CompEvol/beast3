package beastfx.app.inputeditor;

public class LongInputEditor extends InputEditor.Base {

    public LongInputEditor(BeautiDoc doc) {
        super(doc);
    }
    //public IntegerInputEditor() {}

    public LongInputEditor() {
		super();
	}

	@Override
    public Class<?> type() {
        return Long.class;
    }

    @Override
    protected void setUpEntry() {
        super.setUpEntry();
        m_entry.setPrefWidth(150);
    }

} // class LongInputEditor
