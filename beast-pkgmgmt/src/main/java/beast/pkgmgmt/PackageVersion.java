package beast.pkgmgmt;

import java.util.Arrays;

/**
 * A version identifier for BEAST packages.
 * Natural ordering is consistent with equals: versions like "2.0" and "2"
 * are considered equal by both compareTo and equals.
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
        return compareVersionStrings(this.versionString, otherVersion.versionString);
    }

    /**
     * Compare package version strings.
     *
     * @param versionA first version string
     * @param versionB second version string
     * @return -1, 0 or 1 if versionA is less than, equal to or greater than versionB
     */
    private int compareVersionStrings(String versionA, String versionB) {

        String[] partsA = versionA.split("\\.");
        String[] partsB = versionB.split("\\.");

        int n = Math.max(partsA.length, partsB.length);

        for (int i=0; i<n; i++) {
            int partAint, partBint;
            String partAstr, partBstr;

            if (i<partsA.length) {
                partAint = Integer.parseInt("0" + partsA[i].replaceAll("(^[0-9]*).*$", "$1"));
                partAstr = partsA[i].replaceAll("^[0-9]*(.*)$", "$1");
            } else {
                partAint = 0;
                partAstr = "";
            }

            if (i<partsB.length) {
                partBint = Integer.parseInt("0" + partsB[i].replaceAll("(^[0-9]*).*$", "$1"));
                partBstr = partsB[i].replaceAll("^[0-9]*(.*)$", "$1");
            } else {
                partBint = 0;
                partBstr = "";
            }

            if (partAint<partBint)
                return -1;

            if (partAint>partBint)
                return 1;

            // 1.0 > 1.0-alpha
            if (partAstr.isEmpty() && !partBstr.isEmpty())
                return 1;

            // 1.0-alpha < 1.0
            if (!partAstr.isEmpty() && partBstr.isEmpty())
                return -1;

            int strComp = partAstr.compareTo(partBstr);
            if (strComp != 0)
                return strComp;
        }

        return 0;
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

    /**
     * Hash code consistent with {@link #equals}: normalizes by splitting
     * on dots, stripping trailing zero components, and hashing the result.
     */
    @Override
    public int hashCode() {
        String[] parts = versionString.split("\\.");
        int last = parts.length;
        while (last > 1) {
            String part = parts[last - 1];
            int numeric = Integer.parseInt("0" + part.replaceAll("(^[0-9]*).*$", "$1"));
            String suffix = part.replaceAll("^[0-9]*(.*)$", "$1");
            if (numeric == 0 && suffix.isEmpty()) {
                last--;
            } else {
                break;
            }
        }
        return Arrays.hashCode(Arrays.copyOf(parts, last));
    }
}
