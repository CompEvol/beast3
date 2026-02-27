package beastfx.app.beauti;



import java.util.ArrayList;
import java.util.List;

import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import beastfx.app.inputeditor.BeautiDoc;
import beastfx.app.inputeditor.ListInputEditor;
import beastfx.app.inputeditor.SmallLabel;
import beastfx.app.util.FXUtils;
import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.core.Log;
import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.branchratemodel.BranchRateModel;
import beast.base.inference.MCMC;
import beast.base.inference.Operator;
import beast.base.inference.operator.DeltaExchangeOperator;
import beast.base.inference.parameter.IntegerParameter;
import beast.base.inference.parameter.RealParameter;





public class ClockModelListInputEditor extends ListInputEditor {
    List<TextField> textFields = new ArrayList<>();
    List<Operator> operators = new ArrayList<>();

	public ClockModelListInputEditor(BeautiDoc doc) {
		super(doc);
	}

    public ClockModelListInputEditor() {
		super();
	}

	@Override
    public Class<?> type() {
        return List.class;
    }

    @Override
    public Class<?> baseType() {
    	// disable this editor
    	return ClockModelListInputEditor.class;
        //return BranchRateModel.Base.class;
    }

    CheckBox fixMeanRatesCheckBox;
    
    DeltaExchangeOperator operator;
    protected SmallLabel fixMeanRatesValidateLabel;
    
    @Override
    public void init(Input<?> input, BEASTInterface beastObject, int itemNr, ExpandOption isExpandOption, boolean addButtons) {
    	fixMeanRatesCheckBox = new CheckBox("Fix mean rate of clock models");
    	m_buttonStatus = ButtonStatus.NONE;
    	super.init(input, beastObject, itemNr, isExpandOption, addButtons);
    	
		List<Operator> operators = ((MCMC) doc.mcmc.get()).operatorsInput.get();
    	fixMeanRatesCheckBox.setOnAction(e -> {
    		CheckBox averageRatesBox = (CheckBox) e.getSource();
				boolean averageRates = averageRatesBox.isSelected();
				List<Operator> operators2 = ((MCMC) doc.mcmc.get()).operatorsInput.get();
				if (averageRates) {
					// connect DeltaExchangeOperator
					if (!operators2.contains(operator)) {
						operators2.add(operator);
					}
					// set up relative weights
					setUpOperator();
				} else {
					operators2.remove(operator);
					fixMeanRatesValidateLabel.setVisible(false);
					repaint();
				}
			});

    	operator = (DeltaExchangeOperator) doc.pluginmap.get("FixMeanRatesOperator");
    	if (operator == null) {
    		operator = new DeltaExchangeOperator();
    		try {
    			operator.setID("FixMeanRatesOperator");
				operator.initByName("weight", 2.0, "delta", 0.75);
			} catch (Exception e1) {
				// ignore initAndValidate exception
			}
    		doc.addPlugin(operator);
    	}
		fixMeanRatesCheckBox.setSelected(operators.contains(operator));
		HBox box = FXUtils.newHBox();
		box.getChildren().add(fixMeanRatesCheckBox);
		// box.getChildren().add(new Separator());
		fixMeanRatesValidateLabel = new SmallLabel("x", "green");
		fixMeanRatesValidateLabel.setVisible(false);
		box.getChildren().add(fixMeanRatesValidateLabel);
		
    	if (((List<?>) input.get()).size() > 1 && operator != null) {
    		pane.getChildren().add(box);
    	}
		setUpOperator();
    }
    
    @Override
    public void validateInput() {
    	super.validateInput();
    	Log.warning.println("validateInput()");
    }
    
    /** set up relative weights and parameter input **/
    private void setUpOperator() {
    	String weights = "";
    	List<RealParameter> parameters = operator.parameterInput.get();
    	parameters.clear();
    	double commonClockRate = -1;
    	boolean isAllClocksAreEqual = true;
		try {
	    	for (int i = 0; i < doc.alignments.size(); i++) {
	    		Alignment data = doc.alignments.get(i); 
	    		int weight = data.getSiteCount();
	    		BranchRateModel.Base clockModel = (BranchRateModel.Base) doc.clockModels.get(i);
	    		if (clockModel.meanRateInput.get() instanceof RealParameter) {	    		
	    			RealParameter clockRate = (RealParameter) clockModel.meanRateInput.get();
		    		//clockRate.m_bIsEstimated.setValue(true, clockRate);
		    		if (clockRate.isEstimatedInput.get()) {
		    			if (commonClockRate < 0) {
		    				commonClockRate = clockRate.valuesInput.get().get(0);
		    			} else {
		    				if (Math.abs(commonClockRate - clockRate.valuesInput.get().get(0)) > 1e-10) {
		    					isAllClocksAreEqual = false;
		    				}
		    			}
	    				weights += weight + " ";
		    			parameters.add(clockRate);
		    		}
	    		}
	    	}
	    	if (!fixMeanRatesCheckBox.isSelected()) {
	    		fixMeanRatesValidateLabel.setVisible(false);
	    		return;
	    	}
	    	if (parameters.size() == 0) {
	    		fixMeanRatesValidateLabel.setVisible(true);
	    		fixMeanRatesValidateLabel.setColor("red");
	    		fixMeanRatesValidateLabel.setTooltip(new Tooltip("The model is invalid: At least one clock rate should be estimated."));
	    		return;
	    	}

	    	IntegerParameter weightParameter = new IntegerParameter(weights);
			weightParameter.setID("weightparameter");
			weightParameter.isEstimatedInput.setValue(false, weightParameter);
	    	operator.parameterWeightsInput.setValue(weightParameter, operator);
	    	if (!isAllClocksAreEqual) {
	    		fixMeanRatesValidateLabel.setVisible(true);
	    		fixMeanRatesValidateLabel.setColor("orange");
	    		fixMeanRatesValidateLabel.setTooltip(new Tooltip("Not all clocks are equal. Are you sure this is what you want?"));
	    	} else if (parameters.size() == 1) {
	    		fixMeanRatesValidateLabel.setVisible(true);
	    		fixMeanRatesValidateLabel.setColor("orange");
	    		fixMeanRatesValidateLabel.setTooltip(new Tooltip("At least 2 clock models should have their rate estimated"));
	    	} else if (parameters.size() < doc.alignments.size()) {
	    		fixMeanRatesValidateLabel.setVisible(true);
	    		fixMeanRatesValidateLabel.setColor("orange");
	    		fixMeanRatesValidateLabel.setTooltip(new Tooltip("Not all partitions have their rate estimated"));
	    	} else {
	    		fixMeanRatesValidateLabel.setVisible(false);
	    	}
			repaint();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

} // OperatorListInputEditor
