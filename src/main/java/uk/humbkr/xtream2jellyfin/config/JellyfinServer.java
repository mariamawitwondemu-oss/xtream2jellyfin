package uk.humbkr.xtream2jellyfin.config;

import lombok.Data;

@Data
public class JellyfinServer {

    private String protocol;

    private String hostname;

    private int port;

    private String token;

}
