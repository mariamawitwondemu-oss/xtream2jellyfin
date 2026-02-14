package uk.humbkr.xtream2jellyfin.nameformat;

import uk.humbkr.xtream2jellyfin.utils.StringSubstitutor;
import uk.humbkr.xtream2jellyfin.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class StreamNameFormat extends BaseNameFormat {

    private final String template;

    public StreamNameFormat(String template, Map<String, String> searchReplacePatterns) {
        super(searchReplacePatterns);
        this.template = template;
    }

    public String format(String streamName, StreamNameFormatContext context) {
        if (StringUtils.isBlank(streamName)) {
            return "";
        }

        // Phase 1: Apply user-configured regex patterns
        streamName = applyRegexPatterns(streamName);

        // Phase 2: Replace template placeholders
        streamName = applyTemplate(streamName, context);

        // Phase 3: Apply character sanitization
        return sanitize(streamName);
    }

    private String applyTemplate(String streamName, StreamNameFormatContext context) {
        if (StringUtils.isBlank(this.template)) {
            return streamName;
        }
        // Create placeholder value map with default empty strings
        // This keeps templates simple (no need for ${var:-} syntax) for end users
        Map<String, String> placeholderValues = new HashMap<>(4);
        placeholderValues.put("name", streamName);
        placeholderValues.put("year", "");
        placeholderValues.put("externalProviderId", "");
        placeholderValues.put("externalId", "");

        if (context != null) {
            putIfNotBlank(placeholderValues, "year", context.getYear());
            putIfNotBlank(placeholderValues, "externalProviderId", context.getExternalProviderId());
            putIfNotBlank(placeholderValues, "externalId", context.getExternalId());
        }

        // Use VariableSubstitutor to replace placeholders
        return new StringSubstitutor(placeholderValues).replace(template);
    }

    private void putIfNotBlank(Map<String, String> map, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            map.put(key, value);
        }
    }

}
