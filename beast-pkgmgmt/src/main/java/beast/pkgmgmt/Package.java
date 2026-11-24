package beast.pkgmgmt;


import java.net.URL;
import java.util.*;

/**
 * BEAUti Package managed by PackageManager.
 *
 * Add a new rule in issue 754:
 * Package manager should make project links compulsory.
 */
/**"BEAUti package managed by package manager, also named as add-on previously. " +
        "Project URL is compulsory.")
**/        
public class Package {
    protected String packageName, description;
    protected PackageVersion installedVersion;
    protected Set<PackageDependency> installedVersionDeps;
    /**
     * every version the package lists offer, ZIP or Maven (so also the latest version), with its
     * ZIP; the ZIP is null for a version the lists have on Maven only, see availableVersionMaven (#316)
     */
    protected TreeMap<PackageVersion, URL> availableVersionURLs;
    protected TreeMap<PackageVersion, Set<PackageDependency>> availableVersionDeps;

    protected URL projectURL;

    public Package(String name) {
        this.packageName = name;
        this.description = "";

        availableVersionURLs = new TreeMap<>();
        availableVersionDeps = new TreeMap<>();
    }

    public String getName() {
        return packageName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public URL getProjectURL() {
        return projectURL;
    }

    public void setProjectURL(URL url) {
        this.projectURL = url;
    }


    /**
     * @return true if projectURL is valid
     */
    public boolean isValidFormat() {
        //https://www.geeksforgeeks.org/check-if-url-is-valid-or-not-in-java/
        try {
            getProjectURL().toURI();
            return true;
        }
        // If there was an Exception while creating URL object
        catch (Exception e) {
            return false;
        }
    }

    /**
     * @return true iff package is available online.
     */
    public boolean isAvailable() {
        return !availableVersionURLs.isEmpty();
    }

    /**
     * @param version version of package
     * @return true iff version of package is available
     */
    public boolean isAvailable(PackageVersion version) {
        return availableVersionURLs.containsKey(version);
    }

    public void addAvailableVersion(PackageVersion version, URL url, Set<PackageDependency> dependencies) {
        addAvailableVersion(version, url, dependencies, null);
    }

    /**
     * @param url         the ZIP of this version, or null if the package list has it only on Maven
     * @param mavenGroupArtifact "groupId:artifactId" if the package list says this version is
     *                    available from Maven, else null (#316)
     */
    public void addAvailableVersion(PackageVersion version, URL url, Set<PackageDependency> dependencies,
                                    String mavenGroupArtifact) {
        // a version listed as a ZIP in one package list and on Maven in another is available as both
        if (url != null || !availableVersionURLs.containsKey(version)) {
            availableVersionURLs.put(version, url);
        }
        availableVersionDeps.put(version, dependencies);
        if (mavenGroupArtifact != null) {
            availableVersionMaven.put(version, mavenGroupArtifact);
        }
    }

    /** "groupId:artifactId" of each available version that the package list says is on Maven (#316) */
    protected TreeMap<PackageVersion, String> availableVersionMaven = new TreeMap<>();

    /**
     * @return "groupId:artifactId:version" if the package list says {@code version} is available
     *         from Maven, else null (#316)
     */
    public String getAvailableMavenCoordinate(PackageVersion version) {
        String ga = availableVersionMaven.get(version);
        return ga == null ? null : ga + ":" + version.getVersion();
    }

    /**
     * True if {@code version} is installed from Maven rather than as a ZIP (#316): the package list
     * has the version on Maven, and either has no ZIP of it or the copy in use is from Maven, so
     * the package keeps its format. Otherwise the ZIP is installed, as before.
     */
    public boolean installsAsMaven(PackageVersion version) {
        return getAvailableMavenCoordinate(version) != null
                && (getVersionURL(version) == null || mavenCoordinate != null);
    }

    /**
     * the latest version as shown to users: "1.2.3", or "1.2.3 (Maven)" if it would be installed
     * from Maven; "" if none is available
     */
    public String getLatestLabel() {
        PackageVersion latest = getLatestVersion();
        if (latest == null) {
            return "";
        }
        return installsAsMaven(latest) ? latest + MAVEN_LABEL : latest.toString();
    }

    public void setInstalled(PackageVersion version, Set<PackageDependency> dependencies) {
        installedVersion = version;
        installedVersionDeps = dependencies;
    }

    public PackageVersion getInstalledVersion() {
        return installedVersion;
    }

    /** "groupId:artifactId:version" if the installed copy is from Maven, else null (#316) */
    protected String mavenCoordinate;

    public String getMavenCoordinate() {
        return mavenCoordinate;
    }

    public void setMavenCoordinate(String mavenCoordinate) {
        this.mavenCoordinate = mavenCoordinate;
    }

    /** the installed version as shown to users: "1.2.2", or "1.2.2 (Maven)" for a copy from Maven; "" if not installed */
    public String getInstalledLabel() {
        if (!isInstalled()) {
            return "";
        }
        return mavenCoordinate == null ? installedVersion.toString() : installedVersion + MAVEN_LABEL;
    }

    /** marks a version installed from Maven, here and in {@code beast -version} */
    static final String MAVEN_LABEL = " (Maven)";

    public boolean isInstalled() {
        return installedVersion != null;
    }

    /**
     * @return true if a newer version is available.
     */
    public boolean newVersionAvailable() {
        return isAvailable() &&
                (!isInstalled() || getLatestVersion().compareTo(getInstalledVersion()) > 0);
    }

    public Set<PackageDependency> getInstalledVersionDependencies() {
        return installedVersionDeps;
    }

    /**
     * @return latest available version of package.
     */
    public PackageVersion getLatestVersion() {
        return availableVersionURLs.isEmpty()
                ? null
                : availableVersionURLs.lastKey();
    }

    /**
     * @return URL corresponding to latest available version of package, null if that version is
     * only on Maven (#316).
     */
    public URL getLatestVersionURL() {
        return isAvailable()
                ? availableVersionURLs.lastEntry().getValue()
                : null;
    }

    /**
     * Retrieve URL corresponding to particular available version of package.
     *
     * @param version version of package
     * @return URL of its ZIP, null if the version is not available or only on Maven (#316)
     */
    public URL getVersionURL(PackageVersion version) {
        return isAvailable(version)
                ? availableVersionURLs.get(version)
                : null;
    }

    public Set<PackageDependency> getLatestVersionDependencies() {
        return isAvailable()
                ? availableVersionDeps.lastEntry().getValue()
                : null;
    }

    public Set<PackageDependency> getDependencies(PackageVersion version) {
        return availableVersionDeps.get(version);
    }

    public String getDependencies() {
        Set<PackageDependency> dependencies = getDependencies(getInstalledVersion());
        StringBuilder str = new StringBuilder();
        for (PackageDependency p: dependencies) {
        	str.append(p.dependencyName);
        	str.append(", ");
        }
        if (str.length() > 2) {
        	str.delete(str.length()-3, str.length()-1);
        }
        return str.toString();
    }

    /**
     * @return list of available package versions, sorted in order of decreasing version.
     */
    public List<PackageVersion> getAvailableVersions() {
        List<PackageVersion> versionList = new ArrayList<>(availableVersionURLs.keySet());
        Collections.sort(versionList);
        Collections.reverse(versionList);
        return versionList;
    }

    public boolean latestVersionDependsOn(Package pkg) {

        if (!isAvailable())
            throw new IllegalStateException("Requested latest available version dependencies " +
                    "when there is no available version.");

        for (PackageDependency packageDependency : availableVersionDeps.lastEntry().getValue()) {
            if (packageDependency.dependencyName.equals(pkg.packageName))
                return true;
        }
        return false;
    }

    public boolean installedVersionDependsOn(Package pkg) {

        if (!isInstalled())
            throw new IllegalStateException("Requested installed version dependencies " +
                    "when there is no installed version.");

        for (PackageDependency packageDependency : installedVersionDeps) {
            if (packageDependency.dependencyName.equals(pkg.packageName))
                return true;
        }
        return false;
    }

    public String getDependenciesString() {

        if (availableVersionDeps.isEmpty())
            return "";

        String depString = "";
        for (PackageDependency packageDependency : availableVersionDeps.lastEntry().getValue()) {
            String s = packageDependency.dependencyName;
            if (!s.equalsIgnoreCase(PackageManager.BEAST_PACKAGE_NAME)) {
                depString +=  s + ", ";
            }
        }
        if (depString.length() > 2) {
            depString = depString.substring(0, depString.length() - 2);
        }
        return depString;
    }

    @Override
	public String toString() {
        return packageName;
    }

    public String toHTML() {
        String html = "<html>";
        html += "<h1>" + packageName + "</h1>";
        html += "<p>Installed version: " + (isInstalled() ? getInstalledVersion() : "NA") + "</p>";
        html += "<p>Latest version: " + (isAvailable() ? getLatestVersion() : "NA") + "</p>";
        html += "<p>" + description +"</p>";
        html += "</html>";
        return html;
    }
}
