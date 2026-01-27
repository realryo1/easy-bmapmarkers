package dev.deimoslabs.easysignmarkers.helpers;

/**
 * Minimal semantic version comparator used to compare version strings like "1.2.3" or "1.2.3-alpha".
 * The comparator strips any suffix after a '-' character and compares numeric version parts.
 */
public class SemverComparator {

    /**
     * Compares two version strings numerically after stripping suffixes (anything after '-')
     * and splitting by dots.
     *
     * @param v1 first version string (e.g. "1.2.3")
     * @param v2 second version string
     * @return negative if v1 &lt; v2, 0 if equal, positive if v1 &gt; v2
     */
    public static int compare(String v1, String v2) {
        v1 = stripSuffix(v1);
        v2 = stripSuffix(v2);

        String[] a = v1.split("\\.");
        String[] b = v2.split("\\.");

        int max = Math.max(a.length, b.length);

        for (int i = 0; i < max; i++) {
            int x = i < a.length ? parseIntSafe(a[i]) : 0;
            int y = i < b.length ? parseIntSafe(b[i]) : 0;

            if (x != y) {
                return Integer.compare(x, y);
            }
        }
        return 0;
    }

    /**
     * Returns true if the latest version string represents a newer version than the current.
     *
     * @param latest candidate latest version
     * @param current current running version
     * @return true if latest &gt; current according to numeric comparison
     */
    public static boolean isNewer(String latest, String current) {
        return compare(latest, current) > 0;
    }

    private static int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return 0;
        }
    }

    private static String stripSuffix(String version) {
        int idx = version.indexOf('-');
        if (idx != -1) {
            return version.substring(0, idx);
        }
        return version;
    }
}
