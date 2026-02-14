package uk.humbkr.xtream2jellyfin.command;

import lombok.extern.slf4j.Slf4j;
import uk.humbkr.xtream2jellyfin.config.JellyfinServer;
import uk.humbkr.xtream2jellyfin.config.XtreamProviderConfig;
import uk.humbkr.xtream2jellyfin.jellyfin.JellyfinClient;
import uk.humbkr.xtream2jellyfin.xtream.model.MediaType;

@Slf4j
public class RefreshJellyfinCommand {

    private final XtreamProviderConfig providerConfig;

    public RefreshJellyfinCommand(XtreamProviderConfig providerConfig) {
        this.providerConfig = providerConfig;
    }

    public void execute(MediaType mediaType) {
        JellyfinServer libraryRefresh = providerConfig.getLibraryRefresh();
        if (libraryRefresh == null || !libraryRefresh.isEnabled()) {
            log.warn("Library refresh is not configured or disabled for this provider, skipping");
            return;
        }

        JellyfinClient client = new JellyfinClient(libraryRefresh);

        if (mediaType == null || mediaType == MediaType.MOVIE) {
            client.refreshMoviesLibrary();
        }
        if (mediaType == null || mediaType == MediaType.SERIES) {
            client.refreshSeriesLibrary();
        }
    }
}
