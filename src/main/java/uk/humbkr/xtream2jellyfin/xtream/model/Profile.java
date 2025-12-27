package uk.humbkr.xtream2jellyfin.xtream.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Profile {

    @JsonProperty("user_info")
    private UserInfo userInfo;

    @JsonProperty("server_info")
    private ServerInfo serverInfo;

}
