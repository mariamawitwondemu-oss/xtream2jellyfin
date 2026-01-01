package uk.humbkr.xtream2jellyfin.nameformat;

import uk.humbkr.xtream2jellyfin.common.StringUtils;

import java.util.Map;

public class CategoryNameFormat extends BaseNameFormat {

    public CategoryNameFormat(Map<String, String> regexPatterns) {
        super(regexPatterns);
    }

    public String format(String categoryName) {
        if (StringUtils.isBlank(categoryName)) {
            return "";
        }

        // Phase 1: Apply user-configured regex patterns
        categoryName = applyRegexPatterns(categoryName);

        // Phase 2: Apply Jellyfin character sanitization
        return sanitize(categoryName);
    }

}
