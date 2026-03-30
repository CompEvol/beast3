/*
 * File PackageManager.java
 *
 * Copyright (C) 2010 Remco Bouckaert remco@cs.auckland.ac.nz
 *
 * This file is part of BEAST.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership and licensing.
 *
 * BEAST is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 *  BEAST is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with BEAST; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA  02110-1301  USA
 */
/*
 * Parts copied from WEKA ClassDiscovery.java
 * Copyright (C) 2005 University of Waikato, Hamilton, New Zealand
 *
 */

package beast.pkgmgmt;





import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.io.*;
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.*;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * This class is used to manage beast 2 packages, and can
 * - install a new package
 * - un-install an package
 * - list directories that may contain packages
 * - load jars from installed packages
 * - discover classes in packages that implement a certain interface or a derived from a certain class
 */
// TODO: on windows allow installation on drive D: and pick up add-ons in drive C:
public class PackageManager {
	

	
    public static final BEASTVersion beastVersion = BEASTVersion.INSTANCE;

    public enum UpdateStatus {AUTO_CHECK_AND_ASK, AUTO_UPDATE, DO_NOT_CHECK};

    // public final static String[] IMPLEMENTATION_DIR = {"beast", "snap"};
    public final static String TO_DELETE_LIST_FILE = PackageInstaller.TO_DELETE_LIST_FILE;
    public final static String TO_INSTALL_LIST_FILE = PackageInstaller.TO_INSTALL_LIST_FILE;
    
    public final static String BEAST_PACKAGE_NAME = "BEAST";
    public final static String BEAST_BASE_PACKAGE_NAME = "BEAST.base";

    public final static String PACKAGES_XML = PackageRepository.PACKAGES_XML;
    public final static String PACKAGES_XML_BACKUP = PackageRepository.PACKAGES_XML_BACKUP;

    public final static String ARCHIVE_DIR = PackageInstaller.ARCHIVE_DIR;
    
    public static void useArchive(boolean _useArchive) {
    	PackageInstaller.useArchive(_useArchive);
    }
    
    public static final String INSTALLED = "installed";
    public static final String NOT_INSTALLED = "not installed";
    
    public static final String NO_CONNECTION_MESSAGE = "Could not get an internet connection. "
    		+ "The " + BEAST_PACKAGE_NAME + " Package Manager needs internet access in order to list available packages and download them for installation. "
    		+ "Possibly, some software (like security software, or a firewall) blocks the " + BEAST_PACKAGE_NAME + " Package Manager.  "
    		+ "If so, you need to reconfigure such software to allow access.";


    /**
     * Exception thrown when reading a package repository fails.
     * @see PackageRepository.PackageListRetrievalException
     */
    public static class PackageListRetrievalException extends PackageRepository.PackageListRetrievalException {
		private static final long serialVersionUID = 1L;

        public PackageListRetrievalException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Exception thrown when an operation fails due to package dependency issues.
     * @see DependencyResolver.DependencyResolutionException
     */
    public static class DependencyResolutionException extends DependencyResolver.DependencyResolutionException {
		private static final long serialVersionUID = 1L;

        public DependencyResolutionException(String message) {
            super(message);
        }
    }

    /**
     * flag indicating add ons have been loaded at least once *
     */
    static boolean externalJarsLoaded = false;

    /**
     * Directories from deprecated search paths (system dir, install dir,
     * classpath, archive).  If a package is loaded from one of these,
     * a warning is logged advising the user to migrate.
     */

    /**
     * list of all classes found in the class path *
     */
    private static List<String> all_classes;

    /**
     * @return URLs containing list of downloadable packages.
     * @throws java.net.MalformedURLException
     */
    public static List<URL> getRepositoryURLs() throws MalformedURLException {
        return new PackageRepository().getURLs();
    }

    /**
     * Write any third-party package repository URLs to the options file.
     *
     * @param urls List of URLs.  The first is assumed to be the central
     * package repository and is thus ignored.
     */
    public static void saveRepositoryURLs(List<URL> urls) {
        new PackageRepository().saveURLs(urls);
    }

    /**
     * Look through BEAST directories for installed packages and add these
     * to the package database.
     *
     * @param packageMap package database
     */
    public static void addInstalledPackages(Map<String, Package> packageMap) {
        for (String dir : getBeastDirectories()) {
            File versionXML = new File(dir + "/version.xml");
            if (!versionXML.exists())
                continue;

            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                Document doc = factory.newDocumentBuilder().parse(versionXML);
                doc.normalize();
                // get name and version of package
                Element packageElement = doc.getDocumentElement();
                String packageName = packageElement.getAttribute("name");
                String packageVersionString = packageElement.getAttribute("version");

                Package pkg;
                if (packageMap.containsKey(packageName)) {
                    pkg = packageMap.get(packageName);
                } else {
                    pkg = new Package(packageName);
                    packageMap.put(packageName, pkg);
                }

                if (packageElement.hasAttribute("projectURL"))
                    pkg.setProjectURL(new URL(packageElement.getAttribute("projectURL")));

                PackageVersion installedVersion = new PackageVersion(packageVersionString);

                if (packageElement.hasAttribute("projectURL") &&
                        !(pkg.getLatestVersion() != null && installedVersion.compareTo(pkg.getLatestVersion())<0))
                    pkg.setProjectURL(new URL(packageElement.getAttribute("projectURL")));

                Set<PackageDependency> installedVersionDependencies =
                        new TreeSet<PackageDependency>(new Comparator<PackageDependency>() {
							@Override
							public int compare(PackageDependency o1, PackageDependency o2) {
								return o1.dependencyName.compareTo(o2.dependencyName);
							}
						});

                // get dependencies of add-n
                NodeList nodes = doc.getElementsByTagName("depends");
                for (int i = 0; i < nodes.getLength(); i++) {
                    Element dependson = (Element) nodes.item(i);
                    String dependencyName = dependson.getAttribute("on");
                    String atLeastString = dependson.getAttribute("atleast");
                    String atMostString = dependson.getAttribute("atmost");
                    PackageDependency dependency =  new PackageDependency(
                            dependencyName,
                            atLeastString.isEmpty() ? null : new PackageVersion(atLeastString),
                            atMostString.isEmpty() ? null : new PackageVersion(atMostString));

                    installedVersionDependencies.add(dependency);
                }

                pkg.setInstalled(installedVersion, installedVersionDependencies);

            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Manually set currently-installed BEAST 2 version if not already set
        // This can happen when the BEAST package is not installed (perhaps due to 
        // file access issues)
        Package beastPkg;
        if (packageMap.containsKey(BEAST_BASE_PACKAGE_NAME)) {
            beastPkg = packageMap.get(BEAST_BASE_PACKAGE_NAME);
        } else {
            beastPkg = new Package(BEAST_BASE_PACKAGE_NAME);
            packageMap.put(BEAST_BASE_PACKAGE_NAME, beastPkg);
        }

        if (!beastPkg.isInstalled()) {
            PackageVersion beastPkgVersion = new PackageVersion(beastVersion.getVersion());
            Set<PackageDependency> beastPkgDeps = new TreeSet<PackageDependency>();
            beastPkg.setInstalled(beastPkgVersion, beastPkgDeps);
        }

    }

    /**
     * Look through the packages defined in the XML files reached by the repository URLs
     * and add these packages to the package database.
     *
     * @param packageMap package database
     * @throws PackageListRetrievalException when one or more XMLs cannot be retrieved
     */
    public static void addAvailablePackages(Map<String, Package> packageMap) throws PackageRepository.PackageListRetrievalException {
        new PackageRepository().addAvailablePackages(packageMap);
    }


    /**
     * Looks through packages to be installed and uninstalls any that are already installed but
     * do not match the version that is to be installed. Packages that are already installed and do
     * match the version required are removed from packagesToInstall.
     *
     * @param packagesToInstall map from packages to versions to install
     * @param useAppDir if fause, use user directory, otherwise use application directory
     * @param customDir custom installation directory.
     * @throws IOException thrown if packages cannot be deleted and delete list file cannot be written
     */
    public static void prepareForInstall(Map<Package, PackageVersion> packagesToInstall, boolean useAppDir, String customDir) throws IOException {
        PackageInstaller.prepareForInstall(packagesToInstall, useAppDir, customDir);
    }

    /**
     * Download and install specified versions of packages.  Note that
     * this method does not check dependencies.  It is assumed the contents
     * of packagesToInstall has been assembled by fillOutDependencies.
     *
     * It is further assumed that the URL points to a zip file containing
     * a directory lib containing jars used by the package, as well as
     * a directory named templates containing BEAUti XML templates.
     *
     * @param packagesToInstall map from packages to versions to install
     * @param useAppDir if false, use user directory, otherwise use application directory
     * @param customDir custom installation directory.
     * @return list of strings representing directories into which packages were installed
     * @throws IOException if URL cannot be accessed for some reason
     */
    public static Map<String, String> installPackages(Map<Package, PackageVersion> packagesToInstall, boolean useAppDir, String customDir) throws IOException {
        return PackageInstaller.installPackages(packagesToInstall, useAppDir, customDir);
    }

	public static String getPackageDir(Package thisPkg, PackageVersion thisPkgVersion, boolean useAppDir, String customDir) {
        return PackageInstaller.getPackageDir(thisPkg, thisPkgVersion, useAppDir, customDir);
	}

	/**
     * Get list of installed packages that depend on pkg.
     *
     * @param pkg package for which to retrieve installed dependencies
     * @param packageMap package database
     * @return list of names of installed packages dependent on pkg.
     */
    /**
     * Get list of installed packages that depend on pkg.
     *
     * @param pkg package for which to retrieve installed dependencies
     * @param packageMap package database
     * @return list of names of installed packages dependent on pkg.
     */
    public static List<String> getInstalledDependencyNames(Package pkg, Map<String, Package> packageMap) {
        List<String> dependencies = new ArrayList<>();

        for (Package thisPkg : packageMap.values()) {
            if (thisPkg.equals(pkg))
                continue;
            if (!thisPkg.isInstalled())
                continue;

            for (PackageDependency dependency : thisPkg.getInstalledVersionDependencies()) {
                if (dependency.dependencyName.equals(pkg.getName()))
                    dependencies.add(thisPkg.getName());
            }
        }
        return dependencies;
    }

    /**
     * Uninstall the given package. Like installPackages(), this method does not perform any dependency
     * checking - it just blindly removes the specified package. This is so that the method can be called
     * while an installation is in process without falling over because of broken intermediate states.
     *
     * Before using, call getInstalledDependencies() to check for potential problems.
     *
     * @param pkg package to uninstall
     * @param useAppDir if false, use user directory, otherwise use application directory
     * @param customDir custom installation directory.
     * @return name of directory package was removed from, or null if the package was not removed.
     * @throws IOException thrown if packages cannot be deleted and delete list file cannot be written
     */
    public static String uninstallPackage(Package pkg, boolean useAppDir, String customDir) throws IOException {
        return PackageInstaller.uninstallPackage(pkg, useAppDir, customDir);
    }

    public static String uninstallPackage(Package pkg, PackageVersion pkgVersion, boolean useAppDir, String customDir) throws IOException {
        return PackageInstaller.uninstallPackage(pkg, pkgVersion, useAppDir, customDir);
    }

    /**
     * Unzip a zip archive.
     */
    public static void doUnzip(String inputZip, String destinationDirectory) throws IOException {
        PackageInstaller.doUnzip(inputZip, destinationDirectory);
    }

    /**
     * @return directory where to install packages for users *
     */
    public static String getPackageUserDir() {
        return Utils6.getPackageUserDir(BEAST_PACKAGE_NAME);
    }

    /**
     * @return directory where system wide packages reside *
     */
    public static String getPackageSystemDir() {
        return Utils6.getPackageSystemDir(BEAST_PACKAGE_NAME);
    }

    /**
     * Returns directory where BEAST installation resides, based on the location of the jar containing the
     * beast.pkgmgmt.PackageManager class file.  This assumes that the parent directory of the launcher.jar is the base install
     * directory.
     *
     * @return string representation of BEAST install directory or null if this directory cannot be identified.
     */
    public static String getBEASTInstallDir() {
    	return getInstallDir(BEAST_PACKAGE_NAME, "beast.pkgmgmt.PackageManager");
    	
    }
    
    public static String getInstallDir(String application, String mainClass) {
    	String prefix = application.toLowerCase();
        // Allow users to explicitly set install directory - handy for programmers
        if (System.getProperty(prefix + ".install.dir") != null)
            return System.getProperty(prefix + ".install.dir");

        
        URL u;
		try {
			u = BEASTClassLoader.forName(mainClass).getProtectionDomain().getCodeSource().getLocation();
		} catch (ClassNotFoundException e) {
			// e.printStackTrace();
			return null;
		}
		String s = u.getPath();
        File beastJar = new File(s);
        // Log.trace.println("BeastMain found in " + beastJar.getPath());
        if (!beastJar.getName().toLowerCase().endsWith(".jar")) {
        	return null;
        }

        if (beastJar.getParentFile() != null) {
            return beastJar.getParentFile().getParent();
        } else {
            return null;
        }
    }

    /**
     * @return file containing list of files that need to be deleted
     * but could not be deleted. This can happen when uninstalling packages
     * on windows, which locks jar files loaded by java.
     */
    public static File getToDeleteListFile() {
        return PackageInstaller.getToDeleteListFile();
    }

    private static void processDeleteList() {
        PackageInstaller.processDeleteList();
    }

    public static File getToInstallListFile() {
        return PackageInstaller.getToInstallListFile();
    }

    private static void processInstallList(Map<String, Package> packageMap) {
        PackageInstaller.processInstallList(packageMap);
    }

    /**
     * return list of directories that may contain packages *
     */
    public static List<String> getBeastDirectories() {

        List<String> dirs = new ArrayList<String>();

        // 1. BEAST_PACKAGE_PATH: configurable override for CI or custom layouts
        if (PackageManager.getBeastPackagePathProperty() != null) {
            String BEAST = PackageManager.getBeastPackagePathProperty();
            for (String dirName : BEAST.split(":")) {
                dirs.add(dirName);
            }
        }

        // 2. User package directory: ~/.beast/2.8/
        dirs.add(getPackageUserDir());

        // 3. System package directory (e.g. cluster-wide installs managed by admins)
        dirs.add(getPackageSystemDir());

        // 4. BEAST installation directory
        if (getBEASTInstallDir() != null) {
            dirs.add(getBEASTInstallDir());
        }

        // Scan subdirectories that look like they contain a package
        // (detected by checking the subdirectory contains version.xml)
        List<String> subDirs = new ArrayList<String>();
        for (String dirName : dirs) {
            File dir = new File(dirName);
            if (dir.isDirectory()) {
                File[] files = dir.listFiles();
                if (files == null)
                    continue;

                for (File file : files) {
                    if (file.isDirectory()) {
                        File versionFile = new File(file, "version.xml");
                        if (versionFile.exists()) {
                            subDirs.add(file.getAbsolutePath());
                        }
                    }
                }
            }
        }

        subDirs.addAll(dirs);
        dirs = subDirs;

        return dirs;
    }
    
    /*
     * Get directories from archive, if not already loaded when traversing visitedDirs.
     * Only add the latest version from the archive.
     */
    private static List<String> getLatestBeastArchiveDirectories(List<String> visitedDirs) {
        List<String> dirs = new ArrayList<String>();
        String FILESEPARATOR = "/"; //(Utils6.isWindows() ? "\\" : "/");

    	String dir = getPackageUserDir() + FILESEPARATOR + ARCHIVE_DIR;
    	File archiveDir = new File(dir);
    	if (archiveDir.exists()) {
    		
    		// determine which packages will already be loaded
        	Set<String> alreadyLoaded = new HashSet<String>();
        	for (String d : visitedDirs) {
        		File dir2 = new File(d);
        		if (dir2.isDirectory()) {
                    File versionFile = new File(dir2 + "/version.xml");
                    if (versionFile.exists()) {
                        try {
                            // find name of package
                            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                            Document doc = factory.newDocumentBuilder().parse(versionFile);
                            Element packageElement = doc.getDocumentElement();
                            alreadyLoaded.add(packageElement.getAttribute("name"));
                        } catch (Exception e) {
                            // too bad, won't print out any info
                        }
                    }

        			alreadyLoaded.add(dir2.getName());
    	        	for (String f : dir2.list()) {
    	        		File dir3 = new File(f);
    	        		if (dir3.isDirectory()) {
    	                    versionFile = new File(dir3 + "/version.xml");
    	                    if (versionFile.exists()) {
    	                        try {
    	                            // find name of package
    	                            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    	                            Document doc = factory.newDocumentBuilder().parse(versionFile);
    	                            Element packageElement = doc.getDocumentElement();
    	                            alreadyLoaded.add(packageElement.getAttribute("name"));
    	                        } catch (Exception e) {
    	                            // too bad, won't print out any info
    	                        }
    	                    }
    	        		}
    	        	}    		
        		}
        	}
    		
        	for (String f : archiveDir.list()) {
        		File f2 = new File(dir + FILESEPARATOR + f);
        		if (f2.isDirectory()) {
        			// this may be a package directory -- pick the latest directory
        			String [] versionDirs = f2.list();
        			Arrays.sort(versionDirs,
        					new Comparator<String>() {
								@Override
								public int compare(String v1, String v2) {
			        				PackageVersion pv1 = new PackageVersion(v1);
			        				PackageVersion pv2 = new PackageVersion(v2);
			        				return (pv1.compareTo(pv2));
								}
        			});
        			int k = versionDirs.length - 1;
        			while (k >= 0) {
        				String versionDir = versionDirs[k];
        				File vDir = new File(f2.getPath() + FILESEPARATOR + versionDir);
        				if (vDir.exists() && new File(vDir.getPath() + FILESEPARATOR + "version.xml").exists()) {
        					// check it is not already loaded
        					if (!alreadyLoaded.contains(f)) {
        						dirs.add(vDir.getPath());
        					}
        					break;
        				}
        				k--;
        			}
        		}
    		}        		
    	}
        return dirs;
    } // getBeastDirectories

    
	public static void initialise() {
	    processDeleteList();
	
	    addInstalledPackages(packages);
	
	    processInstallList(packages);
	
	//    checkInstalledDependencies(packages);
	}

	/**
     * load external jars in beast directories *
     */
    public static void loadExternalJars() throws IOException {
    	if (externalJarsLoaded) {
    		return;
    	}
    	loadExternalJarsEvenIfAlreadyLoaded();
    }
    
    public static void loadExternalJarsEvenIfAlreadyLoaded() throws IOException {
    	
    	
    	Utils6.logToSplashScreen("PackageManager::processDeleteList");
        processDeleteList();

    	Utils6.logToSplashScreen("PackageManager::addInstalledPackages");
        addInstalledPackages(packages);

    	Utils6.logToSplashScreen("PackageManager::processInstallList");
        processInstallList(packages);

    	Utils6.logToSplashScreen("PackageManager::checkInstalledDependencies");
        checkInstalledDependencies(packages);

        // Log BEAST-related modules already in the boot layer:
        // core beast.* modules plus any module that requires one of them
        // (i.e. BEAST packages loaded via the IDE workspace).
        // In an IDE all modules are typically in the boot layer, so
        // nothing will be loaded from disk — this message explains why.
        ModuleLayer bootLayer = ModuleLayer.boot();
        Set<String> beastCoreNames = bootLayer.modules().stream()
            .map(Module::getName)
            .filter(n -> n.startsWith("beast."))
            .collect(Collectors.toSet());
        List<String> bootBeastModules = bootLayer.modules().stream()
            .filter(m -> beastCoreNames.contains(m.getName())
                || m.getDescriptor().requires().stream()
                    .anyMatch(r -> beastCoreNames.contains(r.name())))
            .map(Module::getName)
            .sorted()
            .toList();
        if (!bootBeastModules.isEmpty()) {
            System.err.println("BEAST modules in boot layer: " + bootBeastModules);
        }

        // Load external package JARs into JPMS ModuleLayers.
        // Maven packages are loaded first, then legacy ZIP packages.
        // Because createAndRegisterModuleLayer() skips modules already
        // present in the boot layer or a previously registered plugin
        // layer, Maven packages take precedence over ZIP packages when
        // both provide the same JPMS module.  This is the desired
        // behaviour: Maven is the recommended distribution format going
        // forward, so a newer Maven version should shadow a legacy ZIP.
        Utils6.logToSplashScreen("PackageManager::loadMavenPackages");
        loadMavenPackages();

        // Load legacy ZIP packages (from ~/.beast/2.8/ and other dirs).
        // These are loaded after Maven packages, so a Maven package with
        // the same module name will take precedence.
        for (String jarDirName : getBeastDirectories()) {
        	loadPackage(jarDirName);
        }

        externalJarsLoaded = true;
    	Utils6.logToSplashScreen("PackageManager::Done");
    } // loadExternalJars
    
//	private static void findDataTypes() {
//		try {
//			Method findDataTypes = BEASTClassLoader.forName("beast.base.evolution.alignment.Alignment").getMethod("findDataTypes");
//			findDataTypes.invoke(null);
//		} catch (Exception e) {
//			// too bad, cannot load data types
//			System.err.print(e.getMessage());
//		}
//	}

	public static void loadExternalJars(String packagesString) throws IOException {
        processDeleteList();

        addInstalledPackages(packages);

        processInstallList(packages);

        if (packagesString != null && packagesString.trim().length() > 0) {
        	String unavailablePacakges = "";
        	String [] packageAndVersions = packagesString.split(":");
        	for (String s : packageAndVersions) {
        		s = s.trim();
        		int i = s.lastIndexOf(" ");
        		if (i > 0) {
        			String pkgname = s.substring(0, i);
        			String pkgversion = s.substring(i+1).trim().replaceAll("v", "");
        			Package pkg = new Package(pkgname);
        			PackageVersion version = new PackageVersion(pkgversion);
        	    	PackageInstaller.useArchive(true);
        			String dirName = getPackageDir(pkg, version, false, PackageManager.getBeastPackagePathProperty());
        			if (new File(dirName).exists()) {
        				loadPackage(dirName);
        			} else {
        				// check the latest installed version
        				Package pkg2 = packages.get(pkgname);
        				if (pkg2 == null || !pkg2.isInstalled() || !pkg2.getInstalledVersion().equals(version)) {
            				unavailablePacakges += s +", ";
        				} else {
	            	    	PackageInstaller.useArchive(false);
	            			dirName = getPackageDir(pkg, version, false, PackageManager.getBeastPackagePathProperty());
	            			if (new File(dirName).exists()) {
	            				loadPackage(dirName);
	            			} else {
	            				unavailablePacakges += s +", ";
	            			}
        				}
        			}
        		}
        	}
        	if (unavailablePacakges.length() > 1) {
        		unavailablePacakges = unavailablePacakges.substring(0, unavailablePacakges.length() - 2);
        		if (unavailablePacakges.contains(",")) {
        			System.err.println("The following packages are required, but not available: " + unavailablePacakges);
        		} else {
        			System.err.println("The following package is required, but is not available: " + unavailablePacakges);
        		}
        		System.err.println("See http://beast2.org/managing-packages/ for details on how to install packages.");
        	}
        }
        
        externalJarsLoaded = true;
    } // loadExternalJars

	private static void loadPackage(String jarDirName) {
        try {
            File versionFile = new File(jarDirName + "/version.xml");
            String packageName = null;
            String packageVersion = null;
            Map<String,Set<String>> services = null;
            if (versionFile.exists()) {
                try {
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    Document doc = factory.newDocumentBuilder().parse(versionFile);
                    Element packageElement = doc.getDocumentElement();
                    packageName = packageElement.getAttribute("name");
                    packageVersion = packageElement.getAttribute("version");
                    services = parseServices(doc);
                } catch (Exception e) {
                    // File is called version.xml, but is not a Beast2 version file
                }
            }

            // Collect JARs from lib/
            File jarDir = new File(jarDirName + "/lib");
            if (!jarDir.exists()) {
                jarDir = new File(jarDirName + "\\lib");
            }
            List<Path> jarPaths = new ArrayList<>();
            if (jarDir.exists() && jarDir.isDirectory()) {
                for (String fileName : jarDir.list()) {
                    if (fileName.endsWith(".jar")) {
                        jarPaths.add(Path.of(jarDir.getAbsolutePath(), fileName));
                    }
                }
            }

            if (!jarPaths.isEmpty()) {
                createAndRegisterModuleLayer(jarPaths, services, packageName, packageVersion, jarDirName);
            } else if (services != null && packageName != null) {
                // No JARs but services found (e.g. running from IDE)
                BEASTClassLoader.classLoader.addServices(packageName, services);
            }
        } catch (Exception e) {
            System.err.println("Skip loading of " + jarDirName + " : " + e.getMessage());
        }
	}

	/**
	 * Create a JPMS ModuleLayer from a list of JAR paths and register it
	 * with BEASTClassLoader.  Modules already present in the boot layer
	 * or previously loaded plugin layers are excluded to avoid
	 * "reads more than one module" errors.
	 *
	 * @param jarPaths       JARs to include in the new layer
	 * @param services       service map from version.xml (may be null)
	 * @param packageName    the BEAST package name (for fallback service registration)
	 * @param packageVersion the package version string (may be null)
	 * @param description    human-readable label used in warning messages
	 */
	public static void createAndRegisterModuleLayer(List<Path> jarPaths,
			Map<String, Set<String>> services, String packageName,
			String packageVersion, String description) {
		try {
			ModuleFinder finder = ModuleFinder.of(jarPaths.toArray(Path[]::new));
			ModuleLayer parent = ModuleLayer.boot();
			Set<String> availableModules = parent.modules().stream()
				.map(Module::getName)
				.collect(Collectors.toCollection(HashSet::new));
			// Also include modules from already-loaded plugin layers
			for (ModuleLayer layer : BEASTClassLoader.getPluginLayers()) {
				layer.modules().stream().map(Module::getName).forEach(availableModules::add);
			}

			// Collect candidate modules (not already loaded)
			Set<String> candidates = finder.findAll().stream()
				.map(ref -> ref.descriptor().name())
				.filter(name -> !availableModules.contains(name))
				.collect(Collectors.toSet());

			// Filter out modules whose requires can't be satisfied
			Set<String> resolvable = new LinkedHashSet<>();
			Set<String> skipped = new LinkedHashSet<>();
			for (String name : candidates) {
				var ref = finder.find(name).orElse(null);
				if (ref == null) continue;
				boolean satisfied = ref.descriptor().requires().stream()
					.filter(r -> !r.modifiers().contains(
						java.lang.module.ModuleDescriptor.Requires.Modifier.STATIC))
					.allMatch(r -> availableModules.contains(r.name())
						|| candidates.contains(r.name())
						|| r.name().equals("java.base"));
				if (satisfied) {
					resolvable.add(name);
				} else {
					skipped.add(name);
				}
			}

			if (!skipped.isEmpty()) {
				System.err.println("Skipping modules with unsatisfied dependencies in " +
					description + ": " + skipped);
			}

			if (resolvable.isEmpty()) {
				if (services != null && packageName != null) {
					BEASTClassLoader.classLoader.addServices(packageName, services);
				}
				return;
			}

			String packageNameAndVersion = packageName != null
				? packageName + (packageVersion != null ? " v" + packageVersion : "")
				: description;
			System.err.println("Loading package " + packageNameAndVersion + " from " + description);
			Utils6.logToSplashScreen("Loading package " + packageNameAndVersion);

			// Re-create finder from only the resolvable JARs
			List<Path> resolvableJars = jarPaths.stream()
				.filter(p -> {
					var refs = ModuleFinder.of(p).findAll();
					return refs.stream().anyMatch(r -> resolvable.contains(r.descriptor().name()));
				})
				.toList();

			ModuleFinder filteredFinder = ModuleFinder.of(resolvableJars.toArray(Path[]::new));
			Configuration config = parent.configuration()
				.resolveAndBind(filteredFinder, ModuleFinder.of(), resolvable);
			ModuleLayer layer = parent.defineModulesWithOneLoader(
				config, ClassLoader.getSystemClassLoader());
			BEASTClassLoader.registerPluginLayer(layer, services);
		} catch (Exception e) {
			System.err.println("Warning: could not create ModuleLayer for " +
				description + ": " + e.getMessage());
			if (services != null && packageName != null) {
				BEASTClassLoader.classLoader.addServices(packageName, services);
			}
		}
	}

	// =========================================================================
	//  Maven package support
	// =========================================================================

	/** A Maven coordinate triple (groupId, artifactId, version). */
	public static class MavenCoordinate {
		public final String groupId, artifactId, version;

		public MavenCoordinate(String groupId, String artifactId, String version) {
			this.groupId = groupId;
			this.artifactId = artifactId;
			this.version = version;
		}

		@Override
		public String toString() {
			return groupId + ":" + artifactId + ":" + version;
		}
	}

	private static final String MAVEN_PACKAGES_FILE = "maven-packages.xml";

	/**
	 * Load the list of Maven package coordinates from
	 * {@code ~/.beast/2.8/maven-packages.xml}.
	 */
	public static List<MavenCoordinate> loadMavenPackageConfig() {
		List<MavenCoordinate> coords = new ArrayList<>();
		File configFile = new File(getPackageUserDir(), MAVEN_PACKAGES_FILE);
		if (!configFile.exists()) return coords;
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			Document doc = factory.newDocumentBuilder().parse(configFile);
			NodeList nodes = doc.getElementsByTagName("package");
			for (int i = 0; i < nodes.getLength(); i++) {
				Element el = (Element) nodes.item(i);
				String g = el.getAttribute("groupId");
				String a = el.getAttribute("artifactId");
				String v = el.getAttribute("version");
				if (!g.isEmpty() && !a.isEmpty() && !v.isEmpty()) {
					coords.add(new MavenCoordinate(g, a, v));
				}
			}
		} catch (Exception e) {
			System.err.println("Warning: could not read " + configFile + ": " + e.getMessage());
		}
		return coords;
	}

	/**
	 * Save the list of Maven package coordinates to
	 * {@code ~/.beast/2.8/maven-packages.xml}.
	 */
	public static void saveMavenPackageConfig(List<MavenCoordinate> coords) {
		File configFile = new File(getPackageUserDir(), MAVEN_PACKAGES_FILE);
		configFile.getParentFile().mkdirs();
		try (PrintWriter pw = new PrintWriter(new FileWriter(configFile))) {
			pw.println("<packages>");
			for (MavenCoordinate c : coords) {
				pw.println("    <package groupId=\"" + c.groupId
						+ "\" artifactId=\"" + c.artifactId
						+ "\" version=\"" + c.version + "\" />");
			}
			pw.println("</packages>");
		} catch (IOException e) {
			System.err.println("Warning: could not write " + configFile + ": " + e.getMessage());
		}
	}

	// ---- Maven repository management ----

	/**
	 * Load extra Maven repository URLs from
	 * {@code beauti.properties} (key {@code maven.repositories}).
	 * Maven Central is always included and need not be listed.
	 */
	public static List<String> getMavenRepositoryURLs() {
		List<String> urls = new ArrayList<>();
		String prop = Utils6.getBeautiProperty("maven.repositories");
		if (prop != null && !prop.isBlank()) {
			for (String s : prop.split(",")) {
				s = s.trim();
				if (!s.isEmpty()) urls.add(s);
			}
		}
		return urls;
	}

	/**
	 * Save extra Maven repository URLs to {@code beauti.properties}.
	 */
	public static void saveMavenRepositoryURLs(List<String> urls) {
		Utils6.saveBeautiProperty("maven.repositories",
				urls.isEmpty() ? null : String.join(",", urls));
	}

	private static MavenPackageResolver createMavenResolver() {
		try {
			Path localRepo = Path.of(getPackageUserDir(), "maven-repo");
			List<String> repoURLs = getMavenRepositoryURLs();
			List<org.eclipse.aether.repository.RemoteRepository> extras = new ArrayList<>();
			for (int i = 0; i < repoURLs.size(); i++) {
				extras.add(MavenPackageResolver.remoteRepository("extra-" + i, repoURLs.get(i)));
			}
			return new MavenPackageResolver(localRepo, extras);
		} catch (NoClassDefFoundError e) {
			throw new UnsupportedOperationException(
					"Maven resolver not available on the module path. " +
					"Maven package install/load requires maven-resolver-supplier-mvn4.", e);
		}
	}

	/**
	 * Load all Maven-installed packages at startup.  Each coordinate in
	 * {@code maven-packages.xml} is resolved, and its JARs are loaded into
	 * a new JPMS ModuleLayer.
	 */
	public static void loadMavenPackages() {
		List<MavenCoordinate> coords = loadMavenPackageConfig();
		if (coords.isEmpty()) return;

		MavenPackageResolver resolver;
		try {
			resolver = createMavenResolver();
		} catch (UnsupportedOperationException e) {
			System.err.println("Warning: " + e.getMessage());
			return;
		}

		for (MavenCoordinate coord : coords) {
			try {
				List<Path> jars = resolver.resolve(coord.groupId, coord.artifactId, coord.version);
				Map<String, Set<String>> services = parseServicesFromJar(jars, coord);
				createAndRegisterModuleLayer(jars, services, coord.artifactId, coord.version, coord.toString());
			} catch (Exception e) {
				System.err.println(" FAILED: " + e.getMessage());
			}
		}
	}

	/**
	 * Install a Maven package: resolve the coordinate, download JARs,
	 * persist the coordinate in {@code maven-packages.xml}, and load the
	 * package into the current runtime.
	 */
	public static void installMavenPackage(String groupId, String artifactId, String version)
			throws Exception {
		// 1. Resolve to verify the coordinate is valid and downloadable
		MavenPackageResolver resolver = createMavenResolver();
		List<Path> jars = resolver.resolve(groupId, artifactId, version);

		// 2. Add to maven-packages.xml
		List<MavenCoordinate> coords = loadMavenPackageConfig();
		coords.removeIf(c -> c.groupId.equals(groupId) && c.artifactId.equals(artifactId));
		coords.add(new MavenCoordinate(groupId, artifactId, version));
		saveMavenPackageConfig(coords);

		// 3. Load into current runtime
		Map<String, Set<String>> services = parseServicesFromJar(jars,
				new MavenCoordinate(groupId, artifactId, version));
		createAndRegisterModuleLayer(jars, services, artifactId, version, groupId + ":" + artifactId + ":" + version);
	}

	/**
	 * Uninstall a Maven package by removing its coordinate from
	 * {@code maven-packages.xml}.  The cached JARs in maven-repo are left
	 * in place (they serve as a cache and won't be loaded if not in config).
	 */
	public static void uninstallMavenPackage(String groupId, String artifactId) {
		List<MavenCoordinate> coords = loadMavenPackageConfig();
		coords.removeIf(c -> c.groupId.equals(groupId) && c.artifactId.equals(artifactId));
		saveMavenPackageConfig(coords);
	}

	/**
	 * Parse service declarations from the main artifact JAR.  Looks for
	 * {@code version.xml} at the JAR root or at
	 * {@code META-INF/beast/version.xml}, then delegates to
	 * {@link #parseServices(Document)}.
	 */
	public static Map<String, Set<String>> parseServicesFromJar(List<Path> jars, MavenCoordinate coord) {
		// Find the main artifact JAR (matches artifactId in filename)
		Path mainJar = null;
		for (Path p : jars) {
			String name = p.getFileName().toString();
			if (name.startsWith(coord.artifactId + "-")) {
				mainJar = p;
				break;
			}
		}
		if (mainJar == null && !jars.isEmpty()) {
			mainJar = jars.get(0);
		}
		if (mainJar == null) return null;

		try (JarFile jar = new JarFile(mainJar.toFile())) {
			// Try root version.xml first, then META-INF/beast/version.xml
			JarEntry entry = jar.getJarEntry("version.xml");
			if (entry == null) {
				entry = jar.getJarEntry("META-INF/beast/version.xml");
			}
			if (entry != null) {
				try (InputStream is = jar.getInputStream(entry)) {
					DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
					Document doc = factory.newDocumentBuilder().parse(new InputSource(is));
					return parseServices(doc);
				}
			}
		} catch (Exception e) {
			System.err.println("Warning: could not read services from " + mainJar + ": " + e.getMessage());
		}
		return null;
	}

	/**
	 * Retrieves map of service names to service classes from version.xml file of the form
	 * <service type="beast.base.evolution.datatype.DataType">
	 * 		<provider classname="beast.base.evolution.datatype.Aminoacid"/>
	 *		<provider classname="beast.base.evolution.datatype.Nucleotide"/>
	 * </service>
	 * @param doc org.w3.doc document containing version.xml file
	 * @return
	 */
    public static Map<String, Set<String>> parseServices(Document doc) {
		Map<String, Set<String>> serviceMap = new HashMap<>();

        // process "service" elements
		NodeList nodes = doc.getElementsByTagName("service");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element service = (Element) nodes.item(i);
            String type = service.getAttribute("type");
            Set<String> providers = new HashSet<>();

            NodeList content = service.getChildNodes();
            for (int j = 0; j < content.getLength(); j++) {
            	Node n = content.item(j);
            	if (n.getNodeType() == Node.ELEMENT_NODE) {
            		if (n.getNodeName().equals("provider")) {
            			String clazz = ((Element)n).getAttribute("classname");
            			providers.add(clazz);
            		} else {
            			System.err.println("Unrecognised element " + n.getNodeName() + " found. Expected 'provider'");
            		}
            	}
            }
            serviceMap.put(type, providers);
        }
        
        // process "packageapp" elements
        // package apps are services of type "has.main.method"
        Set<String> providers = new HashSet<>();
		nodes = doc.getElementsByTagName("packageapp");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element service = (Element) nodes.item(i);
			String clazz = service.getAttribute("class");
			providers.add(clazz);
		}
        if (providers.size() > 0) {
        	serviceMap.put("has.main.method", providers);
        }

		return serviceMap;
	}

    
	public static Set<String> listServices(String service) {
		Set<String> services = BEASTClassLoader.getServices().get(service);
		if (services == null) {
			try {
				loadExternalJars();
				BEASTClassLoader.initServices();
			} catch (IOException e) {
				// ignore
			}
		}
		services = BEASTClassLoader.getServices().get(service);
		// ServiceLoader fallback for JPMS
		if (services == null) {
			try {
				Class<?> serviceClass = BEASTClassLoader.forName(service);
				services = BEASTClassLoader.loadService(serviceClass);
			} catch (ClassNotFoundException e) {
				// ignore
			}
		}
		return services;
	}

	/**
     * Populate given map with versions of packages to install which satisfy dependencies
     * of those already present.
     *
     * @param packageMap database of installed and available packages
     * @param packagesToInstall map to populate with package versions requiring installation.
     * @throws DependencyResolutionException thrown when method fails to identify a consistent set of dependent packages
     */
    public static void populatePackagesToInstall(Map<String, Package> packageMap,
                                                 Map<Package, PackageVersion> packagesToInstall) throws DependencyResolver.DependencyResolutionException {
        new DependencyResolver().resolve(packageMap, packagesToInstall);
    }

    /**
     * Checks that dependencies of all installed packages are met.
     */
    private static void checkInstalledDependencies(Map<String, Package> packageMap) {
        new DependencyResolver().checkInstalled(packageMap);
    }

    public static void checkInstalledDependencies() {
    	checkInstalledDependencies(packages);
    }

    /**
     * Display a warning to console or as a dialog, depending
     * on whether a GUI exists.
     * 
     * @param string warning to display
     */
    static void warning(String string) {
        if (!java.awt.GraphicsEnvironment.isHeadless() && System.getProperty("no.beast.popup") == null) {
        	message(string +
                    "\nUnexpected behavior may follow!");
        }
        System.err.println("Unexpected behavior may follow!");
    }
    
    /**
     * Display a message to console or as a dialog, depending
     * on whether a GUI exists.
     * 
     * @param string message to display
     */
    static void message(String string) {
    	System.out.println(string);
        if (!java.awt.GraphicsEnvironment.isHeadless()) {
        	SwingUtilities.invokeLater(new Runnable() { 
        		@Override
        		public void run() {
        			JOptionPane.showMessageDialog(null, string);
        		}
        	});
//        	Alert a = new Alert(AlertType.NONE);
//        	a.setContentText(string);
//        	a.getButtonTypes().add(ButtonType.CLOSE);
//        	a.showAndWait();
        }
    }

    /**
     * Register a URL with the BEAST class loader and invalidate the class cache.
     *
     * @param u URL
     * @param packageName name of the package
     * @param services service map from version.xml
     */
    public static void addURL(URL u, String packageName, Map<String, Set<String>> services) throws IOException {
    	BEASTClassLoader.classLoader.addURL(u, packageName, services);
        all_classes = null;
    }


    private static void loadAllClasses() {
        if (!externalJarsLoaded) {
            try {
                loadExternalJars();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        all_classes = new ArrayList<String>();
        String pathSep = System.getProperty("path.separator");
        String classpath = System.getProperty("java.class.path");

        for (String path : classpath.split(pathSep)) {
            //System.err.println("loadallclasses " + path);
            path = path.replaceAll("%20", " ");
            File filepath = new File(path);

            if (filepath.isDirectory()) {
                addDirContent(filepath, filepath.getAbsolutePath().length());
            } else if (path.endsWith(".jar")) {

                JarFile jar = null;
                try {
                    jar = new JarFile(filepath);
                } catch (IOException e) {
                    System.err.println("WARNING: " + filepath + " could not be opened!");
                    continue;
                }

                for (Enumeration<JarEntry> entries = jar.entries(); entries.hasMoreElements(); ) {
                    JarEntry entry = entries.nextElement();
                    if (entry.getName().endsWith(".class")) {
                        all_classes.add(entry.getName());
                    }
                }
                try {
					jar.close();
				} catch (IOException e) {
                    System.err.println("WARNING: " + filepath + " could not be closed!");
				}
            } else if (path.endsWith(".class")) {
                all_classes.add(path);
            } else {
                System.err.println("Warning: corrupt classpath entry: " + path);
            }

        }

        String fileSep = System.getProperty("file.separator");
        if (fileSep.equals("\\")) {
            fileSep = "\\\\";
        }
        for (int i = 0; i < all_classes.size(); i++) {
            String str = all_classes.get(i);
            str = str.substring(0, str.length() - 6);
            str = str.replaceAll(fileSep, ".");
            if (str.startsWith(".")) {
                str = str.substring(1);
            }
            all_classes.set(i, str);
        }

    }

    private static void addDirContent(File dir, int len) {
    	try {
    	// No point in checking directories that cannot be read.
    	// Need check here since these potentially can cause exceptions
	    	if (dir.canRead()) {
		        for (File file : dir.listFiles()) {
		            if (file.isDirectory()) {
		                addDirContent(file, len);
		            } else {
		                if (file.getName().endsWith(".class")) {
		                    all_classes.add(file.getAbsolutePath().substring(len));
		                }
		            }
		        }
	    	}
    	} catch (Exception e) {
    		// ignore
    		// windows appears to throw exceptions on unaccessible directories
    	}
    }


    /**
     * Checks whether the "otherclass" is a subclass of the given "superclass".
     *
     * @param superclass the superclass to check against
     * @param otherclass this class is checked whether it is a subclass of the the
     *                   superclass
     * @return TRUE if "otherclass" is a true subclass
     */
    public static boolean isSubclass(Class<?> superclass, Class<?> otherclass) {
        Class<?> currentclass;
        boolean result;

        result = false;
        currentclass = otherclass;
        do {
            result = currentclass.equals(superclass);

            // topmost class reached?
            if (currentclass.equals(Object.class))
                break;

            if (!result)
                currentclass = currentclass.getSuperclass();
        } while (!result);

        return result;
    }


    /**
     * Checks whether the given class implements the given interface.
     *
     * @param intf the interface to look for in the given class
     * @param cls  the class to check for the interface
     * @return TRUE if the class contains the interface
     */
    public static boolean hasInterface(Class<?> intf, Class<?> cls) {
        Class<?>[] intfs;
        int i;
        boolean result;
        Class<?> currentclass;

        result = false;
        currentclass = cls;
        do {
            // check all the interfaces, this class implements
            intfs = currentclass.getInterfaces();
            for (i = 0; i < intfs.length; i++) {
                if (intfs[i].equals(intf)) {
                    result = true;
                    break;
                }
            }

            // get parent class
            if (!result) {
                currentclass = currentclass.getSuperclass();

                // topmost class reached or no superclass?
                if ((currentclass == null) || (currentclass.equals(Object.class)))
                    break;
            }
        } while (!result);

        return result;
    }


    /**
     * Checks the given packages for classes that inherited from the given
     * class, in case it's a class, or implement this class, in case it's an
     * interface.
     *
     * @param classname the class/interface to look for
     * @param pkgnames  the packages to search in
     * @return a list with all the found classnames
     */
    public static List<String> find(String classname, String[] pkgnames) {
        List<String> result;
        Class<?> cls;

        result = new ArrayList<String>();

        try {
            cls = BEASTClassLoader.forName(classname);
            result = find(cls, pkgnames);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Checks the given package for classes that inherited from the given class,
     * in case it's a class, or implement this class, in case it's an interface.
     *
     * @param classname the class/interface to look for
     * @param pkgname   the package to search in
     * @return a list with all the found classnames
     */
    public static List<String> find(String classname, String pkgname) {
        List<String> result;
        Class<?> cls;

        result = new ArrayList<String>();

        try {
            cls = BEASTClassLoader.forName(classname);
            result = find(cls, pkgname);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Checks the given packages for classes that inherited from the given
     * class, in case it's a class, or implement this class, in case it's an
     * interface.
     *
     * @param cls      the class/interface to look for
     * @param pkgnames the packages to search in
     * @return a list with all the found classnames
     */
    public static List<String> find(Class<?> cls, String[] pkgnames) {
        List<String> result;
        int i;
        HashSet<String> names;

        result = new ArrayList<String>();

        names = new HashSet<String>();
        for (i = 0; i < pkgnames.length; i++) {
            names.addAll(find(cls, pkgnames[i]));
        }

        // sort result
        result.addAll(names);
        Collections.sort(result, new Comparator<String>() {
			@Override
			public int compare(String s1, String s2) {
				return comparePackageNames(s1,s2);
			}
        }); //, new StringCompare());

        return result;
    }

    /**
     * Checks the given package for classes that inherited from the given class,
     * in case it's a class, or implement this class, in case it's an interface.
     *
     * @param cls     the class/interface to look for
     * @param pkgname the package to search in
     * @return a list with all the found classnames
     */
    public static List<String> find(Class<?> cls, String pkgname) {
        if (all_classes == null) {
            loadAllClasses();
        }

        List<String> result = new ArrayList<String>();
        for (int i = all_classes.size() - 1; i >= 0; i--) {
            String className = all_classes.get(i);
            if (className.indexOf('/') >= 0) {
            	className = className.replaceAll("/", ".");
            }
            //System.err.println(className + " " + pkgname);

            // must match package
            if (className.startsWith(pkgname)) {
                //System.err.println(className);
                try {
                    Class<?> clsNew = BEASTClassLoader.forName(className);

                    // no abstract classes
                    if (!Modifier.isAbstract(clsNew.getModifiers()) &&
                            // must implement interface
                            (cls.isInterface() && hasInterface(cls, clsNew)) ||
                            // must be derived from class
                            (!clsNew.isInterface() && isSubclass(cls, clsNew))) {
                        result.add(className);
                    }
                } catch (Throwable e) {
                    System.err.println("Checking class: " + className);
                    e.printStackTrace();
                }

            }
        }

        // sort result
        Collections.sort(result, new Comparator<String>() {
			@Override
			public int compare(String s1, String s2) {
	        	return comparePackageNames(s1, s2);
			}
        }); //, new StringCompare());
        // remove duplicates
        for (int i = result.size() - 1; i > 0; i--) {
            if (result.get(i).equals(result.get(i - 1))) {
                result.remove(i);
            }
        }

        return result;
    }

    /**
     * @param parent          The parent class that can be an interface.
     * @param includeParent   if true, then return the parent class itself
     * @return    a list of classes inherited from the parent class (interface).
     *            If those classes cannot be found in the class path,
     *            they will be ignored (ClassNotFoundException handled inside the method).
     */
    public static List<Class<?>> find(Class<?> parent, boolean includeParent) {

        if (all_classes == null) {
            loadAllClasses();
        }

        List<Class<?>> result = new ArrayList<Class<?>>();
        for (int i = all_classes.size() - 1; i >= 0; i--) {
            String className = all_classes.get(i);
            if (className.indexOf('/') >= 0) {
                className = className.replaceAll("/", ".");
            }
            //System.err.println(className);
            try {
                Class<?> cls = BEASTClassLoader.forName(className);

                if (parent.isAssignableFrom(cls)) {
                    if (includeParent || !cls.equals(parent)) {
                        result.add(cls);
                    }
                }
            } catch (ClassNotFoundException e) {
                System.err.println("Cannot find class: " + className);
//                    e.printStackTrace();
            }
        }

        return result;
    }


    /*
     * Command-line interface code
     */

    /**
     * Pretty-print package information.
     *
     * @param ps print stream to which to print package info
     * @param packageMap map from package names to package objects
     */
    public static void prettyPrintPackageInfo(PrintStream ps, Map<String, Package> packageMap) {
        PackageManagerCLI.prettyPrintPackageInfo(ps, packageMap);
    }

    public static void main(String[] args) {
        PackageManagerCLI.main(args);
    }

    /** Compare package names, putting BEAST.base in front, then alphabetical ignoring case. */
	public static int comparePackageNames(String s1, String s2) {
    	if (s1.equals(BEAST_BASE_PACKAGE_NAME)) {
    		if (s2.equals(BEAST_BASE_PACKAGE_NAME)) {
    			return 0;
    		}
    		return -1;
    	}
    	if (s2.equals(BEAST_BASE_PACKAGE_NAME)) {
    		return 1;
    	}
    	return s1.toLowerCase().compareTo(s2.toLowerCase());
	}

	/**  maps package name to a Package object, which contains info on whether 
     * and which version is installed. This is initialised when loadExternalJars()
     * is called, which happens at the start of BEAST, BEAUti and many utilities.
     */
    private static TreeMap<String, Package> packages = new TreeMap<String, Package>();
   
    /** test whether a package with given name and version is available.
     * @param pkgname
     * @param pkgversion ignored for now
     * @return
     */
    // RRB: may need to return PackageStatus instead of boolean, but not sure yet how to handle
    // the case where a newer package is installed, and the old one is not available yet.
    //public static enum PackageStatus {NOT_INSTALLED, INSTALLED, INSTALLED_VERSION_NOT_AVAILABLE};
    public static boolean isInstalled(String pkgname, String pkgversion) {
		if (!packages.containsKey(pkgname)) {
			return false;
		}
		return true;
//		Package pkg = packages.get(pkgname);
//		PackageVersion version = new PackageVersion(pkgversion);
//		if (pkg.isAvailable(version)) {
//			return false;
//		}
	}
    
    public static void updatePackages(UpdateStatus updateStatus, boolean useGUI) {
        PackageManagerCLI.updatePackages(updateStatus, useGUI);
    }


    public static String getBeastPackagePathProperty() {
    	if (System.getProperty("BEAST_PACKAGE_PATH") != null) {
    		return System.getProperty("BEAST_PACKAGE_PATH");
    	}
    	return System.getenv("BEAST_PACKAGE_PATH");
//    	if (System.getenv("BEAST_ADDON_PATH") != null) {
//    		return System.getenv("BEAST_ADDON_PATH");
//    	}    	
//    	return System.getenv("BEAST_ADDON_PATH");
    }
 }
