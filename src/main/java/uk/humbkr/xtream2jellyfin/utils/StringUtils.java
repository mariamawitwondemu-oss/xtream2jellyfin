package uk.humbkr.xtream2jellyfin.common;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class StringUtils {

    public static int length(final CharSequence cs) {
        return cs == null ? 0 : cs.length();
    }

    public static boolean isBlank(CharSequence cs) {
        final int strLen = length(cs);
        if (strLen == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isNotBlank(CharSequence cs) {
        return !isBlank(cs);
    }

    /**
     * Gets the substring before the last occurrence of a separator.
     * The separator is not returned.
     *
     * @param str       the String to get a substring from, may be null
     * @param separator the String to search for, may be null
     * @return the substring before the last occurrence of the separator,
     * empty string if null String input
     */
    public static String substringBeforeLast(final String str, final String separator) {
        if (str == null || separator == null || str.isEmpty()) {
            return str != null ? str : "";
        }
        final int pos = str.lastIndexOf(separator);
        if (pos == -1) {
            return str;
        }
        return str.substring(0, pos);
    }

    /**
     * Gets the substring after the last occurrence of a separator.
     * The separator is not returned.
     *
     * @param str       the String to get a substring from, may be null
     * @param separator the String to search for, may be null
     * @return the substring after the last occurrence of the separator,
     * empty string if null String input
     */
    public static String substringAfterLast(final String str, final String separator) {
        if (str == null || separator == null || str.isEmpty()) {
            return str != null ? str : "";
        }
        final int pos = str.lastIndexOf(separator);
        if (pos == -1 || pos == str.length() - separator.length()) {
            return "";
        }
        return str.substring(pos + separator.length());
    }

}
