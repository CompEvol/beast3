package beast.pkgmgmt;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Handles installation and uninstallation of BEAST packages,
 * including zip extraction, file management, and restart-safe
 * delete/install lists for Windows jar locking.
 */
public class PackageInstaller {

    public static final String TO_DELETE_LIST_FILE = "toDeleteList";
    public static final String TO_INSTALL_LIST_FILE = "toInstallList";
    public static final String ARCHIVE_DIR = "archive";

    // Flag to indicate archive directory and version numbers in directories are required
    private static boolean useArchive = false;

    public static void useArchive(boolean use) {
        useArchive = use;
    }

    public static boolean isUsingArchive() {
        return useArchive;
    }

    /**
     * Looks through packages to be installed and uninstalls any that are already installed but
     * do not match the version that is to be installed.
     */
    public static void prepareForInstall(Map<Package, PackageVersion> packagesToInstall, boolean useAppDir, String customDir) throws IOException {
        if (useArchive) {
            return;
        }

        Map<Package, PackageVersion> ptiCopy = new HashMap<>(packagesToInstall);
        for (Map.Entry<Package, PackageVersion> entry : ptiCopy.entrySet()) {
            Package thisPkg = entry.getKey();
            PackageVersion thisPkgVersion = entry.getValue();

            if (thisPkg.isInstalled()) {
                if (thisPkg.getInstalledVersion().equals(thisPkgVersion))
                    packagesToInstall.remove(thisPkg);
                else
                    uninstallPackage(thisPkg, useAppDir, customDir);
            }
        }

        if (getToDeleteListFile().exists()) {
            try (PrintStream ps = new PrintStream(getToInstallListFile())) {
                for (Map.Entry<Package, PackageVersion> entry : packagesToInstall.entrySet()) {
                    ps.println(entry.getKey() + ":" + entry.getValue());
                }
            } catch (IOException ex) {
                System.out.println("Error writing to-install file: " + ex.getMessage() +
                        " Installation may not resume successfully after restart.");
            }
        }
    }

    /**
     * Download and install specified versions of packages.
     */
    public static Map<String, String> installPackages(Map<Package, PackageVersion> packagesToInstall, boolean useAppDir, String customDir) throws IOException {
        Map<String, String> dirList = new HashMap<>();

        for (Map.Entry<Package, PackageVersion> entry : packagesToInstall.entrySet()) {
            Package thisPkg = entry.getKey();
            PackageVersion thisPkgVersion = entry.getValue();

            URL templateURL = thisPkg.getVersionURL(thisPkgVersion);

            // check the URL exists
            HttpURLConnection huc = (HttpURLConnection) templateURL.openConnection();
            huc.setRequestMethod("HEAD");
            int responseCode = huc.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                throw new IOException("Could not find package at URL\n" + templateURL + "\n"
                        + "The server may be busy, or network may be down.\n"
                        + "If you suspect there is a problem with the URL \n"
                        + "(the URL may have a typo, or the file was removed)\n"
                        + "please contact the package maintainer.\n");
            }

            // create directory
            ReadableByteChannel rbc = Channels.newChannel(templateURL.openStream());
            String dirName = getPackageDir(thisPkg, thisPkgVersion, useAppDir, customDir);
            File dir = new File(dirName);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    throw new IOException("Could not create directory " + dirName);
                }
            }
            // grab file from URL
            String zipFile = dirName + "/" + thisPkg.getName() + ".zip";
            FileOutputStream fos = new FileOutputStream(zipFile);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

            // unzip archive
            doUnzip(zipFile, dirName);
            fos.close();

            // sanity check: does this package contain services that clash
            String nameSpaceCheck = null;
            try {
                nameSpaceCheck = hasNamespaceClash(thisPkg.getName(), dirName);
            } catch (SAXException | IOException | ParserConfigurationException e) {
                e.printStackTrace();
            }
            if (nameSpaceCheck != null) {
                deleteRecursively(dir, new ArrayList<>());
                throw new RuntimeException(nameSpaceCheck);
            }

            dirList.put(thisPkg.getName(), dirName);
        }

        Utils6.saveBeautiProperty("package.path", null);
        return dirList;
    }

    private static String hasNamespaceClash(String packageName, String dirName) throws SAXException, IOException, ParserConfigurationException {
        File versionFile = new File(dirName + "/version.xml");
        Map<String, Set<String>> services = null;
        if (versionFile.exists()) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            Document doc = factory.newDocumentBuilder().parse(versionFile);
            services = PackageManager.parseServices(doc);
        }

        if (services == null) {
            return null;
        }

        for (String service : services.keySet()) {
            Set<String> s = services.get(service);
            String existingNamespace = BEASTClassLoader.usesExistingNamespaces(s);
            if (existingNamespace != null) {
                return "Programmer error: One of the services (" + service + ") in package "
                        + packageName + " uses a namespace that is already in use: " + existingNamespace
                        + ". Package " + packageName + " is NOT loaded and will be removed";
            }
        }
        return null;
    }

    public static String getPackageDir(Package thisPkg, PackageVersion thisPkgVersion, boolean useAppDir, String customDir) {
        String dirName = (useAppDir ? PackageManager.getPackageSystemDir() : PackageManager.getPackageUserDir()) +
                (useArchive ? "/" + ARCHIVE_DIR : "") +
                "/" + thisPkg.getName() +
                (useArchive ? "/" + thisPkgVersion.versionString : "");
        if (customDir != null) {
            dirName = customDir +
                    (useArchive ? "/" + ARCHIVE_DIR : "") +
                    "/" + thisPkg.getName() +
                    (useArchive ? "/" + thisPkgVersion.versionString : "");
        }
        return dirName;
    }

    public static String uninstallPackage(Package pkg, boolean useAppDir, String customDir) throws IOException {
        return uninstallPackage(pkg, null, useAppDir, customDir);
    }

    public static String uninstallPackage(Package pkg, PackageVersion pkgVersion, boolean useAppDir, String customDir) throws IOException {
        if (pkgVersion == null) {
            pkgVersion = pkg.getInstalledVersion();
        }
        String dirName = getPackageDir(pkg, pkgVersion, useAppDir, customDir);
        File dir = new File(dirName);
        if (!dir.exists()) {
            useArchive = !useArchive;
            dirName = getPackageDir(pkg, pkgVersion, useAppDir, customDir);
            dir = new File(dirName);
            useArchive = !useArchive;
        }
        unloadPackage(dir);

        List<File> deleteFailed = new ArrayList<>();
        deleteRecursively(dir, deleteFailed);

        if (useArchive) {
            File parent = dir.getParentFile();
            if (parent.list().length == 0) {
                parent.delete();
            }
        }

        if (deleteFailed.size() > 0) {
            File toDeleteList = getToDeleteListFile();
            FileWriter outfile = new FileWriter(toDeleteList, true);
            for (File file : deleteFailed) {
                outfile.write(file.getAbsolutePath() + "\n");
            }
            outfile.close();
        }

        Utils6.saveBeautiProperty("package.path", null);
        return dirName;
    }

    private static void unloadPackage(File dir) {
        File versionFile = new File(dir.getPath() + "/version.xml");
        if (versionFile.exists()) {
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                Document doc = factory.newDocumentBuilder().parse(versionFile);
                Element packageElement = doc.getDocumentElement();
                String packageName = packageElement.getAttribute("name");
                Map<String, Set<String>> services = PackageManager.parseServices(doc);
                BEASTClassLoader.delService(services, packageName);
            } catch (NullPointerException e) {
                // map not initialised -- ignore
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static void deleteRecursively(File file, List<File> deleteFailed) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f : files) {
                deleteRecursively(f, deleteFailed);
            }
        }
        if (!file.delete()) {
            deleteFailed.add(file);
        }
    }

    /**
     * Unzip a zip archive, with Zip Slip protection.
     */
    public static void doUnzip(String inputZip, String destinationDirectory) throws IOException {
        int BUFFER = 2048;
        File sourceZipFile = new File(inputZip);
        File unzipDestinationDirectory = new File(destinationDirectory).getCanonicalFile();

        try (ZipFile zipFile = new ZipFile(sourceZipFile, ZipFile.OPEN_READ)) {
            Enumeration<?> zipFileEntries = zipFile.entries();

            while (zipFileEntries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();
                String currentEntry = entry.getName();

                File destFile = new File(unzipDestinationDirectory, currentEntry).getCanonicalFile();

                if (!destFile.toPath().startsWith(unzipDestinationDirectory.toPath())) {
                    throw new IOException("Zip entry outside target directory: " + currentEntry);
                }

                destFile.getParentFile().mkdirs();

                if (!entry.isDirectory()) {
                    byte[] data = new byte[BUFFER];
                    try (BufferedInputStream is = new BufferedInputStream(zipFile.getInputStream(entry));
                         BufferedOutputStream dest = new BufferedOutputStream(new FileOutputStream(destFile), BUFFER)) {
                        int currentByte;
                        while ((currentByte = is.read(data, 0, BUFFER)) != -1) {
                            dest.write(data, 0, currentByte);
                        }
                    }
                }
            }
        }
    }

    public static File getToDeleteListFile() {
        return new File(PackageManager.getPackageUserDir() + "/" + TO_DELETE_LIST_FILE);
    }

    public static File getToInstallListFile() {
        return new File(PackageManager.getPackageUserDir() + "/" + TO_INSTALL_LIST_FILE);
    }

    /**
     * Delete files that could not be deleted earlier due to jar locking.
     */
    public static void processDeleteList() {
        File toDeleteListFile = getToDeleteListFile();
        if (toDeleteListFile.exists()) {
            try {
                BufferedReader fin = new BufferedReader(new FileReader(toDeleteListFile));
                while (fin.ready()) {
                    String str = fin.readLine();
                    File file = new File(str);
                    file.delete();
                }
                fin.close();
                toDeleteListFile.delete();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Completes installation procedure if packages could not be upgraded due to
     * Windows preventing the deletion of jar files.
     */
    public static void processInstallList(Map<String, Package> packageMap) {
        File toInstallListFile = getToInstallListFile();
        if (toInstallListFile.exists()) {
            try {
                PackageManager.addAvailablePackages(packageMap);
            } catch (PackageRepository.PackageListRetrievalException e) {
                System.out.println("Failed to resume package installation due to package list retrieval error: " + e.getMessage());
                toInstallListFile.delete();
                return;
            }

            Map<Package, PackageVersion> packagesToInstall = new HashMap<>();
            try (BufferedReader fin = new BufferedReader(new FileReader(toInstallListFile))) {
                String line;
                while ((line = fin.readLine()) != null) {
                    String[] nameVerPair = line.split(":");
                    Package pkg = packageMap.get(nameVerPair[0]);
                    PackageVersion ver = new PackageVersion(nameVerPair[1]);
                    packagesToInstall.put(pkg, ver);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                installPackages(packagesToInstall, false, null);
            } catch (IOException e) {
                System.out.println("Failed to install packages due to I/O error: " + e.getMessage());
            }

            toInstallListFile.delete();
        }
    }
}
