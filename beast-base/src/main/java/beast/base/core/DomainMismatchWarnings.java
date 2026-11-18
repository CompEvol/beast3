package beast.base.core;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Deprecation warnings for a tensor input given a value whose domain does not fit the
 * declared one, e.g. a parameter with domain Real for an input declared PositiveReal (#226).
 * Such a model is accepted for now; it will be an error in a future version of BEAST.
 * Each warning is logged once per run, since inputs are validated more than once, e.g. in
 * BEAUti.
 */
public final class DomainMismatchWarnings {

    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    private DomainMismatchWarnings() {
    }

    /** logs the warning, unless the same one was logged before */
    static void warn(String message) {
        if (WARNED.add(message)) {
            Log.warning.println("WARNING: " + message);
        }
    }

    /** @return whether this warning has been logged */
    public static boolean warned(String message) {
        return WARNED.contains(message);
    }

    /** forgets the warnings logged so far, so they are logged again; for tests */
    public static void reset() {
        WARNED.clear();
    }
}
