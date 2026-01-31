package uk.humbkr.xtream2jellyfin.jellyfin;

import lombok.extern.slf4j.Slf4j;
import uk.humbkr.xtream2jellyfin.config.JellyfinServer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Slf4j
public class JellyfinClient {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final JellyfinServer config;

    private final HttpClient httpClient;

    public JellyfinClient(JellyfinServer config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .build();
    }

    public void refreshLibrary(String libraryId, String libraryName) {
        if (libraryId == null || libraryId.isBlank()) {
            log.debug("No library ID configured for {}, skipping refresh", libraryName);
            return;
        }

        String url = config.getBaseUrl() + "/Items/" + libraryId + "/Refresh"
                + "?Recursive=true"
                + "&ImageRefreshMode=Default"
                + "&MetadataRefreshMode=Default"
                + "&ReplaceAllImages=false"
                + "&ReplaceAllMetadata=false";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "MediaBrowser Token=" + config.getToken())
                    .timeout(REQUEST_TIMEOUT)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Jellyfin {} library refresh triggered successfully", libraryName);
            } else {
                log.warn("Jellyfin {} library refresh failed with status {}: {}",
                        libraryName, response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.warn("Failed to trigger Jellyfin {} library refresh: {}", libraryName, e.getMessage());
        }
    }

    public void refreshMoviesLibrary() {
        refreshLibrary(config.getMoviesLibraryId(), "movies");
    }

    public void refreshSeriesLibrary() {
        refreshLibrary(config.getSeriesLibraryId(), "series");
    }

}
