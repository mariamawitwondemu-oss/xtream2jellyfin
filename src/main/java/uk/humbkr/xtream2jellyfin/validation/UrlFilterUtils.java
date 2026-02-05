package uk.humbkr.xtream2jellyfin.validation;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.humbkr.xtream2jellyfin.common.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class UrlFilterUtils {

    /**
     * Extract domain from URL
     *
     * @param url The URL to extract domain from
     * @return Domain name, or null if URL is invalid
     */
    public static String extractDomain(String url) {
        if (StringUtils.isBlank(url)) {
            return null;
        }

        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host == null) {
                log.debug("Failed to extract domain from URL: {}", url);
                return null;
            }
            return host.toLowerCase();
        } catch (URISyntaxException e) {
            log.debug("Invalid URL format: {}", url);
            return null;
        }
    }

    /**
     * Check if URL format is valid
     *
     * @param url The URL to validate
     * @return true if URL format is valid, false otherwise
     */
    public static boolean isValidUrl(String url) {
        if (StringUtils.isBlank(url)) {
            return false;
        }

        try {
            URI uri = new URI(url);
            // URL must have both scheme and host
            return uri.getScheme() != null && uri.getHost() != null;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    /**
     * Check if URL points to an actual image file (not just a base CDN path).
     * Rejects incomplete URLs like "https://image.tmdb.org/t/p/w1280" that are missing
     * the image filename.
     */
    public static boolean isCompleteImageUrl(String url) {
        if (!isValidUrl(url)) {
            return false;
        }

        try {
            URI uri = new URI(url);
            String path = uri.getPath();
            if (path == null || path.isEmpty() || path.equals("/")) {
                return false;
            }

            // Last path segment must contain a dot (file extension)
            String lastSegment = path.substring(path.lastIndexOf('/') + 1);
            return lastSegment.contains(".");
        } catch (URISyntaxException e) {
            return false;
        }
    }

    /**
     * Combined validation: check URL format and domain validity
     *
     * @param url       The URL to validate
     * @param validator The domain validator to use
     * @return true if URL is valid and domain is valid, false otherwise
     */
    public static boolean isUrlWithValidDomain(String url, DomainValidator validator) {
        if (validator == null) {
            return true; // No validation, accept all URLs
        }

        if (!isValidUrl(url)) {
            return false;
        }

        return validator.isValidUrl(url);
    }
}
