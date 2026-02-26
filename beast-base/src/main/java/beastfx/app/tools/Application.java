package beastfx.app.tools;



import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.json.JSONObject;

import beastfx.app.beauti.ThemeProvider;
import beastfx.app.inputeditor.BEASTObjectDialog;
import beastfx.app.inputeditor.BEASTObjectPanel;
import beastfx.app.inputeditor.BeautiConfig;
import beastfx.app.inputeditor.BeautiDoc;
import beastfx.app.util.Console;
import beastfx.app.util.Utils;
import javafx.application.Platform;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import beast.base.core.BEASTObject;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Log;
import beast.base.core.ProgramStatus;
import beast.pkgmgmt.BEASTClassLoader;

@Description("BEAST application that handles argument parsing by introspection "
		+ "using Inputs declared in the class.")
public class Application extends Console {

	BEASTObject myBeastObject;

	public Application() {
	}
	
	public Application(BEASTObject myBeastObject) {
		this.myBeastObject = myBeastObject;
	}
	
	@Override
	protected void createDialog() {
		// Utils6.startSplashScreen();
		if (Utils.isMac()) {
			Utils.loadUIManager();
		}
		// create BeautiDoc and beauti configuration
		BeautiDoc doc = new BeautiDoc();
		doc.beautiConfig = new BeautiConfig();
		doc.beautiConfig.initAndValidate();

		// suppress a few inputs that we don't want to expose to the user
		if (suppressedInputs != null) {
			for (String suppressedInput : suppressedInputs) {
				doc.beautiConfig.suppressBEASTObjects.add(suppressedInput);
			}
		}
		
		// create panel with entries for the application
		BEASTObjectPanel panel = new BEASTObjectPanel(analyser, analyser.getClass(), doc);
		
		// wrap panel in a dialog
		BEASTObjectDialog dialog = new BEASTObjectDialog(panel, null);

		// Utils6.endSplashScreen();
		
		dialog.setResizable(true);
		dialog.getDialogPane().setPrefSize(prefDialogWidth, prefDialogHeight);
		dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		ThemeProvider.loadStyleSheet(dialog.getDialogPane().getScene());
		
		// show the dialog
		Optional<ButtonType> option = dialog.showAndWait();
		long start = System.currentTimeMillis();
		if (option.get().equals(ButtonType.OK)) {
			dialog.accept(analyser, doc);
//			// create a console to show standard error and standard output
//			ConsoleApp app = new ConsoleApp(title, 
//					title,
//					null
//					);
			analyser.initAndValidate();
			try {
				analyser.run();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		long end = System.currentTimeMillis();
		Log.warning(title + " done in " + (end-start)/1000 + " seconds. Close window to finish application.");
		return;
	}
	
	static beast.base.inference.Runnable analyser;
	static String title;
	static String[] suppressedInputs;
	static int prefDialogWidth = 1024, prefDialogHeight = 768;
	
	public Application(beast.base.inference.Runnable analyser, String title, String[] args) throws Exception {
		this(analyser, null, title, args);
	}

	public Application(beast.base.inference.Runnable analyser, String title, int prefDialogWidth, int prefDialogHeight, String[] args) throws Exception {
		this(analyser, null, title, prefDialogWidth, prefDialogHeight, args);
	}

	public Application(beast.base.inference.Runnable analyser, String[] suppressedInputs, String title, String[] args) throws Exception {
		this(analyser, suppressedInputs, title, prefDialogWidth, prefDialogHeight, args);
	}
	
	public Application(beast.base.inference.Runnable analyser, String[] suppressedInputs, String title, int prefDialogWidth, int prefDialogHeight, String[] args) throws Exception {
		this.prefDialogWidth = prefDialogWidth;
		this.prefDialogHeight = prefDialogHeight;
		this.analyser =  analyser;
		this.title = title;
		this.suppressedInputs = suppressedInputs;
		analyser.setID(title);

		if (args.length == 0) {
			if (ProgramStatus.name.equals("BEAUti")) {
				// if we are in BEAUti, launch() is already called, so cannot call it again
				Platform.runLater(()->{
		            try {
		                Application application = new Application(analyser);
		                Stage primaryStage = new Stage();
		                application.start(primaryStage);
		            } catch (Exception e) {
		                e.printStackTrace();
		            }
		        });
			} else {
				launch(Application.class, args);
			}
			return;
		}

		Application main = new Application(analyser);
		main.parseArgs(args, false);
		analyser.initAndValidate();
		analyser.run();
	}


	/** default input used for argument parsing **/
	protected Input<?> defaultInput = null;

	public void setDefaultInput(Input input) {
		defaultInput = input;
	}

	/**
	 * Arguments of the form -name value are processed by finding Inputs with
	 * matching name and setting their value.
	 * 
	 * If the input is a boolean that needs to be set to true, the 'value'
	 * argument can be omitted.
	 * 
	 * The last argument is assigned to the defaultInput.
	 * **/
	public void parseArgs(String[] args, boolean sloppy) throws Exception {
		List<Input<?>> inputs = myBeastObject.listInputs();
		for (Input<?> input : inputs) {
			input.determineClass(this);
		}

		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			boolean done = false;
			if (arg.startsWith("-")) {
				String name = arg.substring(1);
				String value = (i < args.length - 1 ? args[i + 1] : null);
				Input<?> matchingInput = null;
				for (Input<?> input : inputs) {
					if (input.getName().equals(name)) {
						matchingInput = input;
						break;
					}
				}
				if (matchingInput == null) {
					// nothing matches, see whether a unique prefix match exists
					for (Input<?> input : inputs) {
						if (input.getName().startsWith(name)) {
							if (matchingInput == null) {
								matchingInput = input;
							} else {
								throw new IllegalArgumentException("Multiple matches for argument -"+name + ". Use more letters in the argument name.");
							}
							break;
						}
					}
				}
				if (matchingInput != null) {
					try {
						if (matchingInput.getType() == Boolean.class) {
							if (value != null
									&& (value.toLowerCase().equals("true") || value
											.toLowerCase().equals("false"))) {
								matchingInput.setValue(value, null);
								i++;
							} else {
								matchingInput.setValue(Boolean.TRUE, null);
							}
						} else if (matchingInput.get() != null && matchingInput.get() instanceof List) {
							do {
								Class c = matchingInput.getType();
								if (c == null) {
									matchingInput.determineClass(myBeastObject);
									c = matchingInput.getType();
								}
					            Constructor ctor = null;
					            try {
					            	ctor = c.getDeclaredConstructor(String.class);
					            } catch (NoSuchMethodException e) {
					            
					            }
					            value = args[i + 1];
					            Object o = ctor.newInstance(value);
								((List)matchingInput.get()).add(o);
								i++;
							} while (i + 1 < args.length && !args[i+1].startsWith("-"));
						} else {
							matchingInput.setValue(value, myBeastObject);
							i++;
						}
					} catch (Exception e) {
						throw new IllegalArgumentException("Problem parsing arguments:\n"
								+ e.getMessage());
					}
					done = true;
				} else if (name.equals("help")) {
					Log.info.println(getUsage());
					// CLI usage only
					System.exit(0);
				} else if (name.equals("version_file")) {
					i++;
	        		while (i < args.length && !args[i].startsWith("-")) {
	        			BEASTClassLoader.addServices(args[i]);
	        			i++;
	        		}
	        		i--;
	        		done = true;
				} else if (name.equals("wd")) {
					// set working directory
					ProgramStatus.g_sDir = value;
					i++;
	        		done = true;
				}
				if (!done) {
					
					throw new IllegalArgumentException("Could not find match for argument -" + name + "\n"
							+ getUsage());
				}
			} else {
				if (defaultInput != null) {
					if (defaultInput.get() instanceof Collection) {
						StringBuilder str = new StringBuilder();
						for (int j = i; j < args.length; j++) {
							arg = args[j];
							if (arg.startsWith("-")) {
								throw new Exception("Problem parsing arguments: are all arguments specified by a dash?");
							}
							str.append(arg);
							str.append(" ");
						}
						defaultInput.setValue(str.toString(), null);
						i = args.length;
						done = true;
					} else if (i == args.length-1) {
						defaultInput.setValue(arg, null);
						done = true;
					}
				}
			}
			if (!done) {
				if (sloppy) {
					Log.info.println("Unknown argument: " + args[i]
							+ " ignored.");
					i++;
				} else {
					throw new IllegalArgumentException("Unknown argument: " + args[i] + "\n");
							//+ getUsage());
				}
			}
		}

		myBeastObject.validateInputs();
	}

	protected void parseArgs(JSONObject args) throws Exception {
		List<String> argList = new ArrayList<>();
		for (String key : args.keySet()) {
			argList.add("-" + key.trim());
			argList.add(args.get(key).toString().trim());
		}
		parseArgs(argList.toArray(new String[] {}), true);
	}

	public String getUsage() {
		StringBuffer buf = new StringBuffer();
		try {
			List<Input<?>> inputs = myBeastObject.listInputs();
			buf.append("Usage: " + myBeastObject.getClass().getName() + "\n");
			buf.append(myBeastObject.getDescription() + "\n");
			for (Input<?> input : inputs) {
				buf.append("-" + input.getName() + " ");
				try {
					Class<?> typeClass = input.getType();
					if (typeClass == null) {
						input.determineClass(myBeastObject);
					}
					buf.append(input.getValueTipText());
				} catch (Exception e) {
					// ignore
				}
				buf.append("\t" + input.getTipText());
				if (input.defaultValue != null) {
					buf.append(" (default: " + input.defaultValue + ")");
				}
				buf.append("\n");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		buf.append("-help\t show arguments");
		buf.append("-help\t Provide one or more version files containing a list of services to explicitly allow. (Useful for package development.)");
		return buf.toString();
	}

	// placeholder, so that the main method compiles
	public void run() throws Exception {
		myBeastObject.initAndValidate();
	};

	// template for implementing a main for an application
	// the first argument is interpreted as class name of a BEASTObject
//	public static void main(final String[] args) throws Exception {
//		Application main = null;
//		try {
//			BEASTObject myBeastObject = (BEASTObject) BEASTClassLoader.forName(args[0])
//					.newInstance();
//			main = new Application(myBeastObject);
//			String[] args2 = new String[args.length - 1];
//			System.arraycopy(args, 1, args2, 0, args2.length);
//			main.parseArgs(args2, false);
//			main.run();
//		} catch (Exception e) {
//			System.out.println("Error:" + e.getMessage());
//			if (main != null) {
//				System.out.println(main.getUsage());
//			}
//		}
//	}

	// utility function to open a webpage written by an application in a web browser
		static public void openUrl(String url) throws IOException {
			url = url.replaceAll(" ", "%20");
		    if(Desktop.isDesktopSupported()){
		        Desktop desktop = Desktop.getDesktop();
		        try {
		            desktop.browse(new URI(url));
		            return;
		        } catch (IOException | URISyntaxException e) {
		            // TODO Auto-generated catch block
		            e.printStackTrace();
		        }
		    }
		    if (Utils.isWindows()) {
		    	Runtime rt = Runtime.getRuntime();
		    	rt.exec( "rundll32 url.dll,FileProtocolHandler " + url);
		    } else if (Utils.isMac()) {
		    	Runtime rt = Runtime.getRuntime();
		    	rt.exec( "open" + url);
		    } else {
		    	// Linux:
		    	Runtime rt = Runtime.getRuntime();
		    	String[] browsers = {"epiphany", "firefox", "mozilla", "konqueror",
		    	                                 "netscape","opera","links","lynx"};

		    	StringBuffer cmd = new StringBuffer();
		    	for (int i=0; i<browsers.length; i++) {
		    	     cmd.append( (i==0  ? "" : " || " ) + browsers[i] +" \"" + url + "\" ");
		    	}
		    	rt.exec(new String[] { "sh", "-c", cmd.toString() });
		    }
		    
		   }
		
		// return path containing package jar file
		public static String getPackagePath(String jar) {
			jar = jar.toLowerCase();
			String classpath = System.getProperty("java.class.path");
			String[] classpathEntries = classpath.split(File.pathSeparator);
			String FILESEP = "/";
			if (Utils.isWindows()) {
				FILESEP = "\\\\";
			}
			for (String pathEntry : classpathEntries) {
				//Log.debug.print("Trying >" + pathEntry + "< ");
				if (new File(pathEntry).getName().toLowerCase().equals(jar)) {
					Log.debug.println("Got it!");
					File parentFile = (new File(pathEntry)).getParentFile().getParentFile();
					String parent = parentFile.getPath();
					return parent + FILESEP;
				}
				//Log.debug.println("No luck ");
			}
			String jsPath = System.getProperty("user.dir") + FILESEP;
			//Log.debug.println("Using default: " + jsPath);
			return jsPath;
		}

}
