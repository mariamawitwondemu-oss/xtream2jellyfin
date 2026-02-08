package uk.humbkr.xtream2jellyfin.command;

import lombok.extern.slf4j.Slf4j;
import uk.humbkr.xtream2jellyfin.config.AppConfig;
import uk.humbkr.xtream2jellyfin.config.AppSettings;
import uk.humbkr.xtream2jellyfin.config.XtreamProviderConfig;
import uk.humbkr.xtream2jellyfin.xtream.model.MediaType;

import java.util.Map;

@Slf4j
public class CommandExecutor {

    private final AppConfig appConfig;

    public CommandExecutor(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    public void execute(String[] args) {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }

        String command = args[0];

        switch (command) {
            case "get-series-categories" -> listCategories(MediaType.SERIES);
            case "get-movies-categories" -> listCategories(MediaType.MOVIE);
            case "get-live-categories" -> listCategories(MediaType.LIVE);
            case "import-movies" -> importMedia(MediaType.MOVIE);
            case "import-series" -> importMedia(MediaType.SERIES);
            case "import-live" -> importMedia(MediaType.LIVE);
            default -> {
                System.err.println("Unknown command: " + command);
                printUsage();
                System.exit(1);
            }
        }
    }

    private void listCategories(MediaType mediaType) {
        if (appConfig.getProviders().isEmpty()) {
            log.error("No providers configured");
            System.err.println("Error: No providers found in configuration file");
            System.exit(1);
        }

        boolean foundEnabledProvider = false;
        for (Map.Entry<String, XtreamProviderConfig> entry : appConfig.getProviders().entrySet()) {
            String providerName = entry.getKey();
            XtreamProviderConfig providerConfig = entry.getValue();

            if (!providerConfig.isEnabled()) {
                log.debug("Skipping disabled provider: {}", providerName);
                continue;
            }

            if (!isMediaTypeEnabled(providerConfig, mediaType)) {
                log.debug("Skipping provider {} - {} is disabled", providerName, mediaType);
                continue;
            }

            foundEnabledProvider = true;
            System.out.println("\nProvider: " + providerName);
            ListCategoriesCommand command = new ListCategoriesCommand(providerConfig);
            command.execute(mediaType);
        }

        if (!foundEnabledProvider) {
            System.err.println("Error: No enabled providers found for " + mediaType + " categories");
            System.exit(1);
        }
    }

    private void importMedia(MediaType mediaType) {
        if (appConfig.getProviders().isEmpty()) {
            log.error("No providers configured");
            System.err.println("Error: No providers found in configuration file");
            System.exit(1);
        }

        AppSettings appSettings = appConfig.getAppSettings();
        boolean foundEnabledProvider = false;

        for (Map.Entry<String, XtreamProviderConfig> entry : appConfig.getProviders().entrySet()) {
            String providerName = entry.getKey();
            XtreamProviderConfig providerConfig = entry.getValue();

            if (!providerConfig.isEnabled()) {
                log.debug("Skipping disabled provider: {}", providerName);
                continue;
            }

            foundEnabledProvider = true;
            log.info("Importing {} for provider: {}", mediaType, providerName);
            ImportCommand command = new ImportCommand(appSettings, providerConfig);
            command.execute(mediaType);
        }

        if (!foundEnabledProvider) {
            System.err.println("Error: No enabled providers found");
            System.exit(1);
        }
    }

    private boolean isMediaTypeEnabled(XtreamProviderConfig providerConfig, MediaType mediaType) {
        return switch (mediaType) {
            case SERIES -> providerConfig.getSeriesSettings() != null && providerConfig.getSeriesSettings().isEnabled();
            case MOVIE -> providerConfig.getMoviesSettings() != null && providerConfig.getMoviesSettings().isEnabled();
            case LIVE -> providerConfig.getLiveSettings() != null && providerConfig.getLiveSettings().isEnabled();
        };
    }

    private void printUsage() {
        System.out.println("\nUsage:");
        System.out.println("  java -jar xtream2jellyfin.jar                         - Run the application");
        System.out.println("  java -jar xtream2jellyfin.jar get-series-categories   - List series categories");
        System.out.println("  java -jar xtream2jellyfin.jar get-movies-categories   - List movies categories");
        System.out.println("  java -jar xtream2jellyfin.jar get-live-categories     - List live categories");
        System.out.println("  java -jar xtream2jellyfin.jar import-movies           - Import movies only (one-shot)");
        System.out.println("  java -jar xtream2jellyfin.jar import-series           - Import series only (one-shot)");
        System.out.println("  java -jar xtream2jellyfin.jar import-live             - Import live only (one-shot)");
    }

}
