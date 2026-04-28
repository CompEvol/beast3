package beast.base.core;

import beast.pkgmgmt.BEASTVersion;

/**
 * Version information for the beast-base module.
 * <p>
 * Overrides {@link BEASTVersion} because beast-base and beast-pkgmgmt
 * can be versioned independently.
 * <p>
 * To cut a final release, set {@code PRERELEASE} to {@code null}.
 */
public class BEASTVersion2 extends BEASTVersion {

    /** Shadows {@link BEASTVersion#INSTANCE} so callers get the beast-base version. */
    public static final BEASTVersion2 INSTANCE = new BEASTVersion2();

    /**
     * Version string: assumed to be in format x.x.x
     */
    private static final String VERSION = "2.8.0";

    private static final String DATE_STRING = "2002-2026";

    /**
     * Pre-release label appended to the version string (e.g. "beta1", "rc1").
     * Set to {@code null} for a final release.
     */
    private static final String PRERELEASE = "beta5";

    @Override
	public String getVersion() {
        return VERSION;
    }

    @Override
    public boolean isPrerelease() {
        return PRERELEASE != null && !PRERELEASE.isEmpty();
    }

    @Override
    public String getPrereleaseDescription() {
        return PRERELEASE;
    }

    @Override
	public String getVersionString() {
        return "v" + VERSION + (isPrerelease() ? " " + PRERELEASE : "");
    }

    @Override
	public String getDateString() {
        return DATE_STRING;
    }
}
