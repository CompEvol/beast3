package beastfx.app.beauti;



import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import beastfx.app.inputeditor.BeautiDoc;
import beastfx.app.inputeditor.InputEditor;
import beastfx.app.util.FXUtils;
import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.evolution.speciation.GeneTreeForSpeciesTreeDistribution;



public class GeneTreeForSpeciesTreeDistributionInputEditor extends InputEditor.Base {

	public GeneTreeForSpeciesTreeDistributionInputEditor(BeautiDoc doc) {
		super(doc);
	}

	public GeneTreeForSpeciesTreeDistributionInputEditor() {
		super();
	}

	@Override
	public Class<?> type() {
		return GeneTreeForSpeciesTreeDistribution.class;
	}

	static final int OTHER = 3;
	String [] valuesString = new String[]{"autosomal_nuclear", "X", "Y or mitochondrial", "other"};
	Double [] _values = new Double[]{2.0, 1.5, 0.5, -1.0};
	ComboBox<String> m_selectBeastObjectBox;

	@Override
	public void init(Input<?> input, BEASTInterface beastObject, int itemNr, ExpandOption isExpandOption, boolean addButtons) {
        pane = FXUtils.newHBox();
        m_bAddButtons = addButtons;
        m_input = input;
        m_beastObject = beastObject;
        this.itemNr= itemNr;
        String id = beastObject.getID();
        if (id.contains(".t:")) {
        	id = id.substring(id.indexOf(".t:") + 3);
        }
        Label label = new Label("Ploidy of Gene Tree " + id);
        label.setPrefWidth(LABEL_SIZE.getWidth());
        pane.getChildren().add(label);

        m_selectBeastObjectBox = new ComboBox<>();
        m_selectBeastObjectBox.setId(id+"ComboBox");
        m_selectBeastObjectBox.getItems().addAll(valuesString);
        setSelection();

        m_selectBeastObjectBox.setOnAction(e -> {
                int i = m_selectBeastObjectBox.getSelectionModel().getSelectedIndex();
                try {
                    Input<Double> ploidyInput = (Input<Double>) m_beastObject.getInput("ploidy");
                    ploidyInput.set(_values[i]);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            });
        m_selectBeastObjectBox.setTooltip(new Tooltip(input.getTipText()));
        pane.getChildren().add(m_selectBeastObjectBox);

        getChildren().add(pane);
	}
	
    
	private void setSelection() {
		Double value = (Double) m_beastObject.getInput("ploidy").get();
		m_selectBeastObjectBox.getSelectionModel().select(OTHER);
		for (int i = 0; i < _values.length; i++) {
			if (value.equals(_values[i])) {
				m_selectBeastObjectBox.setValue(valuesString[i]);
			}
		}
	}
}
