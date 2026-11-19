package beast.base.core;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Deprecation warnings for a tensor input given a value whose domain does not fit the
 * declared one, e.g. a parameter with domain Real for an input declared PositiveReal (#226).
 * Such a model is accepted for now; it will be an error in a future version of BEAST.
 * <p>
 * Inputs are validated more than once, e.g. in BEAUti, so a warning is logged once for each
 * object and input while the mismatch lasts. Two objects with the same mismatch, e.g. two
 * Gamma priors without IDs, are each warned about. A mismatch that is fixed and comes back
 * is warned about again. Objects are held weakly, so a closed model is forgotten.
 */
public final class DomainMismatchWarnings {

    /** owner -> input name -> warning logged for it */
    private static final Map<BEASTInterface, Map<String, String>> WARNED = new WeakHashMap<>();

    private DomainMismatchWarnings() {
    }

    /**
     * Logs the domain mismatch of an input of {@code owner}, unless it was logged before for
     * this object and input; a null mismatch means the input fits, so a later mismatch is
     * logged again.
     */
    static synchronized void report(BEASTInterface owner, Input<?> input, String mismatch) {
        if (mismatch == null) {
            Map<String, String> byInput = WARNED.get(owner);
            if (byInput != null) {
                byInput.remove(input.getName());
            }
            return;
        }
        Map<String, String> byInput = WARNED.computeIfAbsent(owner, o -> new HashMap<>());
        if (!mismatch.equals(byInput.put(input.getName(), mismatch))) {
            Log.warning.println("WARNING: " + mismatch);
        }
    }

    /** @return whether this warning has been logged and its mismatch is still there */
    public static synchronized boolean warned(String message) {
        for (Map<String, String> byInput : WARNED.values()) {
            if (byInput.containsValue(message)) {
                return true;
            }
        }
        return false;
    }

    /** forgets the warnings logged so far, so they are logged again; for tests */
    public static synchronized void reset() {
        WARNED.clear();
    }
}
