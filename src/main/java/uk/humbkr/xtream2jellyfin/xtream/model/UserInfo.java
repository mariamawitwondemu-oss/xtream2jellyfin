package uk.humbkr.xtream2jellyfin.xtream.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class UserInfo {

    private String username;

    private String password;

    private String message;

    private Integer auth;

    private String status;

    @JsonProperty("exp_date")
    private String expDate;

    @JsonProperty("is_trial")
    private String isTrial;

    @JsonProperty("active_cons")
    private String activeCons;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("max_connections")
    private String maxConnections;

    @JsonProperty("allowed_output_formats")
    private List<String> allowedOutputFormats;

}
