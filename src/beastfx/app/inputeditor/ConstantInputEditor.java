package beastfx.app.inputeditor;

import beast.base.core.Function.Constant;

public class ConstantInputEditor extends InputEditor.Base {

    public ConstantInputEditor(BeautiDoc doc) {
        super(doc);
    }

    public ConstantInputEditor() {
    	super();
	}

	@Override
    public Class<?> type() {
        return Constant.class;
    }
		
	@Override
	protected void initEntry() {
        m_entry.setText(((Constant)m_input.get()).getValue());
	}
} // class ConstantInputEditor
