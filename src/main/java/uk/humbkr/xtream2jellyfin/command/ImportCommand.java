package uk.humbkr.xtream2jellyfin.command;

import lombok.extern.slf4j.Slf4j;
import uk.humbkr.xtream2jellyfin.config.AppSettings;
import uk.humbkr.xtream2jellyfin.config.MediaSettings;
import uk.humbkr.xtream2jellyfin.config.XtreamProviderConfig;
import uk.humbkr.xtream2jellyfin.xtream.XtreamProcessor;
import uk.humbkr.xtream2jellyfin.xtream.model.MediaType;

@Slf4j
public class ImportCommand {

    private final AppSettings appSettings;
    private final XtreamProviderConfig providerConfig;

    public ImportCommand(AppSettings appSettings, XtreamProviderConfig providerConfig) {
        this.appSettings = appSettings;
        this.providerConfig = providerConfig;
    }

    public void execute(MediaType mediaType) {
        overrideMediaSettings(mediaType);

        // Disable scheduling to run only once
        providerConfig.setScanIntervalMinutes(0);

        try (XtreamProcessor processor = new XtreamProcessor(appSettings, providerConfig)) {
            processor.run();
        }
    }

    private void overrideMediaSettings(MediaType mediaType) {
        // Disable all media types first
        disableIfPresent(providerConfig.getLiveSettings());
        disableIfPresent(providerConfig.getMoviesSettings());
        disableIfPresent(providerConfig.getSeriesSettings());

        // Enable only the requested media type, creating settings if null
        switch (mediaType) {
            case LIVE -> enableSettings(providerConfig.getLiveSettings(), providerConfig::setLiveSettings);
            case MOVIE -> enableSettings(providerConfig.getMoviesSettings(), providerConfig::setMoviesSettings);
            case SERIES -> enableSettings(providerConfig.getSeriesSettings(), providerConfig::setSeriesSettings);
        }
    }

    private void disableIfPresent(MediaSettings settings) {
        if (settings != null) {
            settings.setEnabled(false);
        }
    }

    private void enableSettings(MediaSettings settings, java.util.function.Consumer<MediaSettings> setter) {
        if (settings == null) {
            settings = new MediaSettings();
            setter.accept(settings);
        }
        settings.setEnabled(true);
    }
}
