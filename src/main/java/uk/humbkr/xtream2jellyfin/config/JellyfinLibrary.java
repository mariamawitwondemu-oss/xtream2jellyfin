package uk.humbkr.xtream2jellyfin.config;

import lombok.Data;

@Data
public class JellyfinLibrary {

    private final JellyfinServer jellyfinServer;

    private final String libraryName;

    private final boolean refreshEnabled;

}
