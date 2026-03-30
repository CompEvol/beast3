/*
 * File PackageManagerCLI.java
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

package beast.pkgmgmt;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * CLI entry point and update logic for the BEAST package manager.
 * Delegates all package operations to static methods in {@link PackageManager}.
 */
public class PackageManagerCLI {

    /** Package names shown as "Recommended" in the CLI listing. */
    private final static Set<String> RECOMMENDED_PACKAGES = new HashSet<>(Arrays.asList("ORC", "starbeast3", "CCD"));

    /**
     * Pretty-print package information.
     *
     * @param ps print stream to which to print package info
     * @param packageMap map from package names to package objects
     */
    public static void prettyPrintPackageInfo(PrintStream ps, Map<String, Package> packageMap) {

        // Define headers here - need to know lengths
        String nameHeader = "Name";
        String statusHeader = "Installed Version";
        String latestHeader = "Latest Version";
        String depsHeader = "Dependencies";
        String descriptionHeader = "Description";

        int maxNameWidth = nameHeader.length();
        int maxStatusWidth = statusHeader.length();
        int maxLatestWidth = latestHeader.length();
        int maxDepsWidth = depsHeader.length();

        // Assemble list of packages (excluding beast2), keeping track of maximum field widths
        List<Package> packageList = new ArrayList<Package>();
        for (Package pkg : packageMap.values()) {
//            if (pkg.getName().equals(BEAST_PACKAGE))
//                continue;

            packageList.add(pkg);

            maxNameWidth = Math.max(pkg.getName().length(), maxNameWidth);
            maxStatusWidth = Math.max(pkg.isInstalled() ? pkg.getInstalledVersion().toString().length() : 2, maxStatusWidth);
            maxLatestWidth = Math.max(maxLatestWidth, pkg.isAvailable()
                            ? pkg.getLatestVersion().toString().length()
                            :  Math.max(2, maxStatusWidth));
            maxDepsWidth = Math.max(pkg.getDependenciesString().length(), maxDepsWidth);
        }

        // Assemble format strings
        String nameFormat = "%-" + (maxNameWidth) + "s";
        String statusFormat = "%-" + (maxStatusWidth) + "s";
        String latestFormat = "%-" + (maxLatestWidth) + "s";
        String depsFormat = "%-" + (maxDepsWidth) + "s";
        String sep = " | ";

        // Print headers
        ps.printf(nameFormat, nameHeader); ps.print(sep);
        ps.printf(statusFormat, statusHeader); ps.print(sep);
        ps.printf(latestFormat, latestHeader); ps.print(sep);
        ps.printf(depsFormat, depsHeader); ps.print(sep);
        ps.printf("%s\n", descriptionHeader);

        // Add horizontal rule under header
        int totalWidth = maxNameWidth + maxStatusWidth
                + maxLatestWidth + maxDepsWidth
                + descriptionHeader.length() + 4*3;
        for (int i=0; i<totalWidth; i++)
            ps.print("-");
        ps.println();


        // Print formatted package information
        for (Package pkg : packageList) {
        	if (pkg.getName().equals(PackageManager.BEAST_BASE_PACKAGE_NAME)) {
        		ps.printf(nameFormat, pkg.getName()); ps.print(sep);
		        ps.printf(statusFormat, pkg.isInstalled() ? pkg.getInstalledVersion() : "NA"); ps.print(sep);
		        ps.printf(latestFormat, pkg.isAvailable() ? pkg.getLatestVersion() : "NA"); ps.print(sep);
		        ps.printf(depsFormat, pkg.getDependenciesString()); ps.print(sep);
		        ps.printf("%s\n", pkg.getDescription());
        	}
        }
        for (int i=0; i<totalWidth; i++)
            ps.print("-");
        ps.println();

        // Print formatted package information
        for (Package pkg : packageList) {
        	if (!pkg.getName().equals(PackageManager.BEAST_BASE_PACKAGE_NAME)) {
	            ps.printf(nameFormat, pkg.getName()); ps.print(sep);
	            ps.printf(statusFormat, pkg.isInstalled() ? pkg.getInstalledVersion() :
	            	(RECOMMENDED_PACKAGES.contains(pkg.getName()) ? "NA - Recommended": "NA")); ps.print(sep);
	            ps.printf(latestFormat, pkg.isAvailable() ? pkg.getLatestVersion() : "NA"); ps.print(sep);
	            ps.printf(depsFormat, pkg.getDependenciesString()); ps.print(sep);
	            ps.printf("%s\n", pkg.getDescription());
        	}
        }
    }


    private static void printUsageAndExit(Arguments arguments) {
        arguments.printUsage("packagemanager", "");
        System.out.println("\nExamples:");
        System.out.println("packagemanager -list");
        System.out.println("packagemanager -add SNAPP");
        System.out.println("packagemanager -useAppDir -add SNAPP");
        System.out.println("packagemanager -del SNAPP");
        System.out.println("packagemanager -addRepository URL");
        System.out.println("packagemanager -maven io.github.compevol:beast-morph-models:1.3.0");
        System.out.println("packagemanager -delMaven io.github.compevol:beast-morph-models");
        System.out.println("packagemanager -listMaven");
        System.out.println("packagemanager -addMavenRepository https://beast2.org/maven/");
        System.exit(0);
    }

    public static void main(String[] args) {
        try {
            Arguments arguments = new Arguments(
                    new Arguments.Option[]{
                            new Arguments.Option("list", "List available packages"),
                            new Arguments.StringOption("add", "NAME", "Install the <NAME> package"),
                            new Arguments.StringOption("del", "NAME", "Uninstall the <NAME> package"),
                            new Arguments.StringOption("version", "NAME", "Specify package version"),
                            new Arguments.Option("useAppDir", "Use application (system wide) installation directory. Note this requires writing rights to the application directory. If not specified, the user's BEAST directory will be used."),
                            new Arguments.StringOption("dir", "DIR", "Install/uninstall package in directory <DIR>. This overrides the useAppDir option"),
                            new Arguments.Option("help", "Show help"),
                            new Arguments.Option("update", "Check for updates, and ask to install if available"),
                            new Arguments.Option("updatenow", "Check for updates and install without asking"),
                            new Arguments.StringOption("addRepository", "URL", "Add an external repository URL"),
                            new Arguments.StringOption("delRepository", "URL", "Remove an external repository URL"),
                            new Arguments.Option("listRepositories", "List installed external repositories."),
                            new Arguments.StringOption("maven", "COORD", "Install a Maven package (groupId:artifactId:version)"),
                            new Arguments.StringOption("delMaven", "COORD", "Uninstall a Maven package (groupId:artifactId)"),
                            new Arguments.Option("listMaven", "List installed Maven packages"),
                            new Arguments.StringOption("addMavenRepository", "URL", "Add an extra Maven repository URL"),
                            new Arguments.StringOption("delMavenRepository", "URL", "Remove an extra Maven repository URL"),
                            new Arguments.Option("listMavenRepositories", "List configured Maven repositories"),
                    });
            try {
                arguments.parseArguments(args);
            } catch (Arguments.ArgumentException ae) {
                System.out.println();
                System.out.println(ae.getMessage());
                System.out.println();
                printUsageAndExit(arguments);
            }

            if (args.length == 0 || arguments.hasOption("help")) {
                printUsageAndExit(arguments);
            }

            if (arguments.hasOption("update")) {
            	PackageManager.updatePackages(PackageManager.UpdateStatus.AUTO_CHECK_AND_ASK, false);
            	return;
            }

            if (arguments.hasOption("updatenow")) {
            	PackageManager.updatePackages(PackageManager.UpdateStatus.AUTO_UPDATE, false);
            	return;
            }

            // ---- Maven package commands (independent of CBAN package list) ----

            if (arguments.hasOption("maven")) {
                String coord = arguments.getStringOption("maven");
                String[] parts = coord.split(":");
                if (parts.length != 3) {
                    System.err.println("Expected format groupId:artifactId:version, got: " + coord);
                    System.exit(1);
                }
                System.err.println("Installing Maven package " + coord + "...");
                PackageManager.installMavenPackage(parts[0], parts[1], parts[2]);
                System.out.println("Maven package " + coord + " installed successfully.");
                return;
            }

            if (arguments.hasOption("delMaven")) {
                String coord = arguments.getStringOption("delMaven");
                String[] parts = coord.split(":");
                if (parts.length < 2) {
                    System.err.println("Expected format groupId:artifactId, got: " + coord);
                    System.exit(1);
                }
                PackageManager.uninstallMavenPackage(parts[0], parts[1]);
                System.out.println("Maven package " + parts[0] + ":" + parts[1] + " uninstalled.");
                return;
            }

            if (arguments.hasOption("listMaven")) {
                List<PackageManager.MavenCoordinate> coords = PackageManager.loadMavenPackageConfig();
                if (coords.isEmpty()) {
                    System.out.println("No Maven packages installed.");
                } else {
                    System.out.println("Installed Maven packages:");
                    for (PackageManager.MavenCoordinate c : coords) {
                        System.out.println("  " + c);
                    }
                }
                return;
            }

            if (arguments.hasOption("addMavenRepository")) {
                String url = arguments.getStringOption("addMavenRepository");
                List<String> repos = PackageManager.getMavenRepositoryURLs();
                if (repos.contains(url)) {
                    System.err.println("Maven repository '" + url + "' is already configured.");
                } else {
                    repos.add(url);
                    PackageManager.saveMavenRepositoryURLs(repos);
                    System.out.println("Added Maven repository: " + url);
                }
                return;
            }

            if (arguments.hasOption("delMavenRepository")) {
                String url = arguments.getStringOption("delMavenRepository");
                List<String> repos = PackageManager.getMavenRepositoryURLs();
                if (repos.remove(url)) {
                    PackageManager.saveMavenRepositoryURLs(repos);
                    System.out.println("Removed Maven repository: " + url);
                } else {
                    System.err.println("Maven repository '" + url + "' not found.");
                }
                return;
            }

            if (arguments.hasOption("listMavenRepositories")) {
                List<String> repos = PackageManager.getMavenRepositoryURLs();
                System.out.println("Maven repositories:");
                System.out.println("  https://repo.maven.apache.org/maven2/ (Maven Central, always included)");
                for (String url : repos) {
                    System.out.println("  " + url);
                }
                return;
            }

            boolean useAppDir = arguments.hasOption("useAppDir");
            String customDir = arguments.getStringOption("dir");
            if (customDir != null) {
                String path = PackageManager.getBeastPackagePathProperty();
                System.setProperty("BEAST_PACKAGE_PATH", (path != null ? path + ":" : "") +customDir);
                System.setProperty("beast.user.package.dir", (path != null ? path + ":" : "") +customDir);
            }

            List<URL> urlList = PackageManager.getRepositoryURLs();
            System.err.println("Packages user path : " + PackageManager.getPackageUserDir());
            for (URL url : urlList) {
                System.err.println("Access URL : " + url);
            }
            System.err.print("Getting list of packages ...");
            Map<String, Package> packageMap = new TreeMap<String, Package>(new Comparator<String>() {
            	// String::compareToIgnoreCase
    			@Override
    			public int compare(String s1, String s2) {
    				return PackageManager.comparePackageNames(s1, s2);
    			}
            });
            try {
                PackageManager.addInstalledPackages(packageMap);
                PackageManager.addAvailablePackages(packageMap);
            } catch (PackageRepository.PackageListRetrievalException e) {
            	System.err.println(e.getMessage());
                if (e.getCause() instanceof IOException)
                    System.err.println(PackageManager.NO_CONNECTION_MESSAGE);
            	return;
            }
            System.err.println("Done!\n");

            if (arguments.hasOption("list")) {
                prettyPrintPackageInfo(System.out, packageMap);
            }

            if (arguments.hasOption("add")) {
                String name = arguments.getStringOption("add");
                boolean processed = false;
                for (Package aPackage : packageMap.values()) {
                    if (aPackage.packageName.equals(name)) {
                        processed = true;
                        if (!aPackage.isInstalled() || arguments.hasOption("version")) {
                            System.err.println("Determine packages to install");
                            Map<Package, PackageVersion> packagesToInstall = new HashMap<Package, PackageVersion>();
                            if (arguments.hasOption("version")) {
                            	String versionString = arguments.getStringOption("version");
                            	PackageVersion version = new PackageVersion(versionString);
                            	packagesToInstall.put(aPackage, version);
                            	PackageInstaller.useArchive(true);
                            } else {
                            	packagesToInstall.put(aPackage, aPackage.getLatestVersion());
                            }
                            try {
                                PackageManager.populatePackagesToInstall(packageMap, packagesToInstall);
                            } catch (PackageManager.DependencyResolutionException ex) {
                                System.err.println("Installation aborted: " + ex.getMessage());
                            }
                            System.err.println("Start installation");
                            PackageManager.prepareForInstall(packagesToInstall, useAppDir, customDir);
                            Map<String, String> dirs = PackageManager.installPackages(packagesToInstall, useAppDir, customDir);
                            for (String pkgName : dirs.keySet())
                                System.out.println("Package " + pkgName + " is installed in " + dirs.get(pkgName) + ".");
                        } else {
                            System.out.println("Installation aborted: " + name + " is already installed.");
                            System.exit(1);
                        }
                    }
                }
                if (!processed) {
                    System.out.println("Could not find package '" + name + "' (typo perhaps?)");
                }
            }

            if (arguments.hasOption("del")) {
                String name = arguments.getStringOption("del");
                boolean processed = false;
                for (Package aPackage : packageMap.values()) {
                    if (aPackage.packageName.equals(name)) {
                        processed = true;
                        if (arguments.hasOption("version")) {
                        	PackageInstaller.useArchive(true);
                        	String versionString = arguments.getStringOption("version");
                        	PackageVersion version = new PackageVersion(versionString);
                            String dir = PackageManager.uninstallPackage(aPackage, version, useAppDir, customDir);
                            System.out.println("Package " + name + " is uninstalled from " + dir + ".");
                        } else {
	                        if (aPackage.isInstalled()) {
	                            List<String> deps = PackageManager.getInstalledDependencyNames(aPackage, packageMap);
	                            if (deps.isEmpty()) {
	                                System.err.println("Start un-installation");
	                                String dir = PackageManager.uninstallPackage(aPackage, useAppDir, customDir);
	                                System.out.println("Package " + name + " is uninstalled from " + dir + ".");
	                            } else {
	                                System.out.println("Un-installation aborted: " + name + " is used by these other packages: " +
	                                        join(", ", deps) + ".");
	                                System.out.println("Remove these packages first.");
	                                System.exit(1);
	                            }
	                        } else {
	                            System.out.println("Un-installation aborted: " + name + " is not installed yet.");
	                            System.exit(1);
	                        }
                        }
                    }
                }
                if (!processed) {
                    System.out.println("Could not find package '" + name + "' (typo perhaps?)");
                }
            }

            if (arguments.hasOption("addRepository")) {
                String urlString = arguments.getStringOption("addRepository");
                if (urlString != null) {

                    URL repoURL;
                    try {
                        repoURL = new URI(urlString).toURL();

                        List<URL> urls = PackageManager.getRepositoryURLs();

                        if (!urls.contains(repoURL)) {
                            urls.add(repoURL);
                            PackageManager.saveRepositoryURLs(urls);
                            System.out.println("Successfully added repository URL '"
                                    + repoURL + "' to "
                                    + PackageManager.getPackageUserDir() + "/beauti.properties.");
                        } else {
                            System.err.println("Repository URL '" + repoURL + "' already in "
                                    + PackageManager.getPackageUserDir() + "/beauti.properties.");
                        }
                    } catch (MalformedURLException ex) {
                        System.err.println("Error: malformed repository URL.");
                        System.exit(1);
                    }

                }

            }

            if (arguments.hasOption("delRepository")) {
                String urlString = arguments.getStringOption("delRepository");
                if (urlString != null) {

                    URL repoURL;

                    try {
                        repoURL = new URI(urlString).toURL();

                        List<URL> urls = PackageManager.getRepositoryURLs();

                        int urlIdx = urls.indexOf(repoURL);
                        if (urlIdx < 0) {
                            System.err.println("Repository URL '" + repoURL + "' not found in "
                                    + PackageManager.getPackageUserDir() + "/beauti.properties.");
                        } else if (urlIdx == 0) {
                            System.err.println("Cannot remove main repository, '" + repoURL + "'.");
                        } else {
                            urls.remove(repoURL);
                            PackageManager.saveRepositoryURLs(urls);
                            System.out.println("Successfully removed repository URL '"
                                    + repoURL + "' from "
                                    + PackageManager.getPackageUserDir() + "/beauti.properties.");
                        }
                    } catch(MalformedURLException ex){
                            System.err.println("Error: malformed repository URL.");
                            System.exit(1);
                    }
                }
            }

            if (arguments.hasOption("listRepositories")) {

                List<URL> urls = PackageManager.getRepositoryURLs();

                System.out.println("Installed repository URLs:");
                System.out.println(urls.remove(0) + " (main repository, unchangeable)");
                for (URL url : urls)
                    System.out.println(url);

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String join(String string, List<String> deps) {
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < deps.size(); i++) {
			buf.append(deps.get(i));
			buf.append(',');
		}
		buf.deleteCharAt(buf.length() - 1);
		return buf.toString();
	}
}
