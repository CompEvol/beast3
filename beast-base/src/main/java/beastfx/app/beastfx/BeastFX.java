package beastfx.app.beastfx;


import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

import org.json.JSONObject;

import beagle.BeagleFlag;
import beagle.BeagleInfo;
import beast.base.core.Input;
import beast.base.core.Log;
import beast.base.inference.Logger;
import beast.base.inference.Logger.LogFileMode;
import beast.base.parser.JSONParser;
import beast.base.parser.XMLParser;
import beast.base.parser.XMLParserException;
import beast.base.util.Randomizer;
import beast.pkgmgmt.PackageManager;
import beastfx.app.util.Application;
import beastfx.app.util.HTMLPrintStream;
import beastfx.app.util.HTTPPostServer;
import beastfx.app.util.HTTPRequestHandler;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/** 
 * main program for running MCMC analysis as well as other analysis 
 */

public class BeastFX extends Application implements HTTPRequestHandler {
	
	public Input<String> debugInput = new  Input<String> ("debug", "set level of debug messages shown. One of " + Arrays.toString(Log.Level.values()), "info");  
	public Input<Boolean> optionsInput = new  Input<Boolean> ("options", "Display an options dialog", false);  
	public Input<Boolean> workingInput = new  Input<Boolean> ("working", "Change working directory to input file's directory", true);  
	public Input<String> prefixInput = new  Input<String> ("prefix", "Specify a prefix for all output log filenames"); 
	public Input<Long> seedInput = new  Input<Long> ("seed", "Specify a random number generator seed", Randomizer.nextLong());  
	public Input<File> statefileInput = new  Input<File> ("statefile", "Specify the filename for storing/restoring the state. " +
			"By default the name of the inputfile + '.state' is used."); 
	
	public Input<Logger.LogFileMode> logmodeInput = new  Input<Logger.LogFileMode> ("mode", "Append/overwrite/exit on existance of log files", Logger.LogFileMode.only_new_or_exit, Logger.LogFileMode.values());  
	
	public Input<Integer> threadsInput = new  Input<Integer> ("threads", "The number of computational threads to use (default 1)", 1);  

	enum Processor {java, CPU, SSE, GPU}
	public Input<Processor> processorInput = new  Input<Processor> ("processor", "determines the algorithm used for treelikelihood calculations", Processor.java, Processor.values());  

	public Input<String> beagle_orderInput = new  Input<String> ("beagle_order", "set order of resource use (effective when processor!=java)"); 
	public Input<Integer> beagle_instancesInput = new  Input<Integer> ("beagle_instances", "divide site patterns amongst instances (effective when processor!=java)");

	public Input<String> precisionInput = new  Input<String> ("precision", "use single or double precision if available (effective when processor!=java)","double", new String[]{"single", "double"});  
			     
	enum Scaling {default_, none, dynamic, always};
	public Input<Scaling> beagle_scalingInput = new  Input<Scaling> ("beagleScaling", "set method of scaling for treelikelihood calculations", Scaling.default_, Scaling.values());			     
	public Input<File> inputfileInput = new  Input<File> ("inputfile", "Specify the filename for storing/restoring the state");
	public Input<Boolean> helpInput = new  Input<Boolean> ("help", "Print this information and stop", false);  

	final static String BEASTFX_IS_DONE = "BEASTFX_is_done";

	/** 
	 * used for displaying GUI 
	 */
    private static WebEngine webEngine;
	/** 
	 * used for stream info to the GUI 
	 */
	OutputStream stream = null;
    /**
     * number of threads used to run the likelihood beast.core *
     */
    static public int m_nThreads = 1;
    /**
     * thread pool *
     */
    public static ExecutorService g_exec = Executors.newFixedThreadPool(m_nThreads);
    /**
     * random number seed used to initialise Randomizer *
     */
    long m_nSeed = 127;

    /**
     * MCMC object to execute *
     */
    beast.base.inference.Runnable m_runnable;

    public BeastFX() {
    	defaultInput = inputfileInput;
    	_main = this;
    }

	@Override
	public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("Hello BEAST-FX!");
        
        WebView browser = new WebView();
        webEngine = browser.getEngine();

        webEngine.load("http://localhost:" + port + "/beastfx.html");
        StackPane root = new StackPane();
        root.getChildren().add(browser);
        primaryStage.setScene(new Scene(root, 1024, 768));
        primaryStage.show();
        
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
    	    @Override
    	    public void handle(WindowEvent event) {
    	        Platform.exit();
    	        System.exit(0);
    	    }
    	});
        
        URL url = (URL) ClassLoader.getSystemResource("beastfx/app/icons/beastfx.png");
        Image image = new Image(url.toString());
        primaryStage.getIcons().add(image);
	}

	@Override
	public String handleRequest(String url, StringBuffer data) throws IOException {
        OutputStream stream;
        if (this.stream != null) {
        	stream = this.stream; 
        } else {
	        stream = new AppOutputStream();
	        
            Log.trace = new HTMLPrintStream(stream, Log.Level.trace);
            Log.debug = new HTMLPrintStream(stream, Log.Level.debug);
            Log.info = new HTMLPrintStream(stream, Log.Level.info);
            Log.warning = new HTMLPrintStream(stream, Log.Level.warning);
            Log.err = new HTMLPrintStream(stream, Log.Level.error);
            System.setOut(Log.info);
            Log.setLevel(Log.Level.info);
	        
	        //System.setOut(new HTMLPrintStream(stream, Log.Level.info));
	        System.setErr(Log.warning);
	        this.stream = stream;
        }
		HTMLPrintStream.currentLevel = null;
		
		while (url.startsWith("/")) {
			url = url.substring(1);
		}
		
		if (url.startsWith("quitBeastFX")) {
			System.err.println("Quiting now");
			System.exit(0);
		} else if (url.startsWith("getInputFile")) {
			return getFileNameByDialog();
		} else if (url.startsWith("getRandomSeed")) {
			return "" + Math.abs(Randomizer.nextLong());
		} else if (url.startsWith("showBeagleInfo")) {
			BeagleInfo.printResourceList();
		} else if (url.startsWith("poll")) {
			String output = stream.toString();
			stream.flush();
			HTMLPrintStream.currentLevel = null;
			return output;
		} else if (url.startsWith("runBeastFX")) {
			
			try {
				
				int i = url.indexOf("?");
				url = url.substring(i + 1);
				url = url.replaceAll("%22", "\"");
				url = url.replaceAll("%20", " ");
				JSONObject json = new JSONObject(url);
		        parseArgs(json);
		        initialise();
				
		        new Thread() {
					public void run() {
						try {
							m_runnable.run();
						} catch (Exception e) {
							Log.err.println(e.getMessage());
							e.printStackTrace(Log.debug);
						}
						System.out.println(BEASTFX_IS_DONE);
					};
				}.start();
				return "MCMC started";
	        } catch (XMLParserException e) {
	            Log.err.println(e.getMessage());
				Log.info.println(BEASTFX_IS_DONE);
			} catch (Exception e) {
				Log.err.println("Could not start MCMC: " + e.getMessage());
				Log.info.println(BEASTFX_IS_DONE);
				return "";
			}
			
		}
		
		String output = stream.toString();
		stream.flush();
		HTMLPrintStream.currentLevel = null;
		return output;
	}

    /**
     * open file dialog for prompting the user to specify an xml script file to process *
     */
	CountDownLatch countDownLatch;
	String result;
    String getFileNameByDialog() {
    	countDownLatch = new CountDownLatch(1);
    	SwingUtilities.invokeLater(new java.lang.Runnable() {
			
			@Override
			public void run() {
		        JFileChooser fc = new JFileChooser(System.getProperty("user.dir"));
		        fc.addChoosableFileFilter(new FileFilter() {
		            public boolean accept(File f) {
		                if (f.isDirectory()) {
		                    return true;
		                }
		                String name = f.getName().toLowerCase();
		                if (name.endsWith(".xml")) {
		                    return true;
		                }
		                return false;
		            }

		            // The description of this filter
		            public String getDescription() {
		                return "xml files";
		            }
		        });

		        fc.setDialogTitle("Load xml file");
		        int rval = fc.showOpenDialog(null);

		        if (rval == JFileChooser.APPROVE_OPTION) {
		        	result = fc.getSelectedFile().toString();
		        	countDownLatch.countDown();
		        	return;
		        }
		        result = null;
	        	countDownLatch.countDown();
			}
		});

    	try {countDownLatch.await();} catch (Throwable t) {}
    	return result;
    } // getFileNameByDialog


    /**
     * process inputs
     *
     * @throws Exception *
     */
    public boolean initialise() throws Exception {
    	if (helpInput.get() != null && helpInput.get()) {
    		System.out.println(getUsage());
    		System.exit(0);
    	}
    	
        boolean resume = false;

        
        if (optionsInput.get()) {
            OutputStream stream = new AppOutputStream();
	 		Log.trace = new HTMLPrintStream(stream, Log.Level.trace);
	        Log.debug = new HTMLPrintStream(stream, Log.Level.debug);
	        Log.info = new HTMLPrintStream(stream, Log.Level.info);
	        Log.warning = new HTMLPrintStream(stream, Log.Level.warning);
	        Log.err = new HTMLPrintStream(stream, Log.Level.error);
	        System.setOut(Log.info);
	        System.setErr(Log.warning);
	        this.stream = stream;
        }
        
        switch (debugInput.get()) {
        case "debug":
            Log.setLevel(Log.Level.debug);
            break;
        case "trace":
            Log.setLevel(Log.Level.trace);
            break;
        case "info":
            Log.setLevel(Log.Level.info);
            break;
        case "warning":
            Log.setLevel(Log.Level.warning);
            break;
        case "err":
            Log.setLevel(Log.Level.error);
            break;
        default:
        	throw new Exception("Wrong argument to -" + debugInput.getName() + ". Instead of " + debugInput.get() + 
        			" use one of " + Log.Level.values());        	
        }
        
        File beastFile = inputfileInput.get();

        if (beastFile != null && beastFile.getParent() != null && workingInput.get()) {
            System.setProperty("file.name.prefix", beastFile.getParentFile().getAbsolutePath());
        }
        
        m_nSeed = seedInput.get();

        if (prefixInput.get() != null && prefixInput.get().trim().length() > 0) {
            System.setProperty("file.name.prefix", prefixInput.get().trim());
        }
        
        File stateFile = statefileInput.get();
        if (stateFile != null) {
        	String stateFileName = stateFile.getPath();
	        if (stateFileName!= null && stateFileName.trim().length() > 0) {
	            System.setProperty("state.file.name", stateFileName.trim());
	            System.out.println("Writing state to file " + stateFileName);
	        }
        }
        
        Logger.FILE_MODE = logmodeInput.get();
        if (logmodeInput.get() == LogFileMode.resume) {
            System.setProperty("beast.resume", "true");
            System.setProperty("beast.debug", "false");
            resume = true;
        }

        m_nThreads = threadsInput.get();
        g_exec = Executors.newFixedThreadPool(m_nThreads);
        
        // BEAGLE options
        long beagleFlags = 0;
        switch (processorInput.get()) {
        case java:
            System.setProperty("java.only", "true");
        	break;
        case CPU:
            beagleFlags |= BeagleFlag.PROCESSOR_CPU.getMask();
        	break;
        case SSE:
            beagleFlags |= BeagleFlag.PROCESSOR_CPU.getMask();
            beagleFlags |= BeagleFlag.VECTOR_SSE.getMask();
        	break;
        case GPU:
            beagleFlags |= BeagleFlag.PROCESSOR_GPU.getMask();
        	break;
        }
	    if (processorInput.get() != Processor.java) { 
			
			String beaglePrecision = precisionInput.get();
			switch (beaglePrecision) {
			case "double":
	            beagleFlags |= BeagleFlag.PRECISION_DOUBLE.getMask();
				break;
			case "single":
	            beagleFlags |= BeagleFlag.PRECISION_SINGLE.getMask();
				break;
			}
			
	        System.setProperty("beagle.scaling", "" + beagle_scalingInput.get());
	
	        System.setProperty("beagle.preferred.flags", Long.toString(beagleFlags));		
	        System.setProperty("java.only", "false");
	        
	        if (beagle_orderInput.get() != null) {
	            System.setProperty("beagle.resource.order", beagle_orderInput.get());
	        }

	        if (beagle_instancesInput.get() != null) {
	            System.setProperty("beagle.instance.count", "" + beagle_instancesInput.get());
	        }
	    }

        if (beastFile == null) {
        	return false;
        }

        System.err.println("File: " + beastFile.getName() + " seed: " + m_nSeed + " threads: " + m_nThreads);
        if (resume) {
            System.out.println("Resuming from file");
        }

        PackageManager.loadExternalJars();
        // parse xml
        Randomizer.setSeed(m_nSeed);
        if (beastFile.getName().toLowerCase().endsWith("xml")) {
        	m_runnable = new XMLParser().parseFile(beastFile);
        } else {
        	m_runnable = new JSONParser().parseFile(beastFile);
        }
        m_runnable.setStateFile(beastFile.getName() + ".state", resume);
    	return true;
    } // init

    public void run() throws Exception {
    	g_exec = Executors.newFixedThreadPool(m_nThreads);
        m_runnable.run();
        g_exec.shutdown();
        g_exec.shutdownNow();
    } // run	
	
    
    static int port = 5000;
	/**
	 * @param args
	 */
    public static BeastFX _main;
    
	public static void main(final String[] args) throws Exception {
		try {
			BeastFX main = new BeastFX();
			main.parseArgs(args, false);
			if (main.optionsInput.get()) {
				port = HTTPPostServer.startServer(main);
				launch(args);
				
			} else {
				if (main.initialise()) {
					main.run();
				} else {
			        if (!java.awt.GraphicsEnvironment.isHeadless()) {
						port = HTTPPostServer.startServer(main);
			        	launch(args);
			        } else {
			        	System.out.println("Cannot start without input file");
			        	System.out.println(main.getUsage());
			        }
				}
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

}
