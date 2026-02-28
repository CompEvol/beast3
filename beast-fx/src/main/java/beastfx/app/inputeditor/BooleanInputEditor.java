package beastfx.app.inputeditor;


import javafx.scene.control.CheckBox;
import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.core.Log;
import beastfx.app.util.FXUtils;
import javafx.scene.control.Tooltip;



public class BooleanInputEditor extends InputEditor.Base {
    public BooleanInputEditor(BeautiDoc doc) {
		super(doc);
	}
    
	public BooleanInputEditor() {
		super();
	}

    CheckBox m_entry;


    @Override
    public Class<?> type() {
        return Boolean.class;
    }

    /**
     * create input editor containing a check box *
     */
    @Override
    public void init(Input<?> input, BEASTInterface beastObject, int itemNr, ExpandOption isExpandOption, boolean addButtons) {
        m_bAddButtons = addButtons;
        m_beastObject = beastObject;
        m_input = input;
		this.itemNr = itemNr;
        m_entry = new CheckBox(formatName(m_input.getName()));
        if (input.get() != null) {
            m_entry.setSelected((Boolean) input.get());
        }
        m_entry.setTooltip(new Tooltip(input.getTipText()));
        m_entry.setOnAction(e -> {
                try {
                	setValue(m_entry.isSelected());
                	refreshPanel();
                    validateInput();
                    //m_input.setValue(m_entry.isSelected(), m_beastObject);
                } catch (Exception ex) {
                    Log.err.println("BooleanInputEditor " + ex.getMessage());
                }
            });
        pane = FXUtils.newHBox();
        pane.getChildren().add(m_entry);
        FXUtils.createHMCButton(pane, m_beastObject, m_input);
        //getChildren().add(new Separator());
        // pane.getChildren().add(new Separator());
        getChildren().add(pane);
    } // c'tor

} // class BooleanInputEditor
