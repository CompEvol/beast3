package beastfx.app.inputeditor;



import java.util.List;


import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.spec.evolution.likelihood.GenericTreeLikelihood;
import beastfx.app.util.Alert;
import beastfx.app.util.FXUtils;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class BEASTObjectInputEditor extends InputEditor.Base {
    ComboBox<Object> m_selectBEASTObjectBox;
    protected Button m_editBEASTObjectButton;

    BEASTObjectInputEditor _this;

    public BEASTObjectInputEditor() {    
    	_this = this;
    }
    
    public BEASTObjectInputEditor(BeautiDoc doc) {
        super(doc);
        _this = this;
    }

    @Override
    public Class<?> type() {
        return BEASTInterface.class;
    }

    /**
     * construct an editor consisting of
     * o a label
     * o a combo box for selecting another plug-in
     * o a button for editing the plug-in
     * o validation label -- optional, if input is not valid
     */
    @Override
    public void init(Input<?> input, BEASTInterface beastObject, int itemNr, ExpandOption isExpandOption, boolean addButtons) {
    	//box.setAlignmentY(LEFT_ALIGNMENT);
    	
        m_bAddButtons = addButtons;
        m_input = input;
        m_beastObject = beastObject;
		this.itemNr = itemNr;
    	pane = new HBox();
    	pane.setPadding(new Insets(5));

        if (isExpandOption == ExpandOption.FALSE) {
            simpleInit(input, beastObject);
        } else {
            expandedInit(input, beastObject);
        }

        getChildren().add(pane);
    } // init

    /**
     * add combobox with available beastObjects
     * a button to edit that beastObject +
     * a validation icon
     * *
     */
    void simpleInit(Input<?> input, BEASTInterface beastObject) {

        addInputLabel();

        addComboBox(pane, input, beastObject);

        if (m_bAddButtons) {
            if (BEASTObjectPanel.countInputs(m_input.get(), doc) > 0 && m_editBEASTObjectButton == null) {
            	Region region = new Region();
            	region.setPadding(new Insets(3));
            	pane.getChildren().add(region);
            	Button b = createEditButton(input);
                pane.getChildren().add(b);
            }
        }
        addValidationLabel();
    } // init

    protected Button createEditButton(Input<?> input) {
        m_editBEASTObjectButton = new Button("e");        
        String style = 
                "-fx-background-radius: 10; " +
                "-fx-min-width: 15px; " +
                "-fx-min-height: 15px; " +
                "-fx-max-width: 15px; " +
                "-fx-max-height: 15px; " +
                "-fx-font-size: 5pt";
        m_editBEASTObjectButton.setStyle(style);

        if (input.get() == null) {
            m_editBEASTObjectButton.setDisable(true);
        }
        m_editBEASTObjectButton.setTooltip(new Tooltip("Edit " + m_inputLabel.getText()));
        m_editBEASTObjectButton.getTooltip().setStyle("-fx-font-size: 8pt");

        m_editBEASTObjectButton.setOnAction(e -> {
            BEASTObjectDialog dlg = new BEASTObjectDialog((BEASTInterface) m_input.get(), m_input.getType(), doc);
            if (dlg.showDialog()) {
                try {
                    dlg.accept((BEASTInterface) m_input.get(), doc);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            refresh();
            validateInput();
            refreshPanel();
        });
        return m_editBEASTObjectButton;
	}

	protected void refresh() {
    	if (m_selectBEASTObjectBox != null) {
	        String oldID = (String) m_selectBEASTObjectBox.getValue();
	        String id = ((BEASTInterface) m_input.get()).getID();
	        if (!id.equals(oldID)) {
	            m_selectBEASTObjectBox.getItems().add(id);
	            m_selectBEASTObjectBox.setValue(id);
	            m_selectBEASTObjectBox.getItems().remove(oldID);
	        }
    	}
        super.refreshPanel();
    }

    void initSelectPluginBox() {
        List<String> availableBEASTObjects = doc.getInputEditorFactory().getAvailablePlugins(m_input, m_beastObject, null, doc);
        if (availableBEASTObjects.size() > 0) {
            availableBEASTObjects.add(NO_VALUE);
            for (int i = 0; i < availableBEASTObjects.size(); i++) {
                String beastObjectName = availableBEASTObjects.get(i);
                if (beastObjectName.startsWith("new ")) {
                    beastObjectName = beastObjectName.substring(beastObjectName.lastIndexOf('.'));
                    availableBEASTObjects.set(i, beastObjectName);
                }

            }
            m_selectBEASTObjectBox.getItems().remove(0, m_selectBEASTObjectBox.getItems().size()-1);
            for (String str : availableBEASTObjects.toArray(new String[0])) {
                m_selectBEASTObjectBox.getItems().add(str);
            }
            m_selectBEASTObjectBox.setValue(m_beastObject.getID());
        }
    }

    VBox m_expansionBox = null;

    void expandedInit(Input<?> input, BEASTInterface beastObject) {
        addInputLabel();
        VBox box = new VBox();
        // add horizontal box with combobox of BEASTObjects to select from
        HBox combobox = new HBox();
        addComboBox(combobox, input, beastObject);
        box.getChildren().add(combobox);

        doc.getInputEditorFactory().addInputs(box, (BEASTInterface) input.get(), this, this, doc);

        HBox.setHgrow(box, Priority.ALWAYS);
        pane.getChildren().add(box);
        m_expansionBox = box;
        m_expansionBox.setBorder(new Border(new BorderStroke(Color.LIGHTGRAY, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(1))));
    } // expandedInit


    /**
     * add combobox with BEASTObjects to choose from
     * On choosing a new value, create beastObject (if is not already an object)
     * Furthermore, if expanded, update expanded inputs
     */
    protected void addComboBox(Pane box, Input<?> input, BEASTInterface beastObject0) {
    	if (itemNr >= 0) {
    		box.getChildren().add(new Label(beastObject0.getID()));
    		// box.add(Box.createGlue());
    		return;
    	}
    	
        List<BeautiSubTemplate> availableTemplates = doc.getInputEditorFactory().getAvailableTemplates(m_input, m_beastObject, null, doc);
        if (availableTemplates.size() > 0) {
            m_selectBEASTObjectBox = new ComboBox<>();
            m_selectBEASTObjectBox.getItems().addAll(availableTemplates.toArray());
            m_selectBEASTObjectBox.setId(input.getName()+"ComboBox");

            Object o = input.get();
            if (itemNr >= 0) {
            	o = ((List<?>)o).get(itemNr);
            }
            String id2;
            if (o == null) {
                id2 = beastObject0.getID();
            } else if (o instanceof BEASTInterface) {
                id2 = ((BEASTInterface) o).getID();
            } else {
            	id2 = input.getName();
            }
            if (id2.indexOf('.')>=0) {
            	id2 = id2.substring(0, id2.indexOf('.'));
            }
            for (BeautiSubTemplate template : availableTemplates) {
                if (template.matchesName(id2)) {
                    m_selectBEASTObjectBox.setValue(template);
                }
            }

            m_selectBEASTObjectBox.setOnAction(e -> {

                // get a handle of the selected beastObject
            	BeautiSubTemplate selected = (BeautiSubTemplate) m_selectBEASTObjectBox.getValue();
                BEASTInterface beastObject = (BEASTInterface) m_input.get();
                String id = beastObject==null ? "no_id" : beastObject.getID();
                String partition = id.indexOf('.') >= 0 ? 
                		id.substring(id.indexOf('.') + 1) : "";
                if (partition.indexOf(':') >= 0) {
                	partition = id.substring(id.indexOf(':') + 1);
                }

                if (selected == null || selected.equals(NO_VALUE)) {
                    beastObject = null;
                } else {
                    try {
                        beastObject = selected.createSubNet(doc.getContextFor(beastObject), m_beastObject, m_input, true);
                    } catch (Exception ex) {
                        Alert.showMessageDialog(null, "Could not select beastObject: " +
                                ex.getClass().getName() + " " +
                                ex.getMessage()
                        );
                    }
                }


                try {
                    if (beastObject == null) {
                        m_selectBEASTObjectBox.setValue(NO_VALUE);
                        // is this input expanded?
                        if (m_expansionBox != null) {
                            // remove items from Expansion Box, if any
                            for (int i = 1; i < m_expansionBox.getChildren().size(); i++) {
                                m_expansionBox.getChildren().remove(i);
                            }
                        } else { // not expanded
                            if (m_bAddButtons && m_editBEASTObjectButton != null) {
                                m_editBEASTObjectButton.setDisable(true);
                            }
                        }
                    } else {
                        if (!m_input.canSetValue(beastObject, m_beastObject)) {
                            throw new IllegalArgumentException("Cannot set input to this value");
                        }
                        id = beastObject.getID();
                        if (id.indexOf('.') != -1) {
                        	id = id.substring(0,  id.indexOf('.'));
                        }
                         for (int i = 0; i < m_selectBEASTObjectBox.getItems().size(); i++) {
                            BeautiSubTemplate template = (BeautiSubTemplate) m_selectBEASTObjectBox.getItems().get(i);
                            if (template.getMainID().replaceAll(".\\$\\(n\\)", "").equals(id) ||
                            		template.getMainID().replaceAll(".s:\\$\\(n\\)", "").equals(id) || 
                            		template.getMainID().replaceAll(".c:\\$\\(n\\)", "").equals(id) || 
                            		template.getMainID().replaceAll(".t:\\$\\(n\\)", "").equals(id)) {
                                m_selectBEASTObjectBox.setValue(template);
                            }
                        }
                    }

                    setValue(beastObject);
                    //m_input.setValue(beastObject, m_beastObject);

                    if (m_expansionBox != null) {
                        // remove items from Expansion Box
                        while (m_expansionBox.getChildren().size()>1) {
                            m_expansionBox.getChildren().remove(m_expansionBox.getChildren().size()-1);
                        }
                        // add new items to Expansion Box
                        if (beastObject != null) {
                        	doc.getInputEditorFactory().addInputs(m_expansionBox, beastObject, _this, _this, doc);
                        }
                    } else {
                        // it is not expanded, enable the edit button
                        if (m_bAddButtons && m_editBEASTObjectButton != null) {
                            m_editBEASTObjectButton.setDisable(false);
                        }
                        validateInput();
                    }
                    sync();
                    refreshPanel();
                } catch (Exception ex) {
                    id = ((BEASTInterface) m_input.get()).getID();
                    m_selectBEASTObjectBox.setValue(id);
                    ex.printStackTrace();
                    Alert.showMessageDialog(null, "Could not change beastObject: " +
                            ex.getClass().getName() + " " +
                            ex.getMessage() 
                    );
                }
            });

            if (input instanceof BeautiPanelConfig.FlexibleInput) {
            	Object o2 = input.get();
            	if (o2 instanceof BEASTInterface) {
            		BEASTInterface bi = (BEASTInterface) o2;
            		for (Object o3 : bi.getOutputs()) {
            			if (o3 instanceof GenericTreeLikelihood) {
            				GenericTreeLikelihood tl = (GenericTreeLikelihood) o3;
            				for (Input<?> tlInput : tl.listInputs()) {
            					if (tlInput.get() == o2) {
                                	m_selectBEASTObjectBox.setTooltip(new Tooltip(tlInput.getTipText()));            	
            					}
            				}
            			}
            		}
            	}
            } else {
            	m_selectBEASTObjectBox.setTooltip(new Tooltip(input.getTipText()));
            }
            box.getChildren().add(m_selectBEASTObjectBox);
            FXUtils.createHMCButton(box, m_beastObject, m_input);

            HBox.setMargin(m_selectBEASTObjectBox, new Insets(0,5,0,5));
        }
    }
    
} // class PluginInputEditor
