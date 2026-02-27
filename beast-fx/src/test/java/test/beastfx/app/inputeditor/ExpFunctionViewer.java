package test.beastfx.app.inputeditor;




import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.univariate.BrentOptimizer;
import org.apache.commons.math3.optim.univariate.SearchInterval;
import org.apache.commons.math3.optim.univariate.UnivariateObjectiveFunction;
import org.apache.commons.math3.optim.univariate.UnivariatePointValuePair;

import beast.base.core.Log;
import beast.base.evolution.substitutionmodel.Frequencies;
import beast.base.evolution.substitutionmodel.GeneralSubstitutionModel;
import beast.base.inference.parameter.RealParameter;
import beast.base.util.Randomizer;
import beastfx.app.util.Alert;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ExpFunctionViewer extends javafx.application.Application {
	final static int N = 6;
	
	double a0 = 0.25, a1 = 0.0, b1=0.25, b2 = 0.75, a2=-0.25;
	LineChart<Number,Number> chart;
	Label a0label, a1label, a2label, b1label, b2label;
	DecimalFormat format = new DecimalFormat("#.##");
	DecimalFormat format3= new DecimalFormat("#.####");
	GeneralSubstitutionModel gtr;
	TableView<RateRow> table;
    ObservableList<RateRow> rows;
	Slider a0Slider;
	Slider a1Slider;
	Slider b1Slider;
	Slider b2Slider;
	Label diffLabel;

	
	int current = 1;
	double [] target = new double[100];

	class RateRow {
		double [] r;
		int rowNr;
		
		public Double getR0() {
			return r[0];
		}
		public void setR0(Double r0) {
			this.r[0] = r0;
			normalise();
		}
		public Double getR1() {
			return r[1];
		}
		public void setR1(Double r1) {
			this.r[1] = r1;
			normalise();
		}
		public Double getR2() {
			return r[2];
		}
		public void setR2(Double r2) {
			this.r[2] = r2;
			normalise();
		}
		public Double getR3() {
			return r[3];
		}
		public void setR3(Double r3) {
			this.r[3] = r3;
			normalise();
		}
		public Double getR4() {
			return r[4];
		}
		public void setR4(Double r4) {
			this.r[4] = r4;
			normalise();
		}
		public Double getR5() {
			return r[5];
		}
		public void setR5(Double r5) {
			this.r[5] = r5;
			normalise();
		}
		public Double getR6() {
			return r[6];
		}
		public void setR6(Double r6) {
			this.r[6] = r6;
			normalise();
		}

		
		RateRow(int rowNr) {
			this.rowNr = rowNr;
			r = new double[7];
			for (int i = 0; i < N; i++) {
				r[i] = Randomizer.nextDouble();
			}
		}
		public void setR(int i, Double newValue) {
			r[i] = newValue;
			normalise();
		}
		public Double getR(int i) {
			return r[i];
		}
	}
	
	private void normalise() {
		for (int i = 0; i < N; i++) {
			for (int j = 0; j < i; j++) {
				rows.get(i).r[j] = rows.get(j).r[i];
			}
		}
		
		for (int j = 0; j < N; j++) {
			double sum = 0;
			RateRow row = rows.get(j);
			for (int i = 0; i < N; i++) {
				if (i != j) {
					sum += row.r[i];
				}
			}
			row.r[j] = -sum;
		}
		
		table.refresh();
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		setUpGTR();
		
		SplitPane pane = new SplitPane();
		VBox ctrlPane = new VBox();
		ctrlPane.setPrefWidth(200);
		pane.getItems().add(ctrlPane);
		
		// create control panel
		a0Slider = new Slider(0, 1, 0.25);
		a1Slider = new Slider(-1, 1, 0.0);
		b1Slider = new Slider(0, 5, 0.25);
		b2Slider = new Slider(0, 5, 0.75);

		a0label = new Label("a0=0.25");
		a1label = new Label("a1=0.0");
		a2label = new Label("a2=-0.25");
		b1label = new Label("b1=0.5");
		b2label = new Label("b2=0.5");
		
		ctrlPane.getChildren().addAll(a0label, a0Slider,
				a1label, a1Slider, 
				a2label,
				b1label,
				b1Slider, b2label,b2Slider);
		a0Slider.valueProperty().addListener((ov, oldValue, new_val) -> {
        	a0 = new_val.doubleValue();
        	repaint();
		});
		a1Slider.valueProperty().addListener((ov, oldValue, new_val) -> {
        	a1 = new_val.doubleValue();
        	repaint();
		});
		b1Slider.valueProperty().addListener((ov, oldValue, new_val) -> {
        	b1 = new_val.doubleValue();
        	repaint();
		});
		b2Slider.valueProperty().addListener((ov, oldValue, new_val) -> {
        	b2 = new_val.doubleValue();
        	repaint();
		});

		
		HBox box = new HBox();
		Button optimiseNext = new Button("Next");
		optimiseNext.setOnAction(e->optimiseNext());
		diffLabel = new Label("0.0");
		box.getChildren().addAll(optimiseNext, diffLabel);
		
		Button printButton = new Button("Print");
		printButton.setOnAction(e->{
			WritableImage image = primaryStage.getScene().snapshot(null);
			String fName = "/tmp/ExpFunctionViewer.png";
			File file = new File(fName);
            try {
                ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
            } catch (IOException e2) {
                e2.printStackTrace();
            }
            Alert.showMessageDialog(primaryStage.getScene().getRoot(), "Image saved as file " + fName);
//			ObservableSet<Printer> p = Printer.getAllPrinters();
//			Printer p2= Printer.getDefaultPrinter();
//			System.out.println(p.size());
//			if (p.size()> 0) {
//			 PrinterJob job = PrinterJob.createPrinterJob(p2); 
//					 // PrinterJob.createPrinterJob(p.toArray(new Printer[] {})[0]);
//			 if(job != null){
//			   job.showPrintDialog(primaryStage); // Window must be your main Stage
//			   job.printPage(chart);
//			   job.endJob();
//			 }
//			}
		});
		box.getChildren().add(printButton);

		ctrlPane.getChildren().add(box);

		table = new TableView<>();
		for (int i = 0; i < N; i++) {
	        TableColumn<RateRow, String> col = new TableColumn<>("R" + i);
	        col.setPrefWidth(65);
	        col.setEditable(true);
	        col.setOnEditCommit(event-> {
	        	try {
	        	String str = event.getNewValue();
							Double newValue = Double.parseDouble(str);
							RateRow rateRow = event.getRowValue();
							String colName = event.getTableColumn().getText();
							int colNr = Integer.parseInt(colName.substring(1));
							rateRow.setR(colNr, newValue);
							repaint();
							System.err.println(colNr + " " + newValue);
	        	} catch (NumberFormatException e) {
	        		// ignore
	        	}
					});
	        col.setCellFactory(TextFieldTableCell.forTableColumn());

	        col.setCellValueFactory(event -> {
	        	SimpleObjectProperty<String> property = new SimpleObjectProperty<>();
				String colName = event.getTableColumn().getText();
				int colNr = Integer.parseInt(colName.substring(1));
	            property.setValue(event.getValue().getR(colNr) + "");
	            return property;
	        });
	        table.getColumns().add(col);
		}
		
		rows = FXCollections.observableArrayList();
		for (int i = 0; i < N; i++) {
			RateRow rateRow = new RateRow(i);
			rows.add(rateRow);
		}
		normalise();
		
		table.setItems(rows);
        table.setEditable(true);
        ctrlPane.getChildren().add(table);
        table.setPrefHeight(24 * N+40);
        
        setPadding(ctrlPane);
		
		// create chart
		NumberAxis xAxis = new NumberAxis(0, 10, 1);
        xAxis.setLabel("Time t");                
        NumberAxis yAxis = new NumberAxis(0, 2.0/N, 0.4/N);        
        yAxis.setLabel("P(t)");
        chart = new LineChart<Number,Number>(xAxis,yAxis);
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setCreateSymbols(false);
        calcSeries();
        pane.getItems().add(chart);
		pane.setDividerPositions(0.33,0.67);
       		
		
        pane.setPrefWidth(1024);
        pane.setPrefHeight(600);
		Scene scene = new Scene(pane);
		primaryStage.setScene(scene);
		primaryStage.show();

		primaryStage.setOnCloseRequest((event) -> {
		    System.exit(0);
		});
	}       

	private void setPadding(Node n) {
		if (n instanceof Parent) {
            for (Node c : ((Parent) n).getChildrenUnmodifiable()) {
            	if (c instanceof Control) {
            		((Control)c).setPadding(new Insets(4));
            	}
            	setPadding(c);
            }
        }	
	}

	private Object optimiseNext() {
		current = current + 1;
		if (current == N) {
			current = 1;
		}

		// set up target function
        double [] matrix= new double[N*N];
        gtr.initAndValidate();
        for (int i = 0; i < 100; i++) {
        	double x = (i+0.0)/(10);
        	gtr.getTransitionProbabilities(null, x, 0, 1.0, matrix);
            target[i] = matrix[current];
        }

        
        // optimise b1, b2, a1 (a0, a2 and b0 are calculated from prior info)
        
        a0 = 1.0/N;
        boolean done = false;
        int k = 0;
    	BrentOptimizer optimizer = new BrentOptimizer(1e-8, 1e-8);
        do {
        	int i = k < 2500?k%3:Randomizer.nextInt(3);

        	switch (i) {
        	case 0:
    		UnivariatePointValuePair p = optimizer.optimize(new MaxEval(200), 
    				new UnivariateObjectiveFunction(x -> {
    					b1 = x;
    					return differenceFrom(target);
    				}),
    				GoalType.MINIMIZE, new SearchInterval(0, 5));
    		b1 = p.getPoint();
    		break;
        	case 1:
    		p = optimizer.optimize(new MaxEval(200), 
    				new UnivariateObjectiveFunction(x -> {
    					b2 = x;
    					return differenceFrom(target);
    				}),
    				GoalType.MINIMIZE, new SearchInterval(0, 5));
    		b2 = p.getPoint();
    		break;
        	case 2:
        	p = optimizer.optimize(new MaxEval(200), 
    				new UnivariateObjectiveFunction(x -> {
    					a1 = x;
    					return differenceFrom(target);
    				}),
    				GoalType.MINIMIZE, new SearchInterval(-1, 1));
    		a1 = p.getPoint();
    		break;
        	}

        	k++;
        	done = k > 1500;
        } while (!done);
        
        
    	a0Slider.setValue(a0);
    	a1Slider.setValue(a1);
    	b1Slider.setValue(b1);
    	b2Slider.setValue(b2);

        
    	for (int i = 0; i < N; i++) {
    		if (i+1 == current) {
    	    	chart.getData().get(i+1).getNode().setStyle("-fx-stroke-width:3;");
    		} else {
    	    	chart.getData().get(i+1).getNode().setStyle("-fx-stroke-width:1;");
    		}
    	}
    	
        
		return null;
	}

	private double differenceFrom(double[] target) {
    	a2= -a0 - a1;
		double diff = 0;
        for (int i = 0; i < 100; i++) {
        	double x = (i+0.0)/(10);
        	double y = a0 + a1 * Math.exp(-b1 * x) + a2 * Math.exp(-b2 * x);
        	diff += Math.abs(y-target[i]);
        }
		return diff;
	}

	
	private void setUpGTR() {
		gtr = new GeneralSubstitutionModel();
		Double [] freqs = new Double[N];
		for (int i = 0; i < N; i++) {
			freqs[i] = Double.valueOf(1.0/N);
		}
		RealParameter freqParam = new RealParameter(freqs);
		Frequencies fs = new Frequencies();
		fs.initByName("frequencies", freqParam);
		RealParameter rates = new RealParameter();
		rates.initByName("dimension", N*(N-1),"value", "1.0");
		gtr.initByName("frequencies", fs, "rates", rates);
	}

	private void repaint() {
		//chart.getData().clear();
		calcSeries();
		a0label.setText("a0=" + format.format(a0));
		a1label.setText("a1=" + format.format(a1));
		a2label.setText("a2=" + format.format(a2));
		b1label.setText("b1=" + format.format(b1));
		b2label.setText("b2=" + format.format(b2));

	}

	private void calcSeries() {
		if (chart.getData().size() == 0) {
			LineChart.Series<Number,Number> series1 = new LineChart.Series<>();
	        for (int i = 0; i < 100; i++) {
	        	series1.getData().add(new XYChart.Data(0,0));
	        }
	        chart.getData().addAll(series1);

	        for (int k = 0; k < N; k++) {
				LineChart.Series<Number,Number> series = new LineChart.Series<>();
		        for (int i = 0; i < 100; i++) {
		        	series.getData().add(new XYChart.Data(0,0));
		        }
		        chart.getData().addAll(series);
	        }
		}
		
        for (int i = 0; i < 100; i++) {
        	double x = (i+0.0)/(10);
        	a2= -a0 - a1;
        	double y = a0 + a1 * Math.exp(-b1 * x) + a2 * Math.exp(-b2 * x);
        	Data<Number, Number>p = chart.getData().get(0).getData().get(i);
        	p.setXValue(x);
        	p.setYValue(y);
            //series1.getData().add(new XYChart.Data(x, y));
        }
        

        RealParameter rates = (RealParameter) gtr.ratesInput.get();
        int k = 0;
        for (int i = 0; i < N; i++) {
        	for (int j = 0; j < N; j++) {
        		if (i != j) {
        			double r = rows.get(i).getR(j);
        			rates.setValue(k++, r);
        		}
        	}
        }
        double [] matrix= new double[N*N];
        gtr.initAndValidate();
        Log.warning(Arrays.toString(gtr.getEigenDecomposition(null).getEigenValues()));
        for (int i = 0; i < N; i++) {
        	RateRow row = rows.get(i);
        	for (int j = 0; j < N; j++) {
        		System.out.print(format.format(row.r[j]) + " ");
        	}
        	System.out.println();
        }
        System.out.println("diff = " + differenceFrom(target));
		diffLabel.setText("diff = " + format3.format(differenceFrom(target)));
        
        for (int i = 0; i < 100; i++) {
        	double x = (i+0.0)/(10);
        	gtr.getTransitionProbabilities(null, x, 0, 1.0, matrix);
            for (k = 1; k < N; k++) {
            	double y = matrix[k];
            	Data<Number, Number>p = chart.getData().get(k).getData().get(i);
            	p.setXValue(x);
            	p.setYValue(y);
            }
            //series1.getData().add(new XYChart.Data(x, y));
        }

	}

	public static void main(String[] args) {
	    launch();
	}
	
}
