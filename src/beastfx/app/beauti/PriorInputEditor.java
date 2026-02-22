package beastfx.app.beauti;





import java.util.Arrays;
import java.util.List;

import javafx.geometry.Dimension2D;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import beastfx.app.inputeditor.BEASTObjectDialog;
import beastfx.app.inputeditor.BeautiDoc;
import beastfx.app.inputeditor.BeautiSubTemplate;
import beastfx.app.inputeditor.HasExpandBox;
import beastfx.app.inputeditor.InputEditor;
import beastfx.app.inputeditor.ListInputEditor;
import beastfx.app.util.FXUtils;
import beast.base.core.BEASTInterface;
import beast.base.core.Function;
import beast.base.core.Function.Constant;
import beast.base.core.Input;
import beast.base.inference.distribution.Dirichlet;
import beast.base.inference.distribution.ParametricDistribution;
import beast.base.inference.distribution.Prior;
import beast.base.inference.parameter.IntegerParameter;
import beast.base.inference.parameter.RealParameter;
import beast.base.parser.PartitionContext;

public class PriorInputEditor extends InputEditor.Base implements HasExpandBox {

	public PriorInputEditor(BeautiDoc doc) {
		super(doc);
	}

	public PriorInputEditor() {
		super();
	}

	@Override
	public Class<?> type() {
		return Prior.class;
	}

	private ComboBox<BeautiSubTemplate> comboBox;
	
	@Override
	public void init(Input<?> input, BEASTInterface beastObject, int listItemNr, ExpandOption isExpandOption, boolean addButtons) {
        m_bAddButtons = addButtons;
        m_input = input;
        m_beastObject = beastObject;
        this.itemNr= listItemNr;
		
        pane = FXUtils.newVBox();
        HBox itemBox = FXUtils.newHBox();

        Prior prior = (Prior) beastObject;
        String text = prior.getParameterName();
        Label label = new Label(text);
        Font font = label.getFont();
        Dimension2D size = new Dimension2D(font.getSize() * 200 / 13, font.getSize() * 25/13);
        label.setMinSize(size.getWidth(), size.getHeight());
        label.setPrefSize(size.getWidth(), size.getHeight());
        String tipText = getDoc().tipTextMap.get(beastObject.getID());
        label.setTooltip(new Tooltip(text + " " + (tipText != null ? tipText : "")));
        itemBox.getChildren().add(label);

        

        comboBox = createComboBox();	

        Pane panel = new Pane();
        panel.getChildren().add(comboBox);
        panel.setMaxSize(size.getWidth(), size.getHeight());
        itemBox.getChildren().add(panel);
        FXUtils.createHMCButton(itemBox, m_beastObject, m_input);
        
        if (prior.m_x.get() instanceof RealParameter) {
            // add range button for real parameters
            RealParameter p = (RealParameter) prior.m_x.get();
            Button rangeButton = new Button(paramToString(p));
            rangeButton.setOnAction(e -> {
                Button rangeButton1 = (Button) e.getSource();

                List<?> list = (List<?>) m_input.get();
                Prior prior1 = (Prior) list.get(itemNr);
                RealParameter p1 = (RealParameter) prior1.m_x.get();
                BEASTObjectDialog dlg = new BEASTObjectDialog(p1, RealParameter.class, doc);
                if (dlg.showDialog()) {
                    dlg.accept(p1, doc);
                    p1.initAndValidate();
                    rangeButton1.setText(paramToString(p1));
                    refreshPanel();
                }
            });
            rangeButton.setPrefWidth(InputEditor.Base.LABEL_SIZE.getWidth());
            rangeButton.setTooltip(new Tooltip("Initial value and range of " + p.getID()));
            
            itemBox.getChildren().add(rangeButton);
        } else if (prior.m_x.get() instanceof IntegerParameter) {
            // add range button for real parameters
            IntegerParameter p = (IntegerParameter) prior.m_x.get();
            Button rangeButton = new Button(paramToString(p));
            rangeButton.setOnAction(e -> {
                Button rangeButton1 = (Button) e.getSource();

                List<?> list = (List<?>) m_input.get();
                Prior prior1 = (Prior) list.get(itemNr);
                IntegerParameter p1 = (IntegerParameter) prior1.m_x.get();
                BEASTObjectDialog dlg = new BEASTObjectDialog(p1, IntegerParameter.class, doc);
                if (dlg.showDialog()) {
                    dlg.accept(p1, doc);
                    p1.initAndValidate();
                    rangeButton1.setText(paramToString(p1));
                    refreshPanel();
                }
            });

            itemBox.getChildren().add(rangeButton);
        }
        int fontsize = (int) comboBox.getEditor().getFont().getSize();
        comboBox.setMaxSize(1024 * fontsize / 13, 24 * fontsize / 13);

        if (tipText != null) {
            Label tipTextLabel = new Label(" " + tipText);
            itemBox.getChildren().add(tipTextLabel);
        }
        
        pane.getChildren().add(itemBox);
        getChildren().add(pane);
	}

    private String getParameters() {
    	StringBuilder b = null;
    	ParametricDistribution distr = (ParametricDistribution) m_beastObject.getInput("distr").get();
    	for (Input<?> input: distr.listInputs()) {
    		Object o = input.get();
    		if (o != null && (o instanceof RealParameter||o instanceof Constant)) {
    			BEASTInterface p = (BEASTInterface) o;
    			if (b == null) {
    				b = new StringBuilder();
    				b.append(p.getInput("value").get().toString().trim());
    			} else {
    				b.append(',');
    				b.append(p.getInput("value").get().toString().trim());
    			}
    		} else if (o != null && o instanceof Double && !input.getName().equals("offset")) {
    			Double p = (Double) o;
    			if (b == null) {
    				b = new StringBuilder();
    				b.append(p);
    			} else {
    				b.append(',');
    				b.append(p);
    			}
    		}

    	}
		if (b == null) {
			return "";
		}
		return "[" + b.toString().replaceAll("[\\]\\[]", "") + "]";
	}

	String paramToString(RealParameter p) {
        Double lower = p.lowerValueInput.get();
        Double upper = p.upperValueInput.get();
        return "initial = " + Arrays.toString(p.valuesInput.get().toArray()) +
                " [" + (lower == null ? "-\u221E" : lower + "") +
                "," + (upper == null ? "\u221E" : upper + "") + "]";
    }

    String paramToString(IntegerParameter p) {
        Integer lower = p.lowerValueInput.get();
        Integer upper = p.upperValueInput.get();
        return "initial = " + Arrays.toString(p.valuesInput.get().toArray()) +
                " [" + (lower == null ? "-\u221E" : lower + "") +
                "," + (upper == null ? "\u221E" : upper + "") + "]";
    }

    
    VBox expandBox = null;
    
    @Override
	public void setExpandBox(VBox expandBox) {
		this.expandBox = expandBox;
		this.expandBox.visibleProperty().addListener((o, oldVal, newVal) -> {
			Pane parent = (Pane) comboBox.getParent();
			int i = parent.getChildren().indexOf(comboBox);
			comboBox = createComboBox();
			parent.getChildren().set(i, comboBox);
     });
	}
	
	private ComboBox<BeautiSubTemplate> createComboBox() {
		ComboBox<BeautiSubTemplate> comboBox = new ComboBox<>();

        Prior prior = (Prior) m_beastObject;
        String text = prior.getParameterName();

        List<BeautiSubTemplate> availableBEASTObjects = doc.getInputEditorFactory().getAvailableTemplates(prior.distInput, prior, null, doc);
        if (prior.m_x.get().getDimension() == 1) {
        	// remove Dirichlet entry
        	for (int i = availableBEASTObjects.size() - 1; i >=0 ; i--) {
        		if (availableBEASTObjects.get(i).getID().equals("Dirichlet")) {
        			availableBEASTObjects.remove(i);
        		}
        	}
        }

        comboBox.getItems().addAll(availableBEASTObjects);
        comboBox.setId(text+".distr");
        comboBox.setButtonCell(new ListCell<BeautiSubTemplate>() {
        	@Override
        	protected void updateItem(BeautiSubTemplate item, boolean empty) {
        		super.updateItem(item, empty);
        		if (!empty && item != null) {
        			if (expandBox !=null && expandBox.isVisible()) {
        				setText(item.toString());
        			} else {
        				setText(item.toString() + getParameters());
        			}
                } else {
                    setText(null);
                }
        	}
        });

        String id = prior.distInput.get().getID();

        id = id.substring(0, id.indexOf('.'));
        for (BeautiSubTemplate template : availableBEASTObjects) {
            if (template.classInput.get() != null && template.shortClassName.equals(id)) {
                comboBox.setValue(template);
            }
        }
        comboBox.setOnAction(e -> {
            @SuppressWarnings("unchecked")
			ComboBox<BeautiSubTemplate> comboBox1 = (ComboBox<BeautiSubTemplate>) e.getSource();

            List<?> list = (List<?>) m_input.get();

            BeautiSubTemplate template = (BeautiSubTemplate) comboBox1.getValue();
            PartitionContext context = doc.getContextFor((BEASTInterface) list.get(itemNr));
            Prior prior1 = (Prior) list.get(itemNr);
            try {
                template.createSubNet(context, prior1, prior1.distInput, true);
            } catch (Exception e1) {
                e1.printStackTrace();
            }

            if (prior1.distInput.get() instanceof Dirichlet) {
            	Input<Function> alphaInput = ((Dirichlet)prior1.distInput.get()).alphaInput;
            	Function f = alphaInput.get();
            	if (f instanceof RealParameter) {
            		((RealParameter)f).setDimension(prior1.m_x.get().getDimension());
            	}
            }
            
            sync();
            refreshPanel();
        });
        
        String tipText = getDoc().tipTextMap.get(m_beastObject.getID());
        if (tipText != null) {
        	comboBox.setTooltip(new Tooltip(tipText));
        }
        
        
        return comboBox;

	}

	@Override
	public void refreshPanel() {
		if (expandBox != null) {
			ListInputEditor.updateExpandBox(doc, expandBox, m_beastObject, this);
		}
		super.refreshPanel();
	}    
}
