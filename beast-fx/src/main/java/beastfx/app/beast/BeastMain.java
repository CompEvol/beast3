package beastfx.app.beast;
	

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import beagle.BeagleFactory;
import beagle.BeagleFlag;
import beast.base.core.BEASTVersion2;
import beast.base.core.Log;
import beast.base.parser.XMLParserException;
import beast.base.util.Randomizer;
import beast.pkgmgmt.Arguments;
import beast.pkgmgmt.BEASTVersion;
import beast.pkgmgmt.PackageManager;
import beast.pkgmgmt.Version;
import beastfx.app.util.Utils;
import beastfx.app.beauti.ThemeProvider;
import beastfx.app.util.Console;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.stage.Window;

import static beast.pkgmgmt.BEASTClassLoader.addServices;

public class BeastMain extends Console {
    private final static Version version = new BEASTVersion2();

	@Override
	protected void createDialog() {

        Dialog<String> dialog = new Dialog<>();
	    dialog.setTitle("BEAST " + BEASTVersion2.INSTANCE.getVersion());
	    FXMLLoader fl = new FXMLLoader();
	    fl.setClassLoader(getClass().getClassLoader());
	    fl.setLocation(BeastMain.class.getResource("BeastMain.fxml"));

	    DialogPane root = null;
		try {
			root = fl.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
		dialog.getDialogPane();
	    dialog.setDialogPane(root);
	    ThemeProvider.loadStyleSheet(root.getScene());        

	    Object o = fl.getController();
	    if (o != null) {
	    	((Controller)o).dialog = dialog;
	    	((Controller)o).textView = textView;
	    }

		//Showing the dialog on clicking the button
	    dialog.show();

	    Window window = dialog.getDialogPane().getScene().getWindow();
		window.setOnCloseRequest(event -> window.hide());
 	}
	
    public static void centreLine(final String line, final int pageWidth) {
        final int n = pageWidth - line.length();
        final int n1 = n / 2;
        for (int i = 0; i < n1; i++) {
            Log.info.print(" ");
        }
        Log.info.println(line);
    }

    public static void printTitle() {

        int pageWidth = 72;

        Log.info.println();
        centreLine("BEAST " + version.getVersionString() + ", " + version.getDateString(), pageWidth);
        centreLine("Bayesian Evolutionary Analysis Sampling Trees", pageWidth);
        for (final String creditLine : version.getCredits()) {
            centreLine(creditLine, pageWidth);
        }
        Log.info.println();
    }

    public static void printUsage(final Arguments arguments) {

        arguments.printUsage("beast", "[<input-file-name>]");
        Log.info.println();
        Log.info.println("  Example: beast test.xml");
        Log.info.println("  Example: beast -window test.xml");
        Log.info.println("  Example: beast -help");
        Log.info.println();
    }

	private static void printVersion() {
    	Log.info("BEAST " + (new BEASTVersion()).getVersionString());
        Log.info("---");
        for (String jarDirName : PackageManager.getBeastDirectories()) {
            File versionFile = new File(jarDirName + "/version.xml");
            if (versionFile.exists()) {
            	try {
	                // print name and version of package
	                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	                Document doc = factory.newDocumentBuilder().parse(versionFile);
	                Element packageElement = doc.getDocumentElement();
	                Log.info.print(packageElement.getAttribute("name") + " v" + packageElement.getAttribute("version"));
	                Log.debug.print(" " + jarDirName);
	                Log.info.print("\n");
            	} catch (IOException| SAXException| ParserConfigurationException e) {
            		Log.err(e.getMessage());
            	}
            }
        }
        Log.info("---");
    	Log.info("Java version " + System.getProperty("java.version"));
	}
	
	public static void main(String[] args) {
        final List<String> MCMCargs = new ArrayList<>();

        final Arguments arguments = new Arguments(
                new Arguments.Option[]{
                        new Arguments.Option("window", "Provide a console window"),
                        new Arguments.Option("options", "Display an options dialog"),
                        new Arguments.Option("working", "Change working directory to input file's directory"),
                        new Arguments.LongOption("seed", "Specify a random number generator seed"),
                        new Arguments.StringOption("prefix", "PREFIX", "Specify a prefix for all output log filenames"),
                        new Arguments.StringOption("statefile", "STATEFILE", "Specify the filename for storing/restoring the state"),
                        new Arguments.Option("overwrite", "Allow overwriting of log files"),
                        new Arguments.Option("resume", "Allow appending of log files"),
                        new Arguments.Option("validate", "Parse the XML, but do not run -- useful for debugging XML"),
                        // RRB: not sure what effect this option has
                        new Arguments.IntegerOption("errors", "Specify maximum number of numerical errors before stopping"),
                        new Arguments.IntegerOption("threads", "The number of computational threads to use (default 1), -1 for number of cores"),
                        new Arguments.Option("java", "Use Java only, no native implementations"),
                        new Arguments.Option("noerr", "Suppress all output to standard error"),
                        new Arguments.StringOption("loglevel", "LEVEL", "error,warning,info,debug,trace"),
                        new Arguments.IntegerOption("instances", "divide site patterns amongst number of threads (use with -threads option)"),
                        new Arguments.Option("beagle", "Use beagle library if available"),
                        new Arguments.Option("beagle_info", "BEAGLE: show information on available resources"),
                        new Arguments.StringOption("beagle_order", "order", "BEAGLE: set order of resource use"),
                        new Arguments.Option("beagle_CPU", "BEAGLE: use CPU instance"),
                        new Arguments.Option("beagle_GPU", "BEAGLE: use GPU instance if available"),
                        new Arguments.Option("beagle_SSE", "BEAGLE: use SSE extensions if available"),
                        new Arguments.Option("beagle_single", "BEAGLE: use single precision if available"),
                        new Arguments.Option("beagle_double", "BEAGLE: use double precision if available"),
                        new Arguments.StringOption("beagle_scaling", new String[]{"default", "none", "dynamic", "always"},
                                false, "BEAGLE: specify scaling scheme to use"),
                        new Arguments.Option("help", "Print this information and stop"),
                        new Arguments.Option("version", "Print version and stop"),
                        new Arguments.Option("strictversions", "Use only package versions as specified in the 'required' attribute"),
                        new Arguments.StringOption("D", "DEFINITIONS", "attribute-value pairs to be replaced in the XML, e.g., -D \"arg1=10,arg2=20\"").allowMultipleUse(),
                        new Arguments.StringOption("DF", "DEFINITIONFILE", "as -D, but attribute-value pairs defined in file in JSON format").allowMultipleUse(),
                        new Arguments.StringOption("DFout", "DEFINITIONRESULTFILE", "BEAST XML file written when -DF option is used"),
                        new Arguments.Option("sampleFromPrior", "samples from prior for MCMC analysis (by adding sampleFromPrior=\"true\" in the first run element)"),
                        new Arguments.StringOption("version_file", "VERSIONFILE" ,"Provide a version file containing a list of services to explicitly allow. (Useful for package development.)").allowMultipleUse(),
                        new Arguments.StringOption("packagedir", "PACKAGEDIR" ,"Set user package directory instead of using the default"),
                });

        try {
            arguments.parseArguments(args);
        } catch (Arguments.ArgumentException ae) {
        	Log.info.println();
        	Log.info.println(ae.getMessage());
        	Log.info.println();
            printUsage(arguments);
            System.exit(1);
        }
        
        if (arguments.hasOption("packagedir")) {
            String dir = arguments.getStringOption("packagedir");
            System.setProperty("beast.user.package.dir", dir);
        }
        
        if (arguments.hasOption("loglevel")) {
            String l = arguments.getStringOption("loglevel");
            switch (l) {
                case "error":
                    Log.setLevel(Log.Level.error);
                    break;
                case "warning":
                    Log.setLevel(Log.Level.warning);
                    break;
                case "info":
                    Log.setLevel(Log.Level.info);
                    break;
	            case "debug":
	                Log.setLevel(Log.Level.debug);
	                break;
	            case "trace":
	                Log.setLevel(Log.Level.trace);
	                break;
            }
        }

        if (arguments.hasOption("version")) {
        	printVersion();
        	System.exit(0);
        }

        if (arguments.hasOption("help")) {
            printUsage(arguments);
            System.exit(0);
        }

        final boolean window = arguments.hasOption("window");
        final boolean options = arguments.hasOption("options");
        final boolean working = arguments.hasOption("working");
        final boolean doNotRun = arguments.hasOption("validate");
        String fileNamePrefix = null;
        String stateFileName = null;

        long seed = Randomizer.getSeed();
        boolean useJava = false;

        int threadCount = 1;

        if (arguments.hasOption("java")) {
            useJava = true;
        }

        if (arguments.hasOption("prefix")) {
            fileNamePrefix = arguments.getStringOption("prefix");
        }

        if (arguments.hasOption("statefile")) {
        	stateFileName = arguments.getStringOption("statefile");
        }

        long beagleFlags = 0;

        boolean useBeagle = arguments.hasOption("beagle") ||
                arguments.hasOption("beagle_CPU") ||
                arguments.hasOption("beagle_GPU") ||
                arguments.hasOption("beagle_SSE") ||
                arguments.hasOption("beagle_double") ||
                arguments.hasOption("beagle_single") ||
                arguments.hasOption("beagle_order");

        if (arguments.hasOption("beagle_scaling")) {
            System.setProperty("beagle.scaling", arguments.getStringOption("beagle_scaling"));
        }

        boolean beagleShowInfo = arguments.hasOption("beagle_info");

        
        boolean useSSE = true;
        if (arguments.hasOption("beagle_CPU")) {
            beagleFlags |= BeagleFlag.PROCESSOR_CPU.getMask();
            useSSE = false;
        }
        if (arguments.hasOption("beagle_GPU")) {
            beagleFlags |= BeagleFlag.PROCESSOR_GPU.getMask();
            useSSE = false;
        }
        if (arguments.hasOption("beagle_SSE")) {
            beagleFlags |= BeagleFlag.PROCESSOR_CPU.getMask();
            useSSE = true;
        }
        if (useSSE) {
            beagleFlags |= BeagleFlag.VECTOR_SSE.getMask();
        }
        if (arguments.hasOption("beagle_double")) {
            beagleFlags |= BeagleFlag.PRECISION_DOUBLE.getMask();
        }
        if (arguments.hasOption("beagle_single")) {
            beagleFlags |= BeagleFlag.PRECISION_SINGLE.getMask();
        }

        if (arguments.hasOption("noerr")) {
		 	System.setErr(new PrintStream(new OutputStream() {
		 		@Override
				public void write(int b) {
		 		}
		 	}));
        }        
        
        if (arguments.hasOption("beagle_order")) {
            System.setProperty("beagle.resource.order", arguments.getStringOption("beagle_order"));
        }

        if (arguments.hasOption("instances")) {
            System.setProperty("beast.instance.count", Integer.toString(arguments.getIntegerOption("instances")));
        }

        if (arguments.hasOption("beagle_scaling")) {
            System.setProperty("beagle.scaling", arguments.getStringOption("beagle_scaling"));
        }

        if (arguments.hasOption("threads")) {
            threadCount = arguments.getIntegerOption("threads");
        }
        if (threadCount <= 0) {
        	threadCount = Runtime.getRuntime().availableProcessors();
        	Log.warning.println("Setting number of threads to " + threadCount);
        }

        if (arguments.hasOption("seed")) {
            seed = arguments.getLongOption("seed");
            if (seed <= 0) {
                printTitle();
                Log.err.println("The random number seed should be > 0");
                System.exit(1);
            }
        }

        int maxErrorCount = 0;
        if (arguments.hasOption("errors")) {
            maxErrorCount = arguments.getIntegerOption("errors");
            if (maxErrorCount < 0) {
                maxErrorCount = 0;
            }
        }

        
        
        final String nameString = "BEAST " + version.getVersionString();

        if (window || options) {
        	launch(BeastMain.class, args);
        	return;
        } 

        printTitle();

        File inputFile = null;


        if (arguments.hasOption("overwrite")) {
            MCMCargs.add("-overwrite");
        }

        if (arguments.hasOption("resume")) {
            MCMCargs.add("-resume");
        }

        if (arguments.hasOption("strictversions")) {
        	MCMCargs.add("-strictversions");
        }
        
        if (arguments.hasOption("sampleFromPrior")) {
        	MCMCargs.add("-sampleFromPrior");
        }
        
        if (arguments.hasOption("D")) {
            MCMCargs.add("-D");
            MCMCargs.add(arguments.getStringOption("D"));
            for (String optionVal : arguments.getAdditionalStringOptions("D")) {
                MCMCargs.add("-D");
                MCMCargs.add(optionVal);
            }
        }
        if (arguments.hasOption("DF")) {
            MCMCargs.add("-DF");
            MCMCargs.add(arguments.getStringOption("DF"));
            for (String optionVal : arguments.getAdditionalStringOptions("DF")) {
                MCMCargs.add("-DF");
                MCMCargs.add(optionVal);
            }
        }
        if (arguments.hasOption("DFout")) {
            MCMCargs.add("-DFout");
            MCMCargs.add(arguments.getStringOption("DFout"));
        }

        if (arguments.hasOption("version_file")) {
            addServices(arguments.getStringOption("version_file"));
            for (String optionVal : arguments.getAdditionalStringOptions("version_file"))
                addServices(optionVal);
        }

        if (beagleShowInfo) {
            Log.info.println("\n--- BEAGLE RESOURCES ---\n");
            for (beagle.ResourceDetails details : BeagleFactory.getResourceDetails())
                Log.info.println(details.toString());

            if (window)
                return;
            else
                System.exit(0);
        }

        if (inputFile == null) {

            final String[] args2 = arguments.getLeftoverArguments();

            if (args2.length > 1) {
            	Log.err.println("Unknown option: " + args2[1]);
            	Log.err.println();
                printUsage(arguments);
                System.exit(1);
            }

            String inputFileName = null;


            if (args2.length > 0) {
                inputFileName = args2[0];
                inputFile = new File(inputFileName);
            }

            if (inputFileName == null) {
                // No input file name was given so throw up a dialog box...
            	inputFile = Utils.getLoadFile("BEAST " + version.getVersionString() + " - Select XML input file");
            	if (inputFile == null) {
            		System.exit(0);
            	}
            }
        }

        if (inputFile != null && inputFile.getParent() != null && working) {
            System.setProperty("file.name.prefix", inputFile.getParentFile().getAbsolutePath() + File.separator);
        }

        if (useJava) {
            System.setProperty("java.only", "true");
        }

        if (fileNamePrefix != null && fileNamePrefix.trim().length() > 0) {
            System.setProperty("file.name.prefix", fileNamePrefix.trim());
        }

        if (stateFileName!= null && stateFileName.trim().length() > 0) {
            System.setProperty("state.file.name", stateFileName.trim());
            Log.info.println("Writing state to file " + stateFileName);
        }

        if (beagleFlags != 0) {
            System.setProperty("beagle.preferred.flags", Long.toString(beagleFlags));

        }

        if (threadCount > 0) {
            System.setProperty("thread.count", String.valueOf(threadCount));
            MCMCargs.add("-threads");
            MCMCargs.add(threadCount + "");
        }

        MCMCargs.add("-seed");
        MCMCargs.add(seed + "");
        Randomizer.setSeed(seed);

        Log.info.println("Random number seed: " + seed);
        Log.info.println();

        // Construct the beast object
        final BeastMCMC beastMCMC = new BeastMCMC();

        try {
            // set all the settings...
            MCMCargs.add(inputFile.getAbsolutePath());
            beastMCMC.parseArgs(MCMCargs.toArray(new String[0]));
            
            if (!doNotRun) {
            	beastMCMC.run();
            } else {
            	beastMCMC.validate();
            }
            Log.info.println("Done!");
        } catch (RuntimeException rte) {
            if (window) {
                // This sleep for 2 seconds is to ensure that the final message
                // appears at the end of the console.
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Log.info.println();
                Log.info.println("BEAST has terminated with an error. Please select QUIT from the menu.");
            } else {
            	rte.printStackTrace();
            }
            // logger.severe will throw a RTE but we want to keep the console visible
        } catch (XMLParserException e) {
            Log.info.println(e.getMessage());
            if (!window) {
            	System.exit(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (!window) {
            	System.exit(1);
            }
        }

        if (!window) {
            System.exit(0);
        }
	}
}