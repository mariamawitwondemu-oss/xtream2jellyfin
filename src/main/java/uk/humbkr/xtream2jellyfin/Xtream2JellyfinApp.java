package uk.humbkr.xtream2jellyfin;

import lombok.extern.slf4j.Slf4j;
import uk.humbkr.xtream2jellyfin.command.CommandExecutor;
import uk.humbkr.xtream2jellyfin.common.Constants;
import uk.humbkr.xtream2jellyfin.common.YamlUtils;
import uk.humbkr.xtream2jellyfin.config.AppConfig;
import uk.humbkr.xtream2jellyfin.config.AppSettings;
import uk.humbkr.xtream2jellyfin.config.XtreamProviderConfig;
import uk.humbkr.xtream2jellyfin.logging.LogbackConfigurator;
import uk.humbkr.xtream2jellyfin.xtream.XtreamProcessor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class Xtream2JellyfinApp {

    public static void main(String[] args) {
        Xtream2JellyfinApp app = new Xtream2JellyfinApp();
        AppConfig appConfig = app.readConfig();

        // Apply logging level overrides from config.yaml
        // Base configuration is loaded from logback.xml in resources
        LogbackConfigurator.apply(appConfig.getAppSettings().getLogging());

        if (args.length > 0) {
            CommandExecutor commandExecutor = new CommandExecutor(appConfig);
            commandExecutor.execute(args);
        } else {
            app.run(appConfig);
        }
    }

    public void run(AppConfig appConfig) {
        log.info("Starting xtream2jellyfin");

        List<Thread> threads = new ArrayList<>();

        for (XtreamProviderConfig providerConfig : appConfig.getProviders().values()) {
            String providerName = providerConfig.getName();
            if (!providerConfig.isEnabled()) {
                log.info("Skipping disabled provider {}", providerName);
                continue;
            }
            Thread thread = new Thread(() -> runXtreamProcessor(appConfig.getAppSettings(), providerConfig));
            thread.setName("provider-" + providerName);
            threads.add(thread);
            thread.start();
        }

        // Wait for all provider threads (they run indefinitely with scheduled intervals)
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while waiting for provider threads", e);
            }
        }

        log.info("xtream2jellyfin stopped");
    }

    private AppConfig readConfig() {
        File configFile = new File(Constants.CONFIG_FILE);
        if (configFile.exists()) {
            try {
                return YamlUtils.getYamlMapper().readValue(configFile, AppConfig.class);
            } catch (IOException e) {
                log.error("Failed to load config file: {}", e.getMessage());
                log.debug("", e);
            }
        }
        log.error("Config file not found: {}", Constants.CONFIG_FILE);
        return new AppConfig();
    }

    private void runXtreamProcessor(AppSettings appSettings, XtreamProviderConfig providerConfig) {
        try (XtreamProcessor xtreamProcessor = new XtreamProcessor(appSettings, providerConfig)) {
            xtreamProcessor.run();
        }
    }

}
