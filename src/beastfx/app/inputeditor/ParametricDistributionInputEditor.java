package beastfx.app.inputeditor;






import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.commons.math.MathException;

import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.spec.evolution.tree.MRCAPrior;
import beast.base.evolution.tree.TreeDistribution;
import beast.base.inference.distribution.ParametricDistribution;
import beast.base.inference.distribution.Prior;
import beast.base.inference.parameter.RealParameter;
import beastfx.app.util.FXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.control.Label;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

public class ParametricDistributionInputEditor extends BEASTObjectInputEditor {

	public ParametricDistributionInputEditor() {
		super();
	}
    public ParametricDistributionInputEditor(BeautiDoc doc) {
		super(doc);
	}

    boolean useDefaultBehavior;
	boolean mayBeUnstable;

    @Override
    public Class<?> type() {
        //return ParametricDistributionInputEditor.class;
        return ParametricDistribution.class;
    }

    @Override
    public void init(Input<?> input, BEASTInterface beastObject, int itemNr, ExpandOption isExpandOption, boolean addButtons) {
        useDefaultBehavior = !((beastObject instanceof beast.base.inference.distribution.Prior) || beastObject instanceof MRCAPrior || beastObject instanceof TreeDistribution);

//    	if (useDefaultBehavior && false) {
//    		super.init(input, beastObject, isExpandOption, addButtons);
//    	} else {
        m_bAddButtons = addButtons;
        m_input = input;
        m_beastObject = beastObject;
		this.itemNr = itemNr;
        if (input.get() != null) {
            super.init(input, beastObject, itemNr, ExpandOption.TRUE, addButtons);
        } else {
        	pane = new HBox();
        }
        Pane pane1 = pane;
        registerAsListener(pane);        
        pane = FXUtils.newHBox();
        pane.getChildren().add(pane1);
        pane.getChildren().add(createGraph());
        getChildren().add(pane);
//    	}
    } // init


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
        if (useDefaultBehavior) {
            super.addInputLabel();
        }
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
            ParametricDistribution m_distr = (ParametricDistribution)m_input.get();
            if (m_distr == null) {
                drawError();
            } else {
                try {
                    m_distr.initAndValidate();
                    drawGraph(m_distr);
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

		private void drawGraph(ParametricDistribution m_distr) {//, int labelOffset) {
            RealParameter param = getRealParameter();

            double minValue = 0.1;
            double maxValue = 1;
            try {
                minValue = m_distr.inverseCumulativeProbability(0.01);
            } catch (Throwable e) {
                // use default
            }
            try {
                maxValue = m_distr.inverseCumulativeProbability(0.99);
            } catch (Throwable e) {
            	// use default
            }
            if (param != null && minValue < param.getLower()) {
            	minValue = minValue + 0.99999 * (param.getLower() - minValue);
            }
            if (param != null && maxValue > param.getUpper()) {
            	maxValue = param.getUpper() + 0.001 * (maxValue - param.getUpper());
            }
            double xRange = maxValue - minValue;
            // adjust yMax so that the ticks come out right
            double x0 = minValue;
            if (minValue > 0 && minValue - xRange < 0) {
            	minValue = 0 + 1e-5;
            }
//            if (maxValue < 1 && maxValue + xRange > 0) {
//            	maxValue = 1 - 1e-5;
//            }
            xRange = maxValue - minValue;
            int k = 0;
//            double f = maxValue;
//            double f2 = x0;
//            while (f > 10) {
//                f /= 10;
//                f2 /= 10;
//                k++;
//            }
//            while (f < 1 && f > 0) {
//                f *= 10;
//                f2 *= 10;
//                k--;
//            }
//            f = Math.ceil(f);
//            f2 = Math.floor(f2);
//
//            for (int i = 0; i < k; i++) {
//                f *= 10;
//                f2 *= 10;
//            }
//            for (int i = k; i < 0; i++) {
//                f /= 10;
//                f2 /= 10;
//            }
//
//            xRange = xRange + minValue - f2 + f - maxValue;
//
//            minValue = f2;
//            maxValue = f;

            int points;
            if (!m_distr.isIntegerDistribution()) {
                points = POINTS;
            } else {
                points = (int) (xRange);
            }
            double[] xPoints = new double[points];
            double[] fyPoints = new double[points];
            double yMax = 0;
            
            for (int i = 0; i < points; i++) {
            	xPoints[i] = minValue + (xRange * i) / points;
            	double y0 = minValue + (xRange * i) / points;
            	if (param != null && (y0 < param.getLower() || y0 > param.getUpper())) {
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
                    info2 += format(m_distr.inverseCumulativeProbability(quantiles[k]));
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
    private double getDensityForPlot(ParametricDistribution pDistr, double x) {
        if (pDistr.isIntegerDistribution()) {
            return pDistr.density((int) Math.round(x));
        } else {
            return pDistr.density(x);
        }
    }

    public RealParameter getRealParameter() {
    	if (m_beastObject instanceof Prior) {
    		if (((Prior)m_beastObject).m_x.get() instanceof RealParameter) {
    			return (RealParameter) ((Prior)m_beastObject).m_x.get();
    		}
    	}
		return null;
	}
	private Node createGraph() {
    	
    	graphPanel = new PDPanel();
//        int fsize = UIManager.getFont("Label.font").getSize();
//        Dimension2D size = new Dimension2D(200 * fsize / 13, 200 * fsize / 13);
//        //panel.setSize(size);
//        HBox box = FXUtils.newHBox();
//        //box.setBorder(BorderFactory.createEmptyBorder());
//        box.getChildren().add(graphPanel);        
//        box.setPrefSize(size.getHeight(), size.getWidth());
//        box.setMinSize(size.getHeight(), size.getWidth());
        graphPanel.paintComponent();
        return graphPanel;
//        return box;
    }
    
    
    @Override
    public void validateInput() {
		graphPanel.paintComponent();
		super.validateInput();
    }

//    @Override
//    public void validate() {
//        super.validate();
//        repaint();
//    }

}
