package beastfx.app.inputeditor;







import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.inference.Distribution;
import beast.base.parser.PartitionContext;
import beast.base.spec.domain.Int;
import beast.base.spec.domain.NonNegativeInt;
import beast.base.spec.domain.NonNegativeReal;
import beast.base.spec.domain.PositiveInt;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.distribution.IID;
import beast.base.spec.inference.distribution.ScalarDistribution;
import beast.base.spec.inference.distribution.TensorDistribution;
import beast.base.spec.inference.parameter.BoolScalarParam;
import beast.base.spec.inference.parameter.IntScalarParam;
import beast.base.spec.inference.parameter.IntVectorParam;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.inference.parameter.RealVectorParam;
import beast.base.spec.type.IntScalar;
import beast.base.spec.type.IntVector;
import beast.base.spec.type.RealScalar;
import beast.base.spec.type.RealVector;
import beast.base.spec.type.Scalar;
import beastfx.app.util.FXUtils;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;

public class IIDInputEditor extends ScalarDistributionInputEditor {

	public IIDInputEditor() {
		super();
	}
    public IIDInputEditor(BeautiDoc doc) {
		super(doc);
	}

    //boolean useDefaultBehavior;
	boolean mayBeUnstable;

    @Override
    public Class<?> type() {
        //return ParametricDistributionInputEditor.class;
        return IID.class;
    }

    
    static List<BeautiSubTemplate> tensorTemplates;
    static List<TensorDistribution<?,?>> templateInstances;
    static List<Class<?>> templateDomains;
    
    @Override
    public void init(Input<?> input, BEASTInterface beastObject, int itemNr, ExpandOption isExpandOption, boolean addButtons) {
        // useDefaultBehavior = !((beastObject instanceof beast.base.inference.distribution.Prior) || beastObject instanceof MRCAPrior || beastObject instanceof TreeDistribution);
        
    	if (tensorTemplates == null) {
    		Input<TensorDistribution<?,?>> _input = new Input<>("param", "dummy input");
        	_input.setType(TensorDistribution.class);
        	tensorTemplates = doc.getInputEditorFactory().getAvailableTemplates(_input, beastObject, null, doc);

        	templateInstances = new ArrayList<>();
        	templateDomains = new ArrayList<>();
            List<?> list = (List<?>) input.get();
            PartitionContext context = doc.getContextFor((BEASTInterface) list.get(itemNr));
            TensorDistribution<?,?> prior1 = (TensorDistribution <?,?>) list.get(itemNr);
        	for (BeautiSubTemplate template : tensorTemplates) {
        		TensorDistribution<?,?> newDist = (TensorDistribution<?,?>) template.createSubNet(context, prior1, _input, true);
            	templateInstances.add(newDist);
            	templateDomains.add(getDomain(newDist));
        	}
    	}
    	
    	
        //useDefaultBehavior = true;

        m_bAddButtons = addButtons;
        m_input = input;
        m_beastObject = beastObject;
		this.itemNr = itemNr;
        pane = new HBox();
        addInputLabel();
            //super.init(input, beastObject, itemNr, ExpandOption.FALSE, addButtons);
        ComboBox<?> distributionComboBox = createComboBox();
        if (distributionComboBox != null) {
        	pane.getChildren().add(distributionComboBox);
        }
        FXUtils.createHMCButton(pane, m_beastObject, m_input);
    	pane.setPadding(new Insets(5));
        getChildren().add(pane);
 
        
        
        
        TensorDistribution<?,?> prior = (TensorDistribution<?,?>) beastObject;
        if (prior.paramInput.get() instanceof RealVector p) {
            // add range button for real parameters
            Button rangeButton = new Button(paramToString(p));
            rangeButton.setOnAction(e -> {
                Button rangeButton1 = (Button) e.getSource();

                List<?> list = (List<?>) m_input.get();
                TensorDistribution<?,?> prior1 = (TensorDistribution<?,?>) list.get(itemNr);
                BEASTInterface p1 = (BEASTInterface) prior1.paramInput.get();
                BEASTObjectDialog dlg = new BEASTObjectDialog(p1, RealScalar.class, doc);
                if (dlg.showDialog()) {
                    dlg.accept(p1, doc);
                    ((BEASTInterface)p1).initAndValidate();
                    rangeButton1.setText(paramToString((RealVector<?>)p1));
                    refreshPanel();
                }
            });
            rangeButton.setPrefWidth(InputEditor.Base.LABEL_SIZE.getWidth());
            rangeButton.setTooltip(new Tooltip("Initial value and range of " + ((BEASTInterface)p).getID()));
            
            pane.getChildren().add(rangeButton);
        } else if (prior.paramInput.get() instanceof IntVector p) {
            // add range button for real parameters
            Button rangeButton = new Button(paramToString(p));
            rangeButton.setOnAction(e -> {
                Button rangeButton1 = (Button) e.getSource();

                List<?> list = (List<?>) m_input.get();
                TensorDistribution<?,?> prior1 = (TensorDistribution<?,?>) list.get(itemNr);
                BEASTInterface p1 = (BEASTInterface) prior1.paramInput.get();
                BEASTObjectDialog dlg = new BEASTObjectDialog(p1, IntScalar.class, doc);
                if (dlg.showDialog()) {
                    dlg.accept(p1, doc);
                    p1.initAndValidate();
                    rangeButton1.setText(paramToString((IntVector<?>)p1));
                    refreshPanel();
                }
            });

            pane.getChildren().add(rangeButton);
        }
        
        
        
        
        
//        Pane pane1 = pane;
        registerAsListener(pane);        
//        pane = FXUtils.newHBox();
//        pane.getChildren().add(pane1);
//        getChildren().add(pane);

    } // init


	private Class<?> getDomain(TensorDistribution<?, ?> value) {
		if (value == null) {
			return null;
		}
		
    	if (value instanceof ScalarDistribution<?,?>) {
        	return null;
    	}

		return getParameterDomain(value.getInput("param").get());
//        Type superclass = value.getInput("param").get().getClass().getGenericSuperclass();
//        if (superclass instanceof ParameterizedType pt) {
//            Type [] types = pt.getActualTypeArguments();
//            while (types[0] instanceof ParameterizedType pt2) {
//            	types = pt2.getActualTypeArguments();
//            }
//        	Class<?> type = (Class<?>) types[0];
//        	return type;
//        }
//        
//        Log.warning("Cannot determine Domain of " + value.getClass().getName());
//        
//		return null;
	}
	
	
    private Class<?> getParameterDomain(Object param) {
    	if (param instanceof RealVectorParam rvp) {
    		return rvp.domainTypeInput.get().getClass();
    	}
    	if (param instanceof IntVectorParam ivp) {
    		return ivp.domainTypeInput.get().getClass();
    	}
    	if (param instanceof RealVector rs) {
    		return rs.getDomain().getClass();
    	}
    	if (param instanceof IntVector is) {
    		return is.getDomain().getClass();
    	}
        return Scalar.class;
	}

	String paramToString(RealVector<?> p) {
        Double lower = p.getLower();
        Double upper = p.getUpper();
        List<Double> values = p.getElements();
        Object [] values2 = values.toArray();
        return "initial = " + Arrays.toString(values2) +
                " [" + (lower == null ? "-\u221E" : lower + "") +
                "," + (upper == null ? "\u221E" : upper + "") + "]";
    }

    String paramToString(IntVector<?> p) {
        Integer lower = p.getLower();
        Integer upper = p.getUpper();
        List<Integer> values = p.getElements();
        Object [] values2 = values.toArray();
        return "initial = " + Arrays.toString(values2) +
                " [" + (lower == null ? "-\u221E" : lower + "") +
                "," + (upper == null ? "\u221E" : upper + "") + "]";
    }

    private void registerAsListener(Node node) {
		if (node instanceof InputEditor) {
			((InputEditor)node).addValidationListener(_this);
		}
		if (node instanceof Pane) {
			for (Node child : ((Pane)node).getChildren()) {
				registerAsListener(child);
			}
		}
	}
    
	@Override
    /** suppress label **/
    protected void addComboBox(Pane box, Input<?> input, BEASTInterface beastObject0) {
    }

    @Override
    /** suppress input label**/
    protected void addInputLabel() {
        String name = formatName(m_beastObject.getID());
        boolean b = m_bAddButtons;
        m_bAddButtons = true;
        addInputLabel(name, m_input.getTipText());
        m_bAddButtons = b;
    }

    /**
     * maps most significant digit to nr of ticks on graph *
     */
    final static int[] NR_OF_TICKS = new int[]{5, 10, 8, 6, 8, 10, 6, 7, 8, 9, 10};

    

    
	private ComboBox<BeautiSubTemplate> comboBox;

	protected ComboBox<BeautiSubTemplate> createComboBox() {
		ComboBox<BeautiSubTemplate> comboBox = new ComboBox<>();

        TensorDistribution<?,?> prior = (TensorDistribution<?,?>) m_beastObject;
        String text = ((BEASTInterface)prior.paramInput.get()).getID();

        int k = 0;
        TensorDistribution<?,?> distr = (TensorDistribution<?,?>) m_beastObject;
        Object param = distr.paramInput.get();
        Class<?> domain = getParameterDomain(param);
        for (BeautiSubTemplate template : tensorTemplates) {
        	if (isCompatible(domain, templateDomains.get(k++))) {
        		comboBox.getItems().add(template);
        	}
        }
        
        if (comboBox.getItems().size() == 0) {
        	return null;
        }
        
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

        String id = prior.getID();

        id = prior.getClass().getName();
        		// id.substring(0, id.indexOf('.'));
        for (BeautiSubTemplate template : tensorTemplates) {
            if (template.classInput.get() != null && template._class.getName().equals(id)) {
                comboBox.setValue(template);
            }
        }
        comboBox.setOnAction(e -> {
            @SuppressWarnings("unchecked")
			ComboBox<BeautiSubTemplate> comboBox1 = (ComboBox<BeautiSubTemplate>) e.getSource();

            List<Distribution> list = (List<Distribution>) m_input.get();

            BeautiSubTemplate template = (BeautiSubTemplate) comboBox1.getValue();
            PartitionContext context = doc.getContextFor((BEASTInterface) list.get(itemNr));
            TensorDistribution<?,?> prior1 = (TensorDistribution<?,?>) list.get(itemNr);
            try {
            	Object o = ((TensorDistribution<?,?>) m_beastObject).paramInput.get();
            	Input<TensorDistribution<?,?>> input_ = new Input<>("param", "dummy input");
            	input_.setType(TensorDistribution.class);
            	TensorDistribution<?,?> newDist = (TensorDistribution<?,?>) template.createSubNet(context, prior1, input_, true);
            	newDist.paramInput.setValue(o, newDist);
            	list.set(itemNr, newDist);
            	newDist.setID(m_beastObject.getID());
            	doc.pluginmap.remove(m_beastObject.getID());
            	doc.registerPlugin(newDist);
            } catch (Exception e1) {
                e1.printStackTrace();
            }

//            if (prior1.distInput.get() instanceof Dirichlet) {
//            	Input<Function> alphaInput = ((Dirichlet)prior1.distInput.get()).alphaInput;
//            	Function f = alphaInput.get();
//            	if (f instanceof RealParameter) {
//            		((RealParameter)f).setDimension(prior1.m_x.get().getDimension());
//            	}
//            }
            
            sync();
            refreshPanel();
        });
        
        String tipText = getDoc().tipTextMap.get(m_beastObject.getID());
        if (tipText != null) {
        	comboBox.setTooltip(new Tooltip(tipText));
        }
        
        return comboBox;

	}
	
    
	private boolean isCompatible(Class<?> paramDomain, Class<?> templateDomain) {
    	if (templateDomain == null) {
    		// the "no prior" and ScalarDistribution templates should be rejected
    		return false;
    	}
    	
		if (Real.class.isAssignableFrom(paramDomain)) {
    		// check type first
    		if (!(Real.class.isAssignableFrom(templateDomain))) {
    			return false;
    		}
    		// more range checks here
    		if  (paramDomain == Real.class) {
    			return true;
    		}
    		if (templateDomain == paramDomain) {
    			return true;
    		}
    		if (templateDomain == NonNegativeReal.class && paramDomain == PositiveReal.class) {
    			return true;
    		}
    		if (templateDomain == PositiveReal.class && paramDomain == NonNegativeReal.class) {
    			return true;
    		}
    		return false;
    	}
    	
		if (Int.class.isAssignableFrom(paramDomain)) {
    		// check type first
    		if (!(Int.class.isAssignableFrom(templateDomain))) {
    			return false;
    		}
    		// more range checks here
    		if  (paramDomain == Int.class) {
    			return true;
    		}
    		if (templateDomain == paramDomain) {
    			return true;
    		}
    		if (templateDomain == NonNegativeInt.class && paramDomain == PositiveInt.class) {
    			return true;
    		}
    		if (templateDomain == PositiveInt.class && paramDomain == NonNegativeInt.class) {
    			return true;
    		}
    		return false;
    	}
    	
    	// don't know how to handle -- err on the side of caution and accept anything
		return true;
	}

    
	private String getParameters() {
    	StringBuilder b = null;
    	TensorDistribution<?,?> distr = (TensorDistribution<?,?>) m_beastObject;
    	for (Input<?> input: distr.listInputs()) {
    		if (!input.getName().equals("param")) {
    		Object o = input.get();
	    		if (o != null && (o instanceof RealScalarParam<?> ||o instanceof IntScalarParam || o instanceof BoolScalarParam)) {
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
    	}
		if (b == null) {
			return "";
		}
		return "[" + b.toString().replaceAll("[\\]\\[]", "") + "]";
	}
	
}
