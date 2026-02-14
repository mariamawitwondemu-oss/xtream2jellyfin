package uk.humbkr.xtream2jellyfin.utils;

import java.util.Map;

/**
 * Lightweight variable substitutor for ${key} placeholders.
 * Replaces placeholders with values from a map.
 */
public final class StringSubstitutor {

    private final Map<String, String> values;

    public StringSubstitutor(Map<String, String> values) {
        this.values = values != null ? values : Map.of();
    }

    /**
     * Replace all ${key} placeholders in the template with corresponding values.
     *
     * @param template the template string containing ${key} placeholders
     * @return the template with all placeholders replaced
     */
    public String replace(String template) {
        if (template == null || template.isEmpty()) {
            return template;
        }

        String result = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue() : "";
            result = result.replace(placeholder, value);
        }
        return result;
    }

}
