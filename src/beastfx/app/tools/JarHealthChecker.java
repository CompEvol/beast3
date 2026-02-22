package beastfx.app.tools;


import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import beastfx.app.util.OutFile;
import beast.base.core.BEASTInterface;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Log;
import beast.base.inference.Runnable;
import beast.pkgmgmt.BEASTClassLoader;
import beast.pkgmgmt.PackageManager;


@Description("Checks the health of a jar file with classes for a package")
public class JarHealthChecker extends Runnable {
	final public Input<File> jarFileInput = new Input<>("jar", "jar-file containing BEAST package classes", new File(OutFile.NO_FILE)); 
	final public Input<OutFile> outputInput = new Input<>("output", "output-file where report is stored. Use stdout if not specified.", new OutFile(OutFile.NO_FILE)); 
	final public Input<String> namespaceInput = new Input<>("namespace", "only classes inside this package will be listed", "beast"); 

	
	final static String [] knownServices = {
			"beast.base.core.BEASTInterface",
			"beast.base.evolution.datatype.DataType",
			"beast.base.inference.ModelLogger",
			"beastfx.app.inputeditor.InputEditor",
			"beastfx.app.inputeditor.AlignmentImporter",
			"beastfx.app.beauti.BeautiHelpAction",		
			"beastfx.app.beauti.PriorProvider",
			"beastfx.app.beauti.ThemeProvider"
	};
	
	private PrintStream out = System.out;
//	private Set<String> classesInJar;
//	private Map<String, Set<String>> declaredServices;
	
	public JarHealthChecker() {}
	public JarHealthChecker(File jarFile, String namespace) {
		initByName("jar", jarFile, "namespace", namespace);
		try {
			URL url;
			url = new URL("file:///" + jarFile.getPath());
//			BEASTClassLoader.classLoader.addURL(url, namespace);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			throw new IllegalArgumentException(e.getMessage());
		}
	}
	
	@Override
	public void initAndValidate() {
	}

	@Override
	public void run() throws Exception {
		out = System.out;
		if (OutFile.isSpecified(outputInput.get())) {
			out = new PrintStream(outputInput.get());
		}
		File jarFile = jarFileInput.get();	
		//BEASTClassLoader.newInstance(null);
		Set<String> classesInJar = collectClasses(jarFile);
		checkServices(classesInJar, out, new HashMap<>());
		checkApps(jarFile.getName(), classesInJar, out, new ArrayList<>());
		
		if (OutFile.isSpecified(outputInput.get())) {
			out = new PrintStream(outputInput.get());
		}

		if (OutFile.isSpecified(outputInput.get())) {
			out.close();
		}
		Log.warning("Done");
	}
	
	public boolean checkApps(String jarfile, Set<String> classesInJar, PrintStream out, List<PackageApp> packageApps) {
		report("Checking package apps in " + jarfile + " ...");
		
		if (classesInJar.size() == 0) {
			report("No classes found in jar file! Check whether the build file includes classes.");
			return false;
		}
		boolean packageAppDeclarationMissing = false;
		StringBuilder b = new StringBuilder();
		for (String class_ : classesInJar) {
			if (!class_.startsWith("test."))
			try {
				Method mainMethod = BEASTClassLoader.forName(class_).getMethod("main", new String [] {}.getClass());
				if (mainMethod != null) {
					boolean found = false;
					for (PackageApp p : packageApps) {
						if (p.className.equals(class_)) {
							found = true;
							break;
						}
					}
					if (!found) {
						b.append("<!-- " + class_ + " -->\n");
						if (!packageAppDeclarationMissing) {
							packageAppDeclarationMissing = true;
						}
						Object o = BEASTClassLoader.forName(class_).getDeclaredConstructors()[0].newInstance();
						String description = class_; 
						if (o instanceof BEASTInterface) {
							description = ((BEASTInterface)o).getDescription();
						}
						b.append("    <packageapp description=\"" + description + "\"\n");
						b.append("        class=\"" + class_ + "\"\n");
						b.append("        args=\"\"/>\n\n");
						packageAppDeclarationMissing = true;
					}
				}
			} catch (Throwable e) {
			  // ignore
			}
		}
		if (packageAppDeclarationMissing) {
			report("\nSuggested xml fragment for the version.xml file to declare package applications (because these classes have a main() method):\n");
			report(b.toString());
		} else {
			report("Looks OK: No classes with main() method found" + (packageApps.size() > 0?" that were not already declared" : "") + ".");
		}
		return packageAppDeclarationMissing;	
	}
	
	private Set<String> collectClasses(File file) {
		Set<String> classesInJar = new HashSet<>();				
		try {
			BEASTClassLoader.classLoader.addJar(file.getPath());
		} catch (Throwable t) {
			// ignore
		}
		try {
			JarFile jar = new JarFile(file);
			ZipInputStream zip = new ZipInputStream(new FileInputStream(file));
			while(true) {
				ZipEntry e = zip.getNextEntry();
				if (e == null)
					break;
				String name = e.getName();
				if (name.endsWith("class")) {
					if (classesInJar.contains(name)) {
						report("Class " + name + " has multiple entries in jar files");
					}
					name = name.substring(0, name.length() - 6).replaceAll("/", ".");
					classesInJar.add(name);
					
					// check java version of the class 
					java.io.InputStream is = jar.getInputStream(e);
					DataInputStream input = new DataInputStream(is);
					input.skipBytes(4);
					int minorVersion = input.readUnsignedShort();
					int majorVersion = input.readUnsignedShort();
					if (majorVersion > 61) {
						input.close();
						zip.close();
						jar.close();
						throw new Error("Fatal error: Class " + name + " is compiled with java version > java 17. "
								+ "BEAST only handles java classes up to java 17");
					}
				}
			}
			zip.close();
			jar.close();
		} catch (IOException e) {
			report(e.getMessage());
		}	
		return classesInJar;
	}
	
	public void checkServices(Set<String> classesInJar, PrintStream out, Map<String, Set<String>> declaredServices) {
		this.out = out;
		report("Checking services...");
		boolean serviceDeclarationMissing = false;

		if (declaredServices.size() == 0) {
			StringBuilder b = new StringBuilder();
			b.append("No declared services found. If there are any classes that are one of these:\n");
			for (String service : knownServices)  {
				b.append("o " + service +"\n");
			}
			report(b.toString());
			serviceDeclarationMissing = true;
		}

		// check all services declared are actually in this package
		for (String service : declaredServices.keySet()) {
			for (String className : declaredServices.get(service)) {
				if (!classesInJar.contains(className)) {
					report("Service " + service + " declared with class " + className + " but class could not be found in any jar "
							+ "(possibly a typo in the class name used to declare the class in the version.xml file)");
				}
			}
		}
		
		for (String service : knownServices) {
			if (checkService(service, classesInJar, declaredServices)) {
				serviceDeclarationMissing = true;	
			}
		}


		if (serviceDeclarationMissing) {
			// showServiceInfo();
		} else {
			report("Services look OK");
		}
	}

	private boolean checkService(String service, Set<String> classesInJar, Map<String, Set<String>> declaredServices) {		
		boolean serviceDeclarationMissing = false;
		List<String> list = PackageManager.find(service, namespaceInput.get());
		for (String clazz : list) {
			if (!clazz.equals(service) && 
				clazz.indexOf('$') < 0 &&  
				classesInJar.contains(clazz)) {
				if (!hasDeclaredService(declaredServices, service, clazz)) {
					report("Expected class " + clazz + " to be declared as service " + service);
					serviceDeclarationMissing = true;
				}

			}
		}
		if (serviceDeclarationMissing) {
			report("\nSuggested xml fragment for the version.xml file:\n");
			StringBuilder b = new StringBuilder();
			b.append("    <service type=\"" + service + "\">\n");
			for (String clazz : list) {
				if (!clazz.equals(service) && 
					clazz.indexOf('$') < 0 &&  
					classesInJar.contains(clazz)) {
					b.append("        <provider classname=\"" + clazz + "\"/>\n");
				}
			}
			b.append("    </service>\n");
			report(b.toString());
		}
		return serviceDeclarationMissing;
	}
	
	private boolean hasDeclaredService(Map<String,Set<String>> declaredServices, String service, String className) {
		Set<String> services = declaredServices.get(service);		
		if (services == null) {
			return false;
		}
		return services.contains(className);
	}

	private void showServiceInfo() {
		report("\n\nTo declare services in a jar file, in the version.xml file,inside the 'jar' element that creates the jar file, "
				+ "add a 'service' element with appropriate type attribute to the build.xml file, and 'provider' elements for each class "
				+ "that provides the service. For example, the DataType services in beast.base are declared like so:\n"
				+ "    <service type=\"beast.base.evolution.datatype.DataType\">\n"
				+ "        <provider classname=\"beast.base.evolution.datatype.Aminoacid\"/>\n"
				+ "        <provider classname=\"beast.base.evolution.datatype.Nucleotide\"/>\n"
				+ "        <provider classname=\"beast.base.evolution.datatype.TwoStateCovarion\"/>\n"
				+ "        <provider classname=\"beast.base.evolution.datatype.Binary\"/>\n"
				+ "        <provider classname=\"beast.base.evolution.datatype.IntegerData\"/>\n"
				+ "        <provider classname=\"beast.base.evolution.datatype.StandardData\"/>\n"
				+ "        <provider classname=\"beast.base.evolution.datatype.UserDataType\"/>\n"
				+ "    </service>\n");
	}


	
	private void report(String msg) {
		out.println(msg);
	}

	public static void main(String[] args) throws Exception {
		new Application(new JarHealthChecker(), "Jar Health Checker", args);
	}
}
