package beastfx.app.inputeditor;


import java.util.ArrayList;
import java.util.List;

import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beastfx.app.util.FXUtils;
import javafx.scene.control.*;



/**
 * Input editor for enumeration inputs *
 */
public class EnumInputEditor extends InputEditor.Base {
    public EnumInputEditor(BeautiDoc doc) {
		super(doc);
	}
    //public EnumInputEditor() {}

	public EnumInputEditor() {
		super();
	}

    ComboBox<String> m_selectPluginBox;

    @Override
    public Class<?> type() {
        return Enum.class;
    }

    /**
     * construct an editor consisting of
     * o a label
     * o a combo box for selecting another value in the enumeration
     */
    @Override
    public void init(Input<?> input, BEASTInterface beastObject, int itemNr, ExpandOption isExpandOption, boolean addButtons) {
        m_bAddButtons = addButtons;
        m_input = input;
        m_beastObject = beastObject;
		this.itemNr = itemNr;
		pane = FXUtils.newHBox();
		
        addInputLabel();
        List<String> availableValues = new ArrayList<>();
        for (int i = 0; i < input.possibleValues.length; i++) {
            availableValues.add(input.possibleValues[i].toString());
        }
        if (availableValues.size() > 1) {
            m_selectPluginBox = new ComboBox<>();
            m_selectPluginBox.setId(input.getName()+"ComboBox");
            m_selectPluginBox.getItems().addAll(availableValues.toArray(new String[]{}));
            // Dimension2D maxDim = m_selectPluginBox.getPrefHeight();
            // m_selectPluginBox.setMaxSize(m_selectPluginBox.getPrefWidth(), m_selectPluginBox.getPrefHeight());
            m_selectPluginBox.setMaxSize(10000, m_selectPluginBox.getPrefHeight());

            String selectString = input.get().toString();
            m_selectPluginBox.setValue(selectString);
            m_selectPluginBox.getSelectionModel().select(selectString);

            m_selectPluginBox.setOnAction(e -> {
                    String selected = (String) m_selectPluginBox.getValue();
                    try {
                    	setValue(selected);
                        //lm_input.setValue(selected, m_beastObject);
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                });
            m_selectPluginBox.setTooltip(new Tooltip(input.getTipText()));
            pane.getChildren().add(m_selectPluginBox);
            FXUtils.createHMCButton(pane, m_beastObject, m_input);
            // add(Box.createGlue());
            // pane.getChildren().add(new Separator());
        }
        getChildren().add(pane);
    } // init


} // class EnumInputEditor
