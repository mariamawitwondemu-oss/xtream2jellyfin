package uk.humbkr.xtream2jellyfin.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class AppSettings {

    @JsonProperty("file_manager_type")
    private String fileManagerType = "simple";

    @JsonProperty("media_dir")
    private String mediaDir = "media";

    @JsonProperty("cache_dir")
    private String cacheDir = "cache";

    @JsonProperty("write_metadata_nfo")
    private boolean writeMetadataNfo = true;

    @JsonProperty("logging")
    private LoggingConfig logging = new LoggingConfig();

    @JsonProperty("domain_validation_enabled")
    private boolean domainValidationEnabled = true;

    @JsonProperty("domain_validation_timeout_ms")
    private int domainValidationTimeout = 5000;

    @JsonProperty("domain_validation_failure_threshold")
    private int domainValidationFailureThreshold = 3;

    @JsonProperty("domain_validation_whitelist")
    private List<String> domainValidationWhitelist = List.of("image.tmdb.org");

}
