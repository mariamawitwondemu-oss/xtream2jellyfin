package uk.humbkr.xtream2jellyfin.xtream.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ServerInfo {

    private String url;

    private String port;

    @JsonProperty("https_port")
    private String httpsPort;

    @JsonProperty("server_protocol")
    private String serverProtocol;

    @JsonProperty("rtmp_port")
    private String rtmpPort;

    private String timezone;

    @JsonProperty("timestamp_now")
    private Long timestampNow;

    @JsonProperty("time_now")
    private String timeNow;

    private String process;

}
