package uk.humbkr.xtream2jellyfin.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import uk.humbkr.xtream2jellyfin.common.Constants;

import java.util.HashMap;
import java.util.Map;

@Data
public class XtreamProviderConfig {

    @JsonIgnore
    private String name;

    private String url;

    private String username;

    private String password;

    private int scanInterval = Constants.DEFAULT_SCAN_INTERVAL;

    @JsonProperty("category_name_cleanup_patterns")
    private Map<String, String> categoryNameCleanupPatterns = new HashMap<>();

    @JsonProperty("write_metadata_json")
    private boolean writeMetadataJson = false;

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
