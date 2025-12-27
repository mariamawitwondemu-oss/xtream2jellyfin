package uk.humbkr.xtream2jellyfin.command;

import lombok.extern.slf4j.Slf4j;
import uk.humbkr.xtream2jellyfin.config.AppConfig;
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

        for (Map.Entry<String, XtreamProviderConfig> entry : appConfig.getProviders().entrySet()) {
            String providerName = entry.getKey();
            XtreamProviderConfig providerConfig = entry.getValue();

            System.out.println("\nProvider: " + providerName);
            ListCategoriesCommand command = new ListCategoriesCommand(providerConfig);
            command.execute(mediaType);
        }
    }

    private void printUsage() {
        System.out.println("\nUsage:");
        System.out.println("  java -jar xtream2jellyfin.jar                         - Run the application");
        System.out.println("  java -jar xtream2jellyfin.jar get-series-categories   - List series categories");
        System.out.println("  java -jar xtream2jellyfin.jar get-movies-categories   - List movies categories");
        System.out.println("  java -jar xtream2jellyfin.jar get-live-categories     - List live categories");
    }

}
