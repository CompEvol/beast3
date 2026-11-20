package beast.pkgmgmt;

/**
 * A version identifier for BEAST packages.
 * Natural ordering is Maven's ({@link beast.pkgmgmt.plan.Version}), the same for ZIP and
 * Maven packages, and consistent with equals: versions like "2.0" and "2" are equal.
 *
 * @author Tim Vaughan <tgvaughan@gmail.com>
 */
public class PackageVersion extends Version implements Comparable<PackageVersion> {

    String versionString;

    public PackageVersion(String versionString) {
        this.versionString = versionString;
    }

    @Override
    public String getVersion() {
        return versionString;
    }

    @Override
    public String getVersionString() {
        return versionString;
    }

    @Override
    public String getDateString() {
        return null;
    }

    @Override
    public String[] getCredits() {
        return null;
    }

    @Override
    public String toString() {
        return versionString;
    }

    @Override
    public int compareTo(PackageVersion otherVersion) {
        return order().compareTo(otherVersion.order());
    }

    /** ordered as Maven orders versions, as everywhere in the package manager */
    private beast.pkgmgmt.plan.Version order() {
        return new beast.pkgmgmt.plan.Version(versionString);
    }

    /**
     * Two PackageVersions are equal when {@code compareTo} returns 0,
     * so that "2.0" and "2" are treated as the same version everywhere.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return compareTo((PackageVersion) o) == 0;
    }

    @Override
    public int hashCode() {
        return order().hashCode();
    }
}
