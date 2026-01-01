package uk.humbkr.xtream2jellyfin.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class HttpClientConfig {

    @JsonProperty("timeout_seconds")
    private int timeoutSeconds = 30;

    @JsonProperty("rate_limit")
    private String rateLimit = "100/s";

    @JsonProperty("retry_delay_seconds")
    private int retryDelaySeconds = 1;

    @JsonProperty("max_retries")
    private int maxRetries = 3;

}
