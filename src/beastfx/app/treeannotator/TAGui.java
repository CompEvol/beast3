package beastfx.app.treeannotator;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Optional;

import beast.base.core.Log;
import beast.pkgmgmt.BEASTVersion;
import beastfx.app.beauti.ThemeProvider;
import beastfx.app.treeannotator.services.UserTargetTreeTopologyService;
import beastfx.app.util.Console;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ButtonBar.ButtonData;

public class TAGui extends Console {
	
	public TAGui() {
	}
	
	@Override
	protected void createDialog() {
	Controller controller;
	PrintStream err = System.err;
	System.setErr(new PrintStream(new OutputStream() {
	    public void write(int b) {
	    }
	}));
	System.setErr(err);

      java.net.URL url = TreeAnnotator.class.getClassLoader().getResource("../tools/images/utility.png");

      final String versionString = TreeAnnotator.version.getVersionString();
      String nameString = "TreeAnnotator " + versionString;
      String aboutString = "<html><center><p>" + versionString + ", " + TreeAnnotator.version.getDateString() + "</p>" +
              "<p>by<br>" +
              "Andrew Rambaut and Alexei J. Drummond</p>" +
              "<p>Institute of Evolutionary Biology, University of Edinburgh<br>" +
              "<a href=\"mailto:a.rambaut@ed.ac.uk\">a.rambaut@ed.ac.uk</a></p>" +
              "<p>Department of Computer Science, University of Auckland<br>" +
              "<a href=\"mailto:alexei@cs.auckland.ac.nz\">alexei@cs.auckland.ac.nz</a></p>" +
              "<p>Part of the BEAST package:<br>" +
              "<a href=\"http://beast.bio.ed.ac.uk/\">http://beast.bio.ed.ac.uk/</a></p>" +
              "</center></html>";

      Log.info = System.out;
      Log.err = System.err;

      // The ConsoleApplication will have overridden System.out so set progressStream
      // to capture the output to the window:
      // new beastfx.app.util.Console();            
      TreeAnnotator.progressStream = System.out;

      TreeAnnotator.printTitle();
      
        	try {
				Dialog<String> dialog = new Dialog<>();
			    dialog.setTitle("TreeAnnotator " + BEASTVersion.INSTANCE.getVersion());
			    FXMLLoader fl = new FXMLLoader();
			    fl.setClassLoader(TreeAnnotator.class.getClassLoader());
			    fl.setLocation(TreeAnnotator0.class.getResource("TreeAnnotator.fxml"));
			    DialogPane root = fl.load();
			    dialog.setDialogPane(root);
		
			    ButtonType run = new ButtonType("Run", ButtonData.OK_DONE);
			    dialog.getDialogPane().getButtonTypes().add(run);
			    ButtonType cancel = new ButtonType("Quit", ButtonData.CANCEL_CLOSE);
			    dialog.getDialogPane().getButtonTypes().add(cancel);
			    ThemeProvider.loadStyleSheet(dialog.getDialogPane().getScene());
			    
			    
				//Showing the dialog on clicking the button
		        Optional<String> result = dialog.showAndWait();
		        dialog.close();
		        
		        Object o = result.get();
		        String str = o.toString();
		        
			    if (str.equals(run.toString())) {
				    controller = fl.getController();
		        	controller.run(null);
		        } else {
		        	Log.warning("Quiting TreeAnnotator");
		        	System.exit(0);
		    		return;
		        }

				new Thread() {
		public void run() {
	        int burninPercentage = controller.getBurninPercentage();
	        if (burninPercentage < 0) {
	        	Log.warning.println("burnin percentage is " + burninPercentage + " but should be non-negative. Setting it to zero");
	        	burninPercentage = 0;
	        }
	        if (burninPercentage >= 100) {
	        	Log.err.println("burnin percentage is " + burninPercentage + " but should be less than 100.");
	        	return;
	        }
	        double posteriorLimit = controller.getPosteriorLimit();
	        double hpd2D = 0.80;
	        String targetOption = controller.getTargetOption();
	        String heightsOption = controller.getHeightsOption();

	        String targetTreeFileName = controller.getTargetFileName();
	        if (targetOption.equals(UserTargetTreeTopologyService.SERVICE_NAME) && targetTreeFileName == null) {
	            Log.err.println("No target file specified");
	            return;
	        }

	        String inputFileName = controller.getInputFileName();
	        if (inputFileName == null) {
	            Log.err.println("No input file specified");
	            return;
	        }

	        String outputFileName = controller.getOutputFileName();
	        if (outputFileName == null) {
	            Log.err.println("No output file specified");
	            return;
	        }
	    	boolean lowMem = controller.useLowMem();

	        try {
	            TreeAnnotator ta = new TreeAnnotator();
	            ta.topologyInput.setValue(targetOption, ta);
	            ta.heightInput.setValue(heightsOption, ta);
                ta.targetInput.setValue(targetTreeFileName, ta);

                ta.run(burninPercentage,
	            		lowMem,
	                    posteriorLimit,
	                    hpd2D,
	                    inputFileName,
	                    outputFileName
                );

	        } catch (Exception ex) {
	            Log.err.println("Exception: " + ex.getMessage());
	        } catch (OutOfMemoryError e) {
	        	Log.warning("TreeAnnotator ran out of memory: " + e.getMessage());
	        	Log.warning("You can subsample the tree set with LogCombiner or provide more memory to fix this.");
	        	Log.warning("See https://www.beast2.org/increasing-memory-usage/ on how to provide more memory");
	        }

	        TreeAnnotator.progressStream.println("Finished - Quit program to exit.");
		}
	}.start();


      } catch (IOException e) {
      	e.printStackTrace();
      }
  }
}