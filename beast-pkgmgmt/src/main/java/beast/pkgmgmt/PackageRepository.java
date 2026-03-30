package beast.pkgmgmt;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Manages access to BEAST package repositories (CBAN).
 * Handles fetching available package lists from remote URLs and
 * persisting custom repository URLs.
 */
public class PackageRepository {

    public static final String PACKAGES_XML = "https://raw.githubusercontent.com/CompEvol/CBAN/master/packages" +
            BEASTVersion.INSTANCE.getMajorVersion() + ".xml";
    public static final String PACKAGES_XML_BACKUP = "https://bitbucket.org/rrb/cbanclone/raw/master/packages" +
            BEASTVersion.INSTANCE.getMajorVersion() + ".xml";

    /**
     * Exception thrown when reading a package repository fails.
     */
    public static class PackageListRetrievalException extends Exception {
        private static final long serialVersionUID = 1L;

        public PackageListRetrievalException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * @return URLs containing list of downloadable packages.
     * @throws MalformedURLException if a configured URL is invalid
     */
    public List<URL> getURLs() throws MalformedURLException {
        List<URL> URLs = new ArrayList<>();
        URLs.add(new URL(PACKAGES_XML));

        String urls = Utils6.getBeautiProperty("packages.url");
        if (urls != null) {
            for (String userURLString : urls.split(",")) {
                URLs.add(new URL(userURLString));
            }
        }
        return URLs;
    }

    /**
     * Write any third-party package repository URLs to the options file.
     *
     * @param urls List of URLs. The first is assumed to be the central
     *             package repository and is thus ignored.
     */
    public void saveURLs(List<URL> urls) {
        if (urls.size() < 1)
            return;

        if (urls.size() > 1) {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < urls.size(); i++) {
                if (i > 1)
                    sb.append(",");
                sb.append(urls.get(i));
            }
            Utils6.saveBeautiProperty("packages.url", sb.toString());
        } else {
            Utils6.saveBeautiProperty("packages.url", null);
        }
    }

    /**
     * Look through the packages defined in the XML files reached by the repository URLs
     * and add these packages to the package database.
     *
     * @param packageMap package database
     * @throws PackageListRetrievalException when one or more XMLs cannot be retrieved
     */
    public void addAvailablePackages(Map<String, Package> packageMap) throws PackageListRetrievalException {
        List<URL> urls;
        try {
            urls = getURLs();
        } catch (MalformedURLException e) {
            throw new PackageListRetrievalException("Error parsing one or more repository URLs.", e);
        }

        List<URL> brokenPackageRepositories = new ArrayList<>();
        Exception firstException = null;

        for (URL url : urls) {
            InputStream is = null;
            try {
                is = url.openStream();
                loadURL(url, is, packageMap);
            } catch (IOException e) {
                if (url.toString().equals(PACKAGES_XML)) {
                    URL urlBackup = null;
                    try {
                        urlBackup = new URL(PACKAGES_XML_BACKUP);
                        is = urlBackup.openStream();
                        loadURL(urlBackup, is, packageMap);
                    } catch (IOException | ParserConfigurationException | SAXException e2) {
                        if (brokenPackageRepositories.isEmpty())
                            firstException = e;
                        brokenPackageRepositories.add(urlBackup);
                    }
                } else {
                    if (brokenPackageRepositories.isEmpty())
                        firstException = e;
                    brokenPackageRepositories.add(url);
                }
            } catch (ParserConfigurationException | SAXException e) {
                if (brokenPackageRepositories.isEmpty())
                    firstException = e;
                brokenPackageRepositories.add(url);
            } finally {
                try {
                    if (is != null) is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (!brokenPackageRepositories.isEmpty()) {
            String message = "Error reading the following package repository URLs:";
            for (URL url : brokenPackageRepositories)
                message += " " + url;
            throw new PackageListRetrievalException(message, firstException);
        }
    }

    private void loadURL(URL url, InputStream is, Map<String, Package> packageMap) throws IOException, ParserConfigurationException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new InputSource(is));

        Element rootElement = document.getDocumentElement();
        NodeList nodes = rootElement.getChildNodes();

        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);

            if (node instanceof Element) {
                Element element = (Element) node;
                String packageName = element.getAttribute("name");
                Package pkg;
                if (packageMap.containsKey(packageName)) {
                    pkg = packageMap.get(packageName);
                } else {
                    pkg = new Package(packageName);
                }
                pkg.setDescription(element.getAttribute("description"));

                PackageVersion packageVersion = new PackageVersion(element.getAttribute("version"));

                if (element.hasAttribute("projectURL") &&
                        !(pkg.getLatestVersion() != null && packageVersion.compareTo(pkg.getLatestVersion()) < 0)) {
                    try {
                        pkg.setProjectURL(new URL(element.getAttribute("projectURL")));
                    } catch (MalformedURLException ex) {
                        System.err.println("Error parsing projectURL: " + ex.getMessage());
                    }
                }

                Set<PackageDependency> packageDependencies = new HashSet<>();
                NodeList depNodes = element.getElementsByTagName("depends");
                for (int j = 0; j < depNodes.getLength(); j++) {
                    Element dependson = (Element) depNodes.item(j);
                    String dependencyName = dependson.getAttribute("on");
                    String atLeastString = dependson.getAttribute("atleast");
                    String atMostString = dependson.getAttribute("atmost");
                    PackageDependency dependency = new PackageDependency(
                            dependencyName,
                            atLeastString.isEmpty() ? null : new PackageVersion(atLeastString),
                            atMostString.isEmpty() ? null : new PackageVersion(atMostString));
                    packageDependencies.add(dependency);
                }

                URL packageURL = new URL(element.getAttribute("url"));
                pkg.addAvailableVersion(packageVersion, packageURL, packageDependencies);

                // issue 754 Package manager should make project links compulsory
                if (pkg.isValidFormat()) {
                    packageMap.put(packageName, pkg);
                } else {
                    String urlStr = pkg.getProjectURL() == null ? "null" : pkg.getProjectURL().toString();
                    System.err.println("Warning: filter " + packageName + " from package manager " +
                            " because of invalid project URL " + urlStr + " !");
                }
            }
        }
        is.close();
    }
}
