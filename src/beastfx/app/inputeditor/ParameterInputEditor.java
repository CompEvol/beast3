package beastfx.app.inputeditor;



import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.core.Log;
import beast.base.evolution.branchratemodel.BranchRateModel;
import beast.base.inference.Distribution;
import beast.base.inference.Operator;
import beast.base.inference.distribution.ParametricDistribution;
import beast.base.inference.parameter.Parameter;
import beast.base.parser.PartitionContext;
import beastfx.app.util.Alert;
import beastfx.app.util.FXUtils;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;

import java.util.List;





public class ParameterInputEditor extends BEASTObjectInputEditor {
	boolean isParametricDistributionParameter = false;
	
    public ParameterInputEditor() {
    	super();
    }
    public ParameterInputEditor(BeautiDoc doc) {
		super(doc);
	}

    public CheckBox m_isEstimatedBox;

    @Override
    public Class<?> type() {
        return Parameter.Base.class;
    }
    
    
    @Override
    public void init(Input<?> input, BEASTInterface beastObject, int itemNr, ExpandOption isExpandOption, boolean addButtons) {
    	super.init(input, beastObject, itemNr, isExpandOption, addButtons);
    	m_beastObject = beastObject;
    	pane.setPadding(new Insets(5));
    }

    @Override
    protected void initEntry() {
        if (m_input.get() != null) {
        	if (itemNr < 0) {
        		Parameter.Base<?> parameter = (Parameter.Base<?>) m_input.get();
        		String s = "";
        		for (Object d : parameter.valuesInput.get()) {
        			s += d + " ";
        		}
        		m_entry.setText(s);
        	} else {
        		Parameter.Base<?> parameter = (Parameter.Base<?>) ((List<?>)m_input.get()).get(itemNr);
        		String s = "";
        		for (Object d : parameter.valuesInput.get()) {
        			s += d + " ";
        		}
        		m_entry.setText(s);
        	}
        }
    }

    @Override
    protected void processEntry() {
        try {
            String valueString = m_entry.getText();
            Parameter.Base<?> parameter = (Parameter.Base<?>) m_input.get();
        	String oldValue = "";
    		for (Object d : parameter.valuesInput.get()) {
    			oldValue += d + " ";
    		}
            int oldDim = parameter.getDimension();
            parameter.valuesInput.setValue(valueString, parameter);
            parameter.initAndValidate();
            int newDim = parameter.getDimension();
            if (oldDim != newDim) {
            	parameter.setDimension(oldDim);
                parameter.valuesInput.setValue(oldValue, parameter);
                parameter.initAndValidate();
                throw new IllegalArgumentException("Entry caused change in dimension");
            }
            validateInput();
        } catch (Exception ex) {
            m_validateLabel.setVisible(true);
            m_validateLabel.setTooltip(new Tooltip("Parsing error: " + ex.getMessage() + ". Value was left at " + m_input.get() + "."));
            m_validateLabel.setColor("orange");
            repaint();
        }
    }


    @Override
    protected void addComboBox(Pane box, Input<?> input, BEASTInterface beastObject) {
        HBox paramBox = FXUtils.newHBox();
        Parameter.Base<?> parameter = null;
        if (itemNr >= 0) {
        	parameter = (Parameter.Base<?>) ((List<?>) input.get()).get(itemNr);
        } else {
        	parameter = (Parameter.Base<?>) input.get();
        }

        if (parameter == null) {
            super.addComboBox(box, input, beastObject);
        } else {
            setUpEntry();
            paramBox.getChildren().add(m_entry);
            FXUtils.createHMCButton(paramBox, m_beastObject, m_input);
            if (doc.allowLinking) {
	            boolean isLinked = doc.isLinked(m_input);
				if (isLinked || doc.suggestedLinks((BEASTInterface) m_input.get()).size() > 0) {
		            Button linkbutton = new Button();
		            linkbutton.setGraphic(FXUtils.getIcon(BEASTObjectDialog.ICONPATH +
//		            		new ImageView(BEASTObjectDialog.ICONPATH + 
		            		(isLinked ? "link.png" : "unlink.png")));
		            // linkbutton.setBorder(BorderFactory.createEmptyBorder());
		            linkbutton.setTooltip(new Tooltip("link/unlink this parameter with another compatible parameter"));
		            linkbutton.setOnAction(e -> {
							if (doc.isLinked(m_input)) {
								// unlink
								try {
									BEASTInterface candidate = doc.getUnlinkCandidate(m_input, m_beastObject);
									m_input.setValue(candidate, m_beastObject);
									doc.deLink(m_input);
								} catch (RuntimeException e2) {
									e2.printStackTrace();
									Alert.showMessageDialog(this, "Could not unlink: " + e2.getMessage());
								}
								
							} else {
								// create a link
								List<BEASTInterface> candidates = doc.suggestedLinks((BEASTInterface) m_input.get());
								ComboBox<BEASTInterface> jcb = new ComboBox<>();
								for (BEASTInterface candidate : candidates) {
									jcb.getItems().add(candidate);
								}
                                HBox hbox = new HBox(jcb);
								Alert.showMessageDialog( null, hbox, "select parameter to link with", Alert.QUESTION_MESSAGE);
								BEASTInterface candidate = (BEASTInterface) jcb.getValue();
								if (candidate != null) {
									try {
										m_input.setValue(candidate, m_beastObject);
										doc.addLink(m_input);
									} catch (Exception e2) {
										e2.printStackTrace();
									}
								}
                                sync();
							}
							refreshPanel();
						});
		            paramBox.getChildren().add(linkbutton);
				}
            }            
            
            // paramBox.getChildren().add(new Separator());

            m_isEstimatedBox = new CheckBox(doc.beautiConfig.getInputLabel(parameter, parameter.isEstimatedInput.getName()));
            m_isEstimatedBox.setId(input.getName() + ".isEstimated");
            //((HBox)box).setHgrow(m_isEstimatedBox, Priority.ALWAYS);
            m_isEstimatedBox.setMaxWidth(Double.POSITIVE_INFINITY);
            //m_isEstimatedBox.setPrefWidth(400);
            box.setMaxWidth(Double.POSITIVE_INFINITY);

            if (input.get() != null) {
                m_isEstimatedBox.setSelected(parameter.isEstimatedInput.get());
            }
            m_isEstimatedBox.setTooltip(new Tooltip(parameter.isEstimatedInput.getTipText()));

            boolean isClockRate = false;
            for (Object output : parameter.getOutputs()) {
                if (output instanceof BranchRateModel.Base) {
                    isClockRate |= ((BranchRateModel.Base) output).meanRateInput.get() == parameter;
                }
            }
            m_isEstimatedBox.setDisable(!(!isClockRate || !getDoc().autoSetClockRate));

            m_isEstimatedBox.setOnAction(e -> toggleEstimate());

            if (m_bAddButtons) {
                if (BEASTObjectPanel.countInputs(m_input.get(), doc) > 0) {
                	paramBox.getChildren().add(createEditButton(input));
                }
            }

            paramBox.getChildren().add(m_isEstimatedBox);
            
            // only show the estimate flag if there is an operator that works on this parameter
            m_isEstimatedBox.setVisible(doc.isExpertMode());
            m_isEstimatedBox.setTooltip(new Tooltip("Estimate value of this parameter in the MCMC chain"));
            //m_editPluginButton.setVisible(false);
            //m_bAddButtons = false;
            if (itemNr < 0) {
	            for (Object beastObject2 : ((BEASTInterface) m_input.get()).getOutputs()) {
	                if (beastObject2 instanceof ParametricDistribution) {
	                    m_isEstimatedBox.setVisible(doc.allowLinking);
	                    m_isEstimatedBox.setVisible(true);
	                	isParametricDistributionParameter = true;
	                    break;
	                }
	            }
	            for (Object beastObject2 : ((BEASTInterface) m_input.get()).getOutputs()) {
	                if (beastObject2 instanceof Operator) {
	                    m_isEstimatedBox.setVisible(true);
	                    //m_editPluginButton.setVisible(true);
	                    break;
	                }
	            }
            } else {
	            for (Object beastObject2 : ((BEASTInterface) ((List<?>)m_input.get()).get(itemNr)).getOutputs()) {
	                if (beastObject2 instanceof Operator) {
	                    m_isEstimatedBox.setVisible(true);
	                    //m_editPluginButton.setVisible(true);
	                    break;
	                }
	            }
            }

            box.getChildren().add(paramBox);
        }
    }

    
    public void toggleEstimate() {
    	try {
            Parameter.Base<?> parameter2 = (Parameter.Base<?>) m_input.get();
            parameter2.isEstimatedInput.setValue(m_isEstimatedBox.isSelected(), parameter2);
            if (isParametricDistributionParameter) {
            	if (m_isEstimatedBox.isSelected()) {
            		javafx.scene.control.ButtonType result = Alert.showConfirmDialog(this, 
            				  "You are about to estimate a parameter of aprior and add a hyperprior.\n\n"
            				+ "Hyper priors are rarely used for parametric distribution parameters.\n\n"
            				+ "If you are certain it is useful for you analysis, choose YES, otherwise choose NO", "Hyper prior", Alert.YES_NO_OPTION);
            		if (!result.getText().equals("Yes")) {
            			m_isEstimatedBox.setSelected(false);
            			parameter2.isEstimatedInput.setValue(false, parameter2);
            			return;
            		}
            	}
            	
            	String id = parameter2.getID();
            	

            	if (id.startsWith("RealParameter")) {
                	ParametricDistribution parent = null; 
    	            for (Object beastObject2 : parameter2.getOutputs()) {
    	                if (beastObject2 instanceof ParametricDistribution) {
                    		parent = (ParametricDistribution) beastObject2; 
    	                    break;
    	                }
    	            }
    	            Distribution grandparent = null; 
    	            for (Object beastObject2 : parent.getOutputs()) {
    	                if (beastObject2 instanceof Distribution) {
                    		grandparent = (Distribution) beastObject2; 
    	                    break;
    	                }
    	            }
            		id = "parameter.hyper" + parent.getClass().getSimpleName() + "-" + 
            				m_input.getName() + "-" + grandparent.getID();
            		doc.pluginmap.remove(parameter2.getID());
            		parameter2.setID(id);
            		doc.addPlugin(parameter2);
            	}
            	
            	
            	PartitionContext context = new PartitionContext(id.substring("parameter.".length()));
            	Log.warning.println(context + " " + id);
            	doc.beautiConfig.hyperPriorTemplate.createSubNet(context, true);
            }
        	hardSync();
            refreshPanel();
        } catch (Exception ex) {
            Log.err.println("ParameterInputEditor " + ex.getMessage());
        }
    }
    
    @Override
    protected void addValidationLabel() {
        super.addValidationLabel();
        // make edit button invisible (if it exists) when this parameter is not estimateable
        if (m_editBEASTObjectButton != null)
            m_editBEASTObjectButton.setVisible(m_isEstimatedBox.isVisible());
    }

    @Override
    protected void refresh() {
        Parameter.Base<?> parameter = (Parameter.Base<?>) m_input.get();
		String s = "";
		for (Object d : parameter.valuesInput.get()) {
			s += d + " ";
		}
		m_entry.setText(s);
        m_isEstimatedBox.setSelected(parameter.isEstimatedInput.get());
        repaint();
    }

}
