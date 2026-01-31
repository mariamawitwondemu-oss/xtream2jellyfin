package uk.humbkr.xtream2jellyfin.config;

import lombok.Data;

@Data
public class JellyfinServer {

    private boolean enabled = true;

    private String protocol;

    private String hostname;

    private int port;

    private String token;

    private String moviesLibraryId;

    private String seriesLibraryId;

    public String getBaseUrl() {
        return protocol + "://" + hostname + ":" + port;
    }

}
