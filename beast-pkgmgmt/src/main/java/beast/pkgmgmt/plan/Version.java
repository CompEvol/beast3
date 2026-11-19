package beast.pkgmgmt.plan;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A package or library version, ordered as Maven orders versions (the rules of
 * {@code org.eclipse.aether.util.version.GenericVersionScheme}), so that ZIP packages, Maven
 * packages and libraries are all compared the same way. Implemented here rather than calling
 * the Maven resolver, so that planning at start-up needs no resolver jars.
 * <p>
 * The version is split into numbers and words at {@code .}, {@code -}, {@code _} and wherever
 * digits and letters meet. Numbers compare as numbers ({@code rc9 < rc10}); the words
 * {@code alpha < beta < milestone < rc = cr < snapshot < (release) = ga = final < sp} are
 * qualifiers ({@code a1}, {@code b1}, {@code m1} for short); other words compare
 * alphabetically, ignoring case, and come after a release. Trailing zeros do not count:
 * {@code 1 = 1.0 = 1.0.0}.
 */
public final class Version implements Comparable<Version> {

    private static final int KIND_QUALIFIER = 2, KIND_STRING = 3, KIND_INT = 4, KIND_BIGINT = 5;

    private static final Map<String, Integer> QUALIFIERS = Map.of(
            "alpha", -5, "beta", -4, "milestone", -3, "cr", -2, "rc", -2,
            "snapshot", -1, "ga", 0, "final", 0, "release", 0, "sp", 1);

    private record Item(int kind, Object value) {
        boolean isNumber() {
            return kind == KIND_INT || kind == KIND_BIGINT;
        }

        /** compared with a missing item: a number by its value, a qualifier by its rank, a word is more */
        int compareToPadding() {
            return switch (kind) {
                case KIND_INT -> Long.signum((Long) value);
                case KIND_QUALIFIER -> Integer.signum((Integer) value);
                default -> 1;
            };
        }

        int compareTo(Item that) {
            if (kind != that.kind) {
                return Integer.signum(kind - that.kind);
            }
            return switch (kind) {
                case KIND_INT -> Long.compare((Long) value, (Long) that.value);
                case KIND_BIGINT -> ((BigInteger) value).compareTo((BigInteger) that.value);
                case KIND_QUALIFIER -> Integer.compare((Integer) value, (Integer) that.value);
                default -> ((String) value).compareTo((String) that.value);
            };
        }
    }

    private final String text;
    private final List<Item> items;

    public Version(String text) {
        if (text == null) {
            throw new IllegalArgumentException("version is null");
        }
        this.text = text.trim();
        this.items = parse(this.text);
    }

    public static Version of(String text) {
        return new Version(text);
    }

    /** @return the first number, e.g. 2 for 2.8.0, or 0 if the version starts with a word */
    public long major() {
        Item first = items.isEmpty() ? null : items.get(0);
        if (first == null || !first.isNumber()) {
            return 0;
        }
        return first.kind == KIND_INT ? (Long) first.value : ((BigInteger) first.value).longValue();
    }

    /** @return the second number, e.g. 8 for 2.8.0, or 0 if there is none */
    public long minor() {
        Item second = items.size() < 2 ? null : items.get(1);
        if (second == null || !second.isNumber()) {
            return 0;
        }
        return second.kind == KIND_INT ? (Long) second.value : ((BigInteger) second.value).longValue();
    }

    @Override
    public int compareTo(Version that) {
        List<Item> these = items, those = that.items;
        boolean number = true;
        for (int i = 0; ; i++) {
            if (i >= these.size() && i >= those.size()) {
                return 0;
            } else if (i >= these.size()) {
                return -comparePadding(those, i, null);
            } else if (i >= those.size()) {
                return comparePadding(these, i, null);
            }
            Item a = these.get(i), b = those.get(i);
            if (a.isNumber() != b.isNumber()) {
                return number == a.isNumber() ? comparePadding(these, i, number) : -comparePadding(those, i, number);
            }
            int rel = a.compareTo(b);
            if (rel != 0) {
                return rel;
            }
            number = a.isNumber();
        }
    }

    private static int comparePadding(List<Item> items, int from, Boolean number) {
        int rel = 0;
        for (int i = from; i < items.size(); i++) {
            Item item = items.get(i);
            if (number != null && number != item.isNumber()) {
                break;
            }
            rel = item.compareToPadding();
            if (rel != 0) {
                break;
            }
        }
        return rel;
    }

    public boolean isAtLeast(Version other) {
        return compareTo(other) >= 0;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Version v && compareTo(v) == 0;
    }

    @Override
    public int hashCode() {
        return items.hashCode();
    }

    @Override
    public String toString() {
        return text;
    }

    // ---- parsing, as Maven's GenericVersion ----

    private static List<Item> parse(String version) {
        List<Item> items = new ArrayList<>();
        String v = version.isEmpty() ? "0" : version;
        int n = v.length(), index = 0;
        while (index < n) {
            int state = -2, start = index, end = n;
            boolean terminatedByNumber = false;
            for (; index < n; index++) {
                char c = v.charAt(index);
                if (c == '.' || c == '-' || c == '_') {
                    end = index;
                    index++;
                    break;
                }
                int digit = Character.digit(c, 10);
                if (digit >= 0) {
                    if (state == -1) {
                        end = index;
                        terminatedByNumber = true;
                        break;
                    }
                    if (state == 0) {
                        start++; // drop a leading zero
                    }
                    state = (state > 0 || digit > 0) ? 1 : 0;
                } else {
                    if (state >= 0) {
                        end = index;
                        break;
                    }
                    state = -1;
                }
            }
            String token;
            boolean isNumber;
            if (end - start > 0) {
                token = v.substring(start, end);
                isNumber = state >= 0;
            } else {
                token = "0";
                isNumber = true;
            }
            items.add(toItem(token, isNumber, terminatedByNumber));
        }
        trimPadding(items);
        return items;
    }

    private static Item toItem(String token, boolean isNumber, boolean terminatedByNumber) {
        if (isNumber) {
            return token.length() < 18
                    ? new Item(KIND_INT, Long.parseLong(token))
                    : new Item(KIND_BIGINT, new BigInteger(token));
        }
        if (terminatedByNumber && token.length() == 1) {
            switch (Character.toLowerCase(token.charAt(0))) {
                case 'a': return new Item(KIND_QUALIFIER, -5);
                case 'b': return new Item(KIND_QUALIFIER, -4);
                case 'm': return new Item(KIND_QUALIFIER, -3);
                default: break;
            }
        }
        String lower = token.toLowerCase(Locale.ENGLISH);
        Integer qualifier = QUALIFIERS.get(lower);
        return qualifier != null ? new Item(KIND_QUALIFIER, qualifier) : new Item(KIND_STRING, lower);
    }

    /** drops trailing items equal to padding, e.g. the zeros of 1.0.0 and the "ga" of 1.0-ga */
    private static void trimPadding(List<Item> items) {
        Boolean number = null;
        int end = items.size() - 1;
        for (int i = end; i > 0; i--) {
            Item item = items.get(i);
            if (!Boolean.valueOf(item.isNumber()).equals(number)) {
                end = i;
                number = item.isNumber();
            }
            if (end == i && (i == items.size() - 1 || items.get(i - 1).isNumber() == item.isNumber())
                    && item.compareToPadding() == 0) {
                items.remove(i);
                end--;
            }
        }
    }
}
