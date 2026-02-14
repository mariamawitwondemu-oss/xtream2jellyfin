package uk.humbkr.xtream2jellyfin.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class XtreamProviderConfig {

    private boolean enabled;

    @JsonIgnore
    private String name;

    private String url;

    private String username;

    private String password;

    @JsonProperty("scan_interval_minutes")
    private int scanIntervalMinutes = 360;

    @JsonProperty("category_name_cleanup_patterns")
    private Map<String, String> categoryNameCleanupPatterns = new HashMap<>();

    @JsonProperty("http_client")
    private HttpClientConfig httpClientConfig = new HttpClientConfig();

    private JellyfinServer libraryRefresh;

    private MediaSettings liveSettings;

    private MediaSettings moviesSettings;

    private MediaSettings seriesSettings;

    @JsonProperty("settings")
    public void setMediaSettings(Map<String, MediaSettings> settings) {
        this.liveSettings = settings.get("live");
        this.moviesSettings = settings.get("movies");
        this.seriesSettings = settings.get("series");
    }

}
