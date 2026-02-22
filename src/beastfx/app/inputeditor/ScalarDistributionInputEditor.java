package beastfx.app.inputeditor;


import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.core.Log;
import beast.base.inference.Distribution;
import beast.base.parser.PartitionContext;
import beast.base.spec.Bounded;
import beast.base.spec.domain.*;
import beast.base.spec.evolution.tree.MRCAPrior;
import beast.base.spec.inference.distribution.IID;
import beast.base.spec.inference.distribution.ScalarDistribution;
import beast.base.spec.inference.distribution.TensorDistribution;
import beast.base.spec.inference.distribution.TruncatedReal;
import beast.base.spec.inference.parameter.BoolScalarParam;
import beast.base.spec.inference.parameter.IntScalarParam;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.type.IntScalar;
import beast.base.spec.type.IntVector;
import beast.base.spec.type.RealScalar;
import beast.base.spec.type.RealVector;
import beastfx.app.util.FXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.apache.commons.math.MathException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ScalarDistributionInputEditor extends BEASTObjectInputEditor implements HasExpandBox {

	public ScalarDistributionInputEditor() {
		super();
	}
    public ScalarDistributionInputEditor(BeautiDoc doc) {
		super(doc);
	}

    boolean useDefaultBehavior;
	boolean mayBeUnstable;

    @Override
    public Class<?> type() {
        //return ParametricDistributionInputEditor.class;
        return ScalarDistribution.class;
    }

    
    static List<BeautiSubTemplate> scalarTemplates;
    static List<ScalarDistribution<?,?>> templateInstances;
    static List<Class<?>> templateDomains;
    
    @Override
    public void init(Input<?> input, BEASTInterface beastObject, int itemNr, ExpandOption isExpandOption, boolean addButtons) {
        // useDefaultBehavior = !((beastObject instanceof beast.base.inference.distribution.Prior) || beastObject instanceof MRCAPrior || beastObject instanceof TreeDistribution);
        
    	if (scalarTemplates == null) {
    		Input<ScalarDistribution<?,?>> _input = new Input<>("param", "dummy input");
        	_input.setType(ScalarDistribution.class);
        	scalarTemplates = doc.getInputEditorFactory().getAvailableTemplates(_input, beastObject, null, doc);

        	templateInstances = new ArrayList<>();
        	templateDomains = new ArrayList<>();
            List<?> list = (List<?>) input.get();
            PartitionContext context = doc.getContextFor((BEASTInterface) list.get(itemNr));
            ScalarDistribution<?,?> prior1 = (ScalarDistribution <?,?>) list.get(itemNr);
        	for (BeautiSubTemplate template : scalarTemplates) {
            	ScalarDistribution<?,?> newDist = (ScalarDistribution<?,?>) template.createSubNet(context, prior1, _input, true);
            	templateInstances.add(newDist);
            	templateDomains.add(getDomain(newDist));
        	}
    	}
    	
    	
        useDefaultBehavior = !(beastObject instanceof TensorDistribution); //|| 
        		//((ScalarDistribution<?,?>)beastObject).getApacheDistribution() == null;

        m_bAddButtons = addButtons;
        m_input = input;

        if (beastObject instanceof ScalarDistribution<?, ?>) {
            m_beastObject = beastObject;        	
        } else {
        	// m_input = beastObject.getInput("distr");
            m_beastObject = (ScalarDistribution<?, ?>) beastObject.getInput("distr").get();
        }

        if (useDefaultBehavior && (beastObject instanceof MRCAPrior)) {
        	useDefaultBehavior = false;
        	pane = new HBox();
        	expandedInit(input, beastObject);
        	pane = (Pane) pane.getChildren().get(1);
        	setExpandBox(m_expansionBox);
            registerAsListener(pane);
            getChildren().add(pane);
            return;
        	//useDefaultBehavior = false;
        }
        
        //m_bAddButtons = true;
        
		this.itemNr = itemNr;
        if (input.get() != null) {
            super.init(input, m_beastObject, itemNr, ExpandOption.FALSE, m_bAddButtons);
        } else {
        	pane = new HBox();
        }
        pane.getChildren().add(createComboBox(m_beastObject, m_input));
    	pane.setPadding(new Insets(5));
        if (m_beastObject != null) {
        	FXUtils.createHMCButton(pane, m_beastObject, m_input);
        }
        
        
        
        
        ScalarDistribution<?,?> prior = (ScalarDistribution<?,?>) m_beastObject;
	        if (prior != null) {
	        	if (prior.paramInput.get() instanceof RealScalar p) {
	            // add range button for real parameters
	            Button rangeButton = new Button(paramToString(p));
	            rangeButton.setOnAction(e -> {
	                Button rangeButton1 = (Button) e.getSource();
	
	                List<?> list = (List<?>) m_input.get();
	                ScalarDistribution<?,?> prior1 = (ScalarDistribution<?,?>) list.get(itemNr);
	                BEASTInterface p1 = (BEASTInterface) prior1.paramInput.get();
	                BEASTObjectDialog dlg = new BEASTObjectDialog(p1, RealScalar.class, doc);
	                if (dlg.showDialog()) {
	                    dlg.accept(p1, doc);
	                    ((BEASTInterface)p1).initAndValidate();
	                    rangeButton1.setText(paramToString((RealScalar<?>)p1));
	                    refreshPanel();
	                }
	            });
	            rangeButton.setPrefWidth(InputEditor.Base.LABEL_SIZE.getWidth());
	            rangeButton.setTooltip(new Tooltip("Initial value and range of " + ((BEASTInterface)p).getID()));
	            
	            pane.getChildren().add(rangeButton);
	        } else if (prior.paramInput.get() instanceof IntScalar p) {
	            // add range button for real parameters
	            Button rangeButton = new Button(paramToString(p));
	            rangeButton.setOnAction(e -> {
	                Button rangeButton1 = (Button) e.getSource();
	
	                List<?> list = (List<?>) m_input.get();
	                ScalarDistribution<?,?> prior1 = (ScalarDistribution<?,?>) list.get(itemNr);
	                BEASTInterface p1 = (BEASTInterface) prior1.paramInput.get();
	                BEASTObjectDialog dlg = new BEASTObjectDialog(p1, IntScalar.class, doc);
	                if (dlg.showDialog()) {
	                    dlg.accept(p1, doc);
	                    p1.initAndValidate();
	                    rangeButton1.setText(paramToString((IntScalar<?>)p1));
	                    refreshPanel();
	                }
	            });
	
	            pane.getChildren().add(rangeButton);
	        }
        }
        
        
        
        
        
//        Pane pane1 = pane;
        registerAsListener(pane);        
//        pane = FXUtils.newHBox();
//        pane.getChildren().add(pane1);
//        getChildren().add(pane);

    } // init


	private Class<?> getDomain(ScalarDistribution<?, ?> value) {
		if (value == null) {
			return null;
		}
		
        Type superclass = value.getClass().getGenericSuperclass();
        if (superclass instanceof ParameterizedType pt) {
            Type [] types = pt.getActualTypeArguments();
            while (types[0] instanceof ParameterizedType pt2) {
            	types = pt2.getActualTypeArguments();
            }
        	Class<?> type = (Class<?>) types[0];
        	return type;
        }
        
//        if (superclass instanceof ParameterizedType pt) {
//            Type [] types = pt.getActualTypeArguments();
//            if (types[0] instanceof ParameterizedType pt2) {
//                Type [] types2 = pt2.getActualTypeArguments();
//            	Class<?> type = (Class<?>) types2[0];
//            	return type;
//            }
//        }
        
        Log.warning("Cannot determine Domain of " + value.getClass().getName());
        
		return null;
	}
	
	
    private Class<?> getParameterDomain(Object param) {
    	if (param instanceof RealScalarParam rsp) {
    		return rsp.domainTypeInput.get().getClass();
    	}
    	if (param instanceof IntScalarParam isp) {
    		return isp.domainTypeInput.get().getClass();
    	}
    	if (param instanceof RealScalar rs) {
    		return rs.getDomain().getClass();
    	}
    	if (param instanceof IntScalar is) {
    		return is.getDomain().getClass();
    	}
    	if (param instanceof RealVector rsp) {
    		return rsp.getDomain().getClass();
    	}
    	if (param instanceof IntVector isp) {
    		return isp.getDomain().getClass();
    	}
    	if (param == null && m_beastObject != null) {
    		// check the parent distribution
    		for (Object o : m_beastObject.getOutputs()) {
    			if (o instanceof ScalarDistribution sd) {
    				Object o2 = sd.paramInput.get();
    				if (o2 != null) {
	    				Class<?> class_ = getParameterDomain(o2);
	    				return class_;
    				}
    			}
    		}
    		if (m_beastObject instanceof TensorDistribution sd) {
				try {
    				Input<?> input = sd.getInput("param");
    				if (input != null && input.get() != null) {
	    				Class<?> class_ = getParameterDomain(input.get());
	    				return class_;
    				}
				} catch (Throwable e) {
					// ignore
				}
    		}
    	}
    	return Real.class;
	}

	String paramToString(RealScalar<?> p) {
        Double lower = p.getLower();
        Double upper = p.getUpper();
        return "initial = " + p.get() +
                " [" + (lower == null ? "-\u221E" : lower + "") +
                "," + (upper == null ? "\u221E" : upper + "") + "]";
    }

    String paramToString(IntScalar<?> p) {
        Integer lower = p.getLower();
        Integer upper = p.getUpper();
        return "initial = " + p.get() +
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
    /** suppress combobox **/
    protected void addComboBox(Pane box, Input<?> input, BEASTInterface beastObject0) {
        if (useDefaultBehavior) {
        	super.addComboBox(box, input, beastObject0);
        }
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

    PDPanel graphPanel;
    
    /* class for drawing information for a parametric distribution **/
    class PDPanel extends VBox {
    	
    	LineChart<Number,Number> chart;
    	LineChart.Series<Number,Number> series;
    	Label infoLabel1, infoLabel2, infoLabel3;
    	
        // the margin to the left of y-labels
        private static final int MARGIN_LEFT_OF_Y_LABELS = 5;

        private static final int POINTS = 1000;

        int m_nTicks;

        PDPanel() {
    		NumberAxis xAxis = new NumberAxis();
    		xAxis.setForceZeroInRange(false);
            //xAxis.setLabel("x");                
            NumberAxis yAxis = new NumberAxis();        
            yAxis.setLabel("p(x)");
            chart = new LineChart<Number,Number>(xAxis,yAxis);
            //chart.setAnimated(false);
            chart.setLegendVisible(false);
            chart.setCreateSymbols(false);
            chart.getXAxis().setAutoRanging(true);
            chart.getYAxis().setAutoRanging(true);
            series = new LineChart.Series<>();
	        for (int i = 0; i < POINTS; i++) {
	        	series.getData().add(new XYChart.Data<Number,Number>(0,0));
	        }
	        chart.getData().add(series);
	        getChildren().add(chart);
	        
	    	infoLabel1 = new Label();
	    	infoLabel1.setStyle("-fx-font-size:6pt;");
	    	infoLabel1.setPadding(new Insets(0, 10, 0, MARGIN_LEFT_OF_Y_LABELS));
	    	infoLabel2 = new Label();
	    	infoLabel2.setStyle("-fx-font-size:6pt;");
	    	infoLabel2.setPadding(new Insets(0, 100, 0, MARGIN_LEFT_OF_Y_LABELS));
	    	infoLabel3 = new Label();
	    	infoLabel3.setStyle("-fx-font-size:6pt;");
	    	HBox box = new HBox();
	    	// box.setSpacing(50);
	    	box.setAlignment(Pos.CENTER);
	    	box.getChildren().addAll(infoLabel1, infoLabel2, infoLabel3);
	    	getChildren().add(box);
        }
        
        // @Override
        synchronized private void paintComponent() {
            TensorDistribution<?,?> distr = (TensorDistribution<?,?>) m_beastObject;
            if (distr == null || !(distr instanceof ScalarDistribution)) {
            	try {
            		distr = (TensorDistribution<?,?>) m_beastObject.getInput("distr").get();
            	} catch (Throwable t) {
            		// ignore
            	}
            }
            if (distr == null || !(distr instanceof ScalarDistribution)) {
                drawError();
            } else {
                try {
                    distr.initAndValidate();
                    drawGraph((ScalarDistribution<?,?>) distr);
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                    drawError();
                }
            }

        }

        private void drawError() {
        	// chart.getData().clear();
//            g.setFill(Color.WHITE);
//            g.fillRect(0, 0, getWidth(), getHeight());
//            g.setStroke(Color.BLACK);
//            g.rect(0, 0, getWidth()-1, getHeight()-1);
//
//            String errorString = "Cannot display distribution.";
//            
//            int stringWidth = stringWidth(errorString);
//            int stringHeight = stringHeight(errorString);
//            g.strokeText(errorString,
//                    (getWidth() - stringWidth)/2,
//                    (getHeight() - stringHeight)/2);
        }

		private void drawGraph(ScalarDistribution<?,?> m_distr) {//, int labelOffset) {
            Bounded<?> param = getParameter();

            double minValue = 0.1;
            double maxValue = 1;
            try {
            	minValue = inverseCumulativeProbability(m_distr, 0.01);
            } catch (Throwable e) {
                // use default
            }
            try {
                maxValue = inverseCumulativeProbability(m_distr, 0.99);
            } catch (Throwable e) {
            	// use default
            }
//            double lowerParam = param == null ? 0 :
//            	param.getLower() instanceof Integer ? (int) param.getLower() : (double) param.getLower();
            double lowerParam = (param != null && param.getLower() instanceof Number lower)
                    ? lower.doubleValue() : 0.0;
            if (param != null && minValue < lowerParam) {
            	minValue = minValue + 0.99999 * (lowerParam - minValue);
            }
//            double upperParam = param == null ? 0 :
//            	param.getUpper() instanceof Integer ? (int) param.getUpper() : (double) param.getUpper();
            double upperParam = (param != null && param.getUpper() instanceof Number upper)
                    ? upper.doubleValue() : 0.0;
            if (param != null && maxValue > upperParam) {
//            	maxValue = (double) param.getUpper() + 0.001 * (maxValue - upperParam);
                maxValue = upperParam + 0.001 * (maxValue - upperParam);
            }
            double xRange = maxValue - minValue;
            // adjust yMax so that the ticks come out right
            if (minValue > 0 && minValue - xRange < 0) {
            	minValue = 0 + 1e-5;
            }
            xRange = maxValue - minValue;
            int k = 0;

            int points;
            if (!m_distr.isIntegerDistribution()) {
                points = POINTS;
            } else {
                points = (int) (xRange+1);
            }
            double[] xPoints = new double[points];
            double[] fyPoints = new double[points];
            double yMax = 0;
            
            for (int i = 0; i < points; i++) {
            	xPoints[i] = minValue + (xRange * i) / (points-1);
            	double y0 = minValue + (xRange * i) / (points-1);
            	if (param != null && (y0 < lowerParam || y0 > upperParam)) {
            		fyPoints[i] = 0;
            	} else {
            		fyPoints[i] = getDensityForPlot(m_distr, y0);
            	}
                if (Double.isInfinite(fyPoints[i]) || Double.isNaN(fyPoints[i])) {
                    fyPoints[i] = 0;
                }
                yMax = Math.max(yMax, fyPoints[i]);
            }
            yMax = adjust(yMax);


            for (int i = 0; i < points; i++) {
            	Data<Number, Number> p = series.getData().get(i);
            	p.setXValue(xPoints[i]);
            	p.setYValue(fyPoints[i]);
            }
            synchronized (this) {
                if (chart.getData().size() == 0) {
                	try {
                		chart.getData().add(series);
                	} catch (IllegalArgumentException e) {
                		// ignore
                	}
                }
			}

            String info1 = "", info2 = "", info3 = "";
            String[] strs = new String[]{"2.5% Quantile", "5% Quantile", "Median", "95% Quantile", "97.5% Quantile"};
            Double[] quantiles = new Double[]{0.025, 0.05, 0.5, 0.95, 0.975};
            mayBeUnstable = false;
            for (k = 0; k < 5; k++) {
                try {
                    info2 += format(inverseCumulativeProbability(m_distr, quantiles[k]));
                } catch (MathException | RuntimeException e) {
                	info2 += "not available";
                }
                info1 += strs[k] + "\n";
                info2 += "\n";
            }
            if (mayBeUnstable) {
                info1 += "* numbers\n";
                info1 += "may not be\n";
                info1 += "accurate\n";
            }
            try {
            	info3 += "mean " + format(m_distr.getMean());
            } catch (RuntimeException e) {
                // catch in case it is not implemented.
            }
            infoLabel1.setText(info1);
            infoLabel2.setText(info2);
            infoLabel3.setText(info3);
        }

		private double inverseCumulativeProbability(ScalarDistribution<?, ?> m_distr, double level) throws MathException {
            Object o = m_distr.inverseCumulativeProbability(level);
            if (o instanceof Double d) {
            	return d;
            } else if (o instanceof Integer i) {
               	return i;
            }
			return 0;
		}

		private String format(double value) {
            StringWriter writer = new StringWriter();
            PrintWriter pw = new PrintWriter(writer);
            pw.printf("%.3g", value);
            if (value != 0.0 && Math.abs(value) / 1000 < 1e-320) { // 2e-6 = 2 * AbstractContinuousDistribution.solverAbsoluteAccuracy
            	mayBeUnstable = true;
            	pw.printf("*");
            }
            pw.flush();
            return writer.toString();
        }
        
        private double adjust(double yMax) {
            // adjust yMax so that the ticks come out right
            int k = 0;
            double y = yMax;
            while (y > 10) {
                y /= 10;
                k++;
            }
            while (y < 1 && y > 0) {
                y *= 10;
                k--;
            }
            y = Math.ceil(y);
            m_nTicks = NR_OF_TICKS[(int) y];
            for (int i = 0; i < k; i++) {
                y *= 10;
            }
            for (int i = k; i < 0; i++) {
                y /= 10;
            }
            return y;
        }
    }
    
    /**
     * Returns the density of pDistr at x when pDistr is a density of a
     * continuous variable, but returns the probability of the closest
     * integer when pDistr is a probability distribution over an integer-valued
     * parameter.
     * 
     * @param pDistr
     * @param x
     * @return density at x or probability of closest integer to x
     */
    private double getDensityForPlot(ScalarDistribution<?,?> distr, double x) {
        if (distr.isIntegerDistribution()) {
            return distr.density((int) Math.round(x));
        } else {
            return distr.density(x);
        }
    }

    public Bounded<?> getParameter() {
    	if (m_beastObject instanceof TensorDistribution td) {
    		Object o = td.paramInput.get();
    		if (o instanceof Bounded b) {
    			return b;
    		}
    	}
		return null;
	}
	private Node createGraph() {
    	graphPanel = new PDPanel();
        graphPanel.paintComponent();
        return graphPanel;
    }
    
    
    @Override
    public void validateInput() {
    	if (graphPanel != null) {
    		if (m_beastObject instanceof TruncatedReal trd) {
    			trd.distributionInput.get().refresh();
    		} else if (m_beastObject.getClass().getName().endsWith("TruncatedIntDistribution")) {
    			((ScalarDistribution)m_beastObject.getInput("distribution").get()).refresh();
    		} else if (m_beastObject.getClass().getName().endsWith("OffsetRealDistribution")) {
    			((ScalarDistribution)m_beastObject.getInput("distribution").get()).refresh();
    		}
    		graphPanel.paintComponent();
    	}
		super.validateInput();
    }

    
	private ComboBox<BeautiSubTemplate> comboBox;

	protected ComboBox<BeautiSubTemplate> createComboBox(BEASTInterface m_beastObject, Input<?> m_input) {
		ComboBox<BeautiSubTemplate> comboBox = new ComboBox<>();

        TensorDistribution<?,?> prior = (TensorDistribution<?,?>) m_beastObject;
        
        String text = prior == null ? "[[none]]" : 
        		prior.paramInput.get() != null ? ((BEASTInterface)prior.paramInput.get()).getID() : prior.getID();

        int k = 0;
//        ScalarDistribution<?,?> distr = (ScalarDistribution<?,?>) (
//        		(prior instanceof ScalarDistribution<?,?>) ?
//        				m_beastObject :
//        				m_beastObject.getInput("distr").get());
        Object param =
        		m_beastObject != null?
        		m_beastObject.getInput("param").get() : 
        		null;
        Class<?> domain = getParameterDomain(param);
        boolean isReal = Real.class.isAssignableFrom(domain);
        for (BeautiSubTemplate template : scalarTemplates) {
        	if (isCompatible(domain, templateDomains.get(k++)) || 
        		(//param != null && 
        			(template.getID().equals("BoundedReal") && isReal  ||
        			 template.getID().equals("BoundedInt")  && !isReal ||
        			 template.getID().equals("OffsetReal")  && isReal  || 
        			 template.getID().equals("OffsetInt")   && !isReal))) {
//        		if (!(template.getID().equals("BoundedReal") && isReal   || 
//        			  template.getID().equals("BoundedInt")  && !isReal  || 
//        			  template.getID().equals("OffsetReal")  && isReal   || 
//        			  template.getID().equals("OffsetInt")   && !isReal) || 
//        				param != null) {
        			comboBox.getItems().add(template);
//        		}
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
        				setText(item.toString() + getParameters());
        			} else {
        				setText(item.toString() + getParameters());
        			}
                } else {
                    setText(null);
                }
        	}
        });

        String id = prior != null ? prior.getID() : null;

        id = prior != null ? prior.getClass().getName() : "[[none]]";
        if (prior != null && !(prior instanceof ScalarDistribution<?,?>)) {
        	id = ((BEASTInterface)prior.getInput("distr").get()).getClass().getName();
        }
        		        		// id.substring(0, id.indexOf('.'));
        for (BeautiSubTemplate template : scalarTemplates) {
            if (template.classInput.get() != null && template._class.getName().equals(id)) {
                comboBox.setValue(template);
            }
        }
        comboBox.setOnAction(e -> {
            @SuppressWarnings("unchecked")
			ComboBox<BeautiSubTemplate> comboBox1 = (ComboBox<BeautiSubTemplate>) e.getSource();

            
            try {
	            if (m_input.get() instanceof List) {
		            List<Distribution> list = (List<Distribution>) m_input.get();
		
		            BeautiSubTemplate template = (BeautiSubTemplate) comboBox1.getValue();
		            PartitionContext context = doc.getContextFor((BEASTInterface) list.get(itemNr));
		            
		            Object item = list.get(itemNr);
		            if (item instanceof ScalarDistribution<?,?>) {
			            ScalarDistribution<?,?> prior1 = null;
		            	prior1 = (ScalarDistribution<?,?>) list.get(itemNr);
		            	Object o = ((ScalarDistribution<?,?>) m_beastObject).paramInput.get();
		            	((BEASTInterface) o).getOutputs().remove(prior1);
		            	Input<ScalarDistribution<?,?>> input_ = new Input<>("param", "dummy input");
		            	input_.setType(ScalarDistribution.class);
		            	ScalarDistribution<?,?> newDist = (ScalarDistribution<?,?>) template.createSubNet(context, prior1, input_, true);
		            	newDist.paramInput.setValue(o, newDist);
		            	list.set(itemNr, newDist);
		            	newDist.setID(m_beastObject.getID());
		            	doc.pluginmap.remove(m_beastObject.getID());
		            	doc.registerPlugin(newDist);
		            } else {
		            	ScalarDistribution<?,?> newDist = (ScalarDistribution<?,?>) template.createSubNet(context, m_beastObject, m_beastObject.getInput("distr"), true);
		            }
	            } else {
		    		
		            BeautiSubTemplate template = (BeautiSubTemplate) comboBox1.getValue();
		            PartitionContext context = doc.getContextFor((BEASTInterface) m_beastObject);
	            	ScalarDistribution<?,?> newDist = (ScalarDistribution<?,?>) template.createSubNet(context, m_beastObject, m_input, true);
	            }
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
        
        String tipText = m_beastObject != null ? getDoc().tipTextMap.get(m_beastObject.getID()) : null;
        if (tipText != null) {
        	comboBox.setTooltip(new Tooltip(tipText));
        }
        
        
        return comboBox;

	}
	
    
	private boolean isCompatible(Class<?> paramDomain, Class<?> templateDomain) {
    	if (templateDomain == null) {
    		// the "no prior" template should now be rejected
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
	    		if (o != null) {
	    			if (o instanceof RealScalarParam<?> p) {
		    			if (b == null) {
		    				b = new StringBuilder();
		    				b.append((p.get()+"").trim());
		    			} else {
		    				b.append(',');
		    				b.append((p.get()+"").trim());
		    			}
	    			} else if (o instanceof IntScalarParam p) {
		    			if (b == null) {
		    				b = new StringBuilder();
		    				b.append((p.get()+"").trim());
		    			} else {
		    				b.append(',');
		    				b.append((p.get()+"").trim());
		    			}
	    			} else if (o instanceof BoolScalarParam p) {
		    			if (b == null) {
		    				b = new StringBuilder();
		    				b.append((p.get()+"").trim());
		    			} else {
		    				b.append(',');
		    				b.append((p.get()+"").trim());
		    			}
	    				
	    			} else if (o instanceof Double && !input.getName().equals("offset")) {
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
    	}
		if (b == null) {
			return "";
		}
		return "[" + b.toString().replaceAll("[\\]\\[]", "") + "]";
	}
    
    VBox expandBox = null;
    
    @Override
	public void setExpandBox(VBox expandBox) {
		this.expandBox = expandBox;

		if (m_beastObject.getClass().getName().endsWith("TruncatedReal")
				|| m_beastObject.getClass().getName().endsWith("TruncatedInt")
				|| m_beastObject.getClass().getName().endsWith("OffsetReal")
				|| m_beastObject.getClass().getName().endsWith("OffsetInt")) {
			((ScalarDistribution)m_beastObject.getInput("distribution").get()).refresh();
			Input input = m_beastObject.getInput("distribution");
			ComboBox<?> distributionComboBox = createComboBox((BEASTInterface)input.get(), input);
			if (distributionComboBox != null) {
				expandBox.getChildren().set(0, distributionComboBox);
			}
			
	        List<InputEditor> editors = doc.getInputEditorFactory().addInputs(expandBox, (BEASTInterface)input.get(), this, null, doc);
			
//		} else if (m_beastObject instanceof TruncatedIntDistribution td) {
//			expandBox.getChildren().set(0, createComboBox(td.distributionInput.get(), td.distributionInput));
//			
//	        List<InputEditor> editors = doc.getInputEditorFactory().addInputs(expandBox, td.distributionInput.get(), this, null, doc);
	        
	        processVbox(editors);
//		} else if (m_beastObject instanceof OffsetRealDistribution od) {
//			expandBox.getChildren().set(0, createComboBox(od.distributionInput.get(), od.distributionInput));
//			
//	        List<InputEditor> editors = doc.getInputEditorFactory().addInputs(expandBox, od.distributionInput.get(), this, null, doc);
//	        
//	        processVbox(editors);
		}
		if (m_beastObject instanceof IID<?, ?, ?>) {
			Input input = m_beastObject.getInput("distr");
			ComboBox<?> distributionComboBox = createComboBox((BEASTInterface)input.get(), input);
			if (distributionComboBox != null) {
				expandBox.getChildren().set(0, distributionComboBox);
			}
			
	        List<InputEditor> editors = doc.getInputEditorFactory().addInputs(expandBox, (BEASTInterface)input.get(), this, null, doc);
	        
	        processVbox(editors);
		}
	

		removeBorder(expandBox);

		VBox vbox = FXUtils.newVBox();
		vbox.getChildren().addAll(expandBox.getChildren());
		HBox hbox = FXUtils.newHBox();
		hbox.getChildren().add(vbox);
		hbox.getChildren().add(createGraph());
		
		expandBox.getChildren().clear();
		expandBox.getChildren().add(hbox);

		vbox.setBorder(new Border(new BorderStroke(Color.LIGHTGRAY, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(1))));

		for (Node node : vbox.getChildren()) {
			if (node instanceof InputEditor ie) {
				ie.addValidationListener(this);
			}
		}
		
		this.expandBox.visibleProperty().addListener((o, oldVal, newVal) -> {
			if (comboBox != null) {
				Pane parent = (Pane) comboBox.getParent();
				int i = parent.getChildren().indexOf(comboBox);
				comboBox = createComboBox(m_beastObject, m_input);
				parent.getChildren().set(i, comboBox);
			}
     });
	}

    private void processVbox(List<InputEditor> editors) {
        // move editors in list to the front of the expandBox
        expandBox.getChildren().removeAll(editors);
        
        for (int i = editors.size() - 1; i >= 0; i--) {
        	InputEditor e = editors.get(i);
        	expandBox.getChildren().add(1, e.getComponent());
        }
//		for (InputEditor editor : editors) {
//			editor.addValidationListener(this);
//		}
	}
    
	private void removeBorder(Node node) {
		if (node instanceof VBox vb) {
			vb.setBorder(new Border(new BorderStroke(Color.LIGHTGRAY, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(0))));
			for (Node n : vb.getChildren()) {
				removeBorder(n);
			}
		} else if (node instanceof HBox hb) {
			hb.setBorder(new Border(new BorderStroke(Color.LIGHTGRAY, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(0))));
			for (Node n : hb.getChildren()) {
				removeBorder(n);
			}
		} else if (node instanceof ScalarDistributionInputEditor sc) {
			removeBorder(sc.pane);
		}
	}

	
}
