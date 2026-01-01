package uk.humbkr.xtream2jellyfin.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class LoggingConfig {

    @JsonProperty("level")
    private String level = "INFO";

    @JsonProperty("loggers")
    private Map<String, String> loggers = new HashMap<>();

}
