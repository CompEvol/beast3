package beastfx.app.beauti;



import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;

import beastfx.app.inputeditor.BEASTObjectInputEditor;
import beastfx.app.inputeditor.BeautiDoc;
import beastfx.app.inputeditor.InputEditor;
import beastfx.app.inputeditor.ListInputEditor;
import beastfx.app.util.FXUtils;
import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.inference.Operator;
import beast.base.inference.StateNode;



public class OperatorListInputEditor extends ListInputEditor {
    List<TextField> textFields = new ArrayList<>();
    List<Operator> operators = new ArrayList<>();

	public OperatorListInputEditor(BeautiDoc doc) {
		super(doc);
	}

    public OperatorListInputEditor() {
		super();
	}

	@Override
    public Class<?> type() {
        return List.class;
    }

    @Override
    public Class<?> baseType() {
        return Operator.class;
    }

    @Override
    public void init(Input<?> input, BEASTInterface beastObject, int itemNr, ExpandOption isExpandOption, boolean addButtons) {
    	HBox box = FXUtils.newHBox();
    	pane = new BorderPane();

    	box.getChildren().add(new Label("Operator"));

    	box.getChildren().add(new Label("Weight"));

    	((BorderPane)pane).setTop(box);
    	

    	m_buttonStatus = ButtonStatus.NONE;
    	super.init(input, beastObject, itemNr, isExpandOption, addButtons);
    	
    	BEASTObjectInputEditor osEditor = new BEASTObjectInputEditor(doc);
    	osEditor.init(((BEASTInterface) doc.mcmc.get()).getInput("operatorschedule"), (BEASTInterface) doc.mcmc.get(), -1, isExpandOption, addButtons);
    	((BorderPane)pane).setBottom(osEditor);
    }
    
    @Override
    protected InputEditor addPluginItem(Pane itemBox, BEASTInterface beastObject) {
        Operator operator = (Operator) beastObject;

        TextField entry = new TextField(" " + getLabel(operator));
        entry.setMinSize(700, 16);

        m_entries.add(entry);
        entry.setBackground(getBackground());
        entry.setBorder(null);
        itemBox.getChildren().add(entry);
        entry.setEditable(false);


        TextField weightEntry = new TextField();
        weightEntry.setTooltip(new Tooltip(operator.m_pWeight.getTipText()));
        weightEntry.setText(operator.m_pWeight.get() + "");
        weightEntry.setOnKeyReleased(e->{
            try {
                Double weight = Double.parseDouble(weightEntry.getText());
                operator.m_pWeight.setValue(weight, operator);
            } catch (Exception ex) {
                // ignore
            }
            validateInput();        	
        });

        Dimension size = new Dimension(50, 25);
        weightEntry.setMinSize(size.getWidth(), size.getHeight());
        weightEntry.setPrefSize(size.getWidth(), size.getHeight());

        itemBox.getChildren().add(weightEntry);

        FXUtils.createHMCButton(itemBox, operator, m_input);
        return this;
    }


    @Override
    public void updateState() {
        super.updateState();
        for (int i = 0; i < textFields.size(); i++) {
            textFields.get(i).setText(operators.get(i).m_pWeight.get() + "");
            //m_labels.get(i).setText(getLabel(m_operators.get(i)));
            m_entries.get(i).setText(getLabel(operators.get(i)));
        }
    }

    String getLabel(Operator operator) {
        String name = operator.getClass().getName();
        name = name.substring(name.lastIndexOf('.') + 1);
        name = name.replaceAll("Operator", "");
        if (name.matches(".*[A-Z].*")) {
            name = name.replaceAll("(.)([A-Z])", "$1 $2");
        }
        name += ": ";
        try {
            for (BEASTInterface beastObject2 : operator.listActiveBEASTObjects()) {
                if (beastObject2 instanceof StateNode && ((StateNode) beastObject2).isEstimatedInput.get()) {
                    name += beastObject2.getID() + " ";
                }
                // issue https://github.com/CompEvol/beast2/issues/661
                if (name.length() > 100) {
                	name += "... ";
                	break;
                }
            }
        } catch (Exception e) {
            // ignore
        }
        String tipText = getDoc().tipTextMap.get(operator.getID());
        if (tipText != null) {
            name += " " + tipText;
        }
        return name;
    }
} // OperatorListInputEditor
