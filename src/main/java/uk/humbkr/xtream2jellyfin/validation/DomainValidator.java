package uk.humbkr.xtream2jellyfin.validation;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import uk.humbkr.xtream2jellyfin.utils.JsonUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;

@Slf4j
public class DomainValidator {

    private static final String CACHE_FILENAME = "domain-cache.json";

    private final Path cacheFilePath;

    private final int timeoutMs;

    private final int failureThreshold;

    private final HttpClient httpClient;

    private final Set<String> whitelist = new HashSet<>();

    private final Map<String, DomainCacheEntry> domainCache = new HashMap<>();

    private final Set<String> blacklistedDomainsInCurrentRun = new HashSet<>();

    public DomainValidator(String cacheDir, int timeoutMs, int failureThreshold, List<String> whitelistDomains) {
        this.timeoutMs = timeoutMs;
        this.failureThreshold = failureThreshold;
        this.cacheFilePath = Paths.get(cacheDir, CACHE_FILENAME);

        // Initialize whitelist (case-insensitive)
        if (whitelistDomains != null && !whitelistDomains.isEmpty()) {
            whitelistDomains.stream()
                    .map(String::toLowerCase)
                    .forEach(whitelist::add);
            log.info("Domain whitelist configured with {} domains: {}", whitelist.size(), whitelist);
        }

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        ensureCacheDirectoryExists(cacheDir);
        loadCache();
    }

    /**
     * Validate URL by checking domain availability
     *
     * @param url The URL to validate
     * @return true if URL domain is valid, false otherwise
     */
    public synchronized boolean isValidUrl(String url) {
        String domain = UrlFilterUtils.extractDomain(url);
        if (domain == null) {
            log.debug("Cannot extract domain from URL: {}", url);
            return false;
        }

        // Check whitelist first (always valid, no HTTP check needed)
        if (whitelist.contains(domain)) {
            log.trace("Domain in whitelist, skipping validation: {}", domain);
            return true;
        }

        DomainCacheEntry entry = domainCache.get(domain);

        // If domain is already marked as invalid, return false immediately
        if (entry != null && "invalid".equals(entry.getStatus())) {
            blacklistedDomainsInCurrentRun.add(domain);
            return false;
        }

        // If domain is marked as valid, return true immediately
        if (entry != null && "valid".equals(entry.getStatus())) {
            return true;
        }

        // Domain is unknown or has fewer than threshold failures - validate it
        boolean isValid = validateDomainWithHttp(domain);

        if (!isValid) {
            blacklistedDomainsInCurrentRun.add(domain);
        }

        return isValid;
    }

    /**
     * Perform HTTP HEAD request to validate domain
     *
     * @param domain The domain to validate
     * @return true if domain responds successfully, false otherwise
     */
    private synchronized boolean validateDomainWithHttp(String domain) {
        DomainCacheEntry entry = domainCache.computeIfAbsent(domain, k -> new DomainCacheEntry());

        try {
            String url = "http://" + domain;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .timeout(Duration.ofMillis(timeoutMs))
                    .build();

            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());

            int statusCode = response.statusCode();
            boolean isSuccess = statusCode >= 200 && statusCode < 400;

            if (isSuccess) {
                // Domain responded successfully
                entry.setStatus("valid");
                entry.setFailureCount(0);
                entry.setLastChecked(System.currentTimeMillis());
                log.debug("Domain validated successfully: {}", domain);
                return true;
            } else {
                // HTTP error response
                entry.setFailureCount(entry.getFailureCount() + 1);
                entry.setLastChecked(System.currentTimeMillis());

                if (entry.getFailureCount() >= failureThreshold) {
                    entry.setStatus("invalid");
                    log.debug("Domain marked as invalid after {} failures: {}", failureThreshold, domain);
                } else {
                    entry.setStatus("unknown");
                    log.debug("Domain validation failed (attempt {}/{}): {}",
                            entry.getFailureCount(), failureThreshold, domain);
                }
                return false;
            }

        } catch (Exception e) {
            // Network error, timeout, or other exception
            entry.setFailureCount(entry.getFailureCount() + 1);
            entry.setLastChecked(System.currentTimeMillis());

            if (entry.getFailureCount() >= failureThreshold) {
                entry.setStatus("invalid");
                log.debug("Domain marked as invalid after {} failures: {} (error: {})",
                        failureThreshold, domain, e.getMessage());
            } else {
                entry.setStatus("unknown");
                log.debug("Domain validation failed (attempt {}/{}): {} (error: {})",
                        entry.getFailureCount(), failureThreshold, domain, e.getMessage());
            }
            return false;
        }
    }

    /**
     * Get set of domains that were blacklisted in the current run
     *
     * @return Set of blacklisted domain names
     */
    public Set<String> getBlacklistedDomains() {
        return new HashSet<>(blacklistedDomainsInCurrentRun);
    }

    /**
     * Reset blacklisted domains set for next run
     */
    public void reset() {
        blacklistedDomainsInCurrentRun.clear();
    }

    /**
     * Load domain cache from disk
     */
    private void loadCache() {
        if (!Files.exists(cacheFilePath)) {
            log.debug("Domain cache file does not exist, starting with empty cache");
            return;
        }

        try {
            String json = Files.readString(cacheFilePath);
            Map<String, DomainCacheEntry> loadedCache = JsonUtils.getJsonMapper()
                    .readValue(json, new TypeReference<Map<String, DomainCacheEntry>>() {
                    });

            domainCache.putAll(loadedCache);
            log.info("Loaded {} domain entries from cache", domainCache.size());
        } catch (IOException e) {
            log.warn("Failed to load domain cache, starting with empty cache: {}", e.getMessage());
        }
    }

    /**
     * Save domain cache to disk
     */
    public void saveCache() {
        try {
            String json = JsonUtils.getJsonMapper().writeValueAsString(domainCache);
            Files.writeString(cacheFilePath, json);
            log.debug("Saved {} domain entries to cache", domainCache.size());
        } catch (IOException e) {
            log.warn("Failed to save domain cache: {}", e.getMessage());
        }
    }

    /**
     * Ensure cache directory exists
     */
    private void ensureCacheDirectoryExists(String cacheDir) {
        try {
            Path cacheDirPath = Paths.get(cacheDir);
            if (!Files.exists(cacheDirPath)) {
                Files.createDirectories(cacheDirPath);
                log.debug("Created cache directory: {}", cacheDir);
            }
        } catch (IOException e) {
            log.warn("Failed to create cache directory: {}", e.getMessage());
        }
    }

    /**
     * Domain cache entry data class
     */
    @Data
    public static class DomainCacheEntry {

        private String status = "unknown"; // "valid", "invalid", "unknown"

        private int failureCount = 0;

        private long lastChecked = 0;

    }

}
