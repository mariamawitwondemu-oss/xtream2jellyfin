package uk.humbkr.xtream2jellyfin.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import lombok.NoArgsConstructor;
import org.slf4j.LoggerFactory;
import uk.humbkr.xtream2jellyfin.config.LoggingConfig;

import java.util.Map;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class LogbackConfigurator {

    /**
     * Apply logging level overrides from config.yaml to the existing Logback configuration.
     * The base configuration is loaded from logback.xml in resources.
     */
    public static void apply(LoggingConfig config) {
        if (config == null) {
            return;
        }

        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        // Override root logger level if specified
        if (config.getLevel() != null && !config.getLevel().isBlank()) {
            Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
            rootLogger.setLevel(parseLevel(config.getLevel()));
        }

        // Override specific logger levels
        for (Map.Entry<String, String> entry : config.getLoggers().entrySet()) {
            String loggerName = entry.getKey();
            String levelStr = entry.getValue();
            Logger logger = context.getLogger(loggerName);
            logger.setLevel(parseLevel(levelStr));
        }

        System.out.println("Logback overrides applied: root level=" + config.getLevel()
                + ", custom loggers=" + config.getLoggers().size());
    }

    private static Level parseLevel(String levelStr) {
        if (levelStr == null || levelStr.isBlank()) {
            return Level.INFO;
        }
        return Level.toLevel(levelStr.toUpperCase(), Level.INFO);
    }

}
