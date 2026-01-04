package uk.humbkr.xtream2jellyfin.xtream;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import uk.humbkr.xtream2jellyfin.common.StringUtils;
import uk.humbkr.xtream2jellyfin.config.AppSettings;
import uk.humbkr.xtream2jellyfin.config.XtreamProviderConfig;
import uk.humbkr.xtream2jellyfin.filemanager.CachedFileManager;
import uk.humbkr.xtream2jellyfin.filemanager.FileManager;
import uk.humbkr.xtream2jellyfin.filemanager.SimpleFileManager;
import uk.humbkr.xtream2jellyfin.http.ConfigurableHttpClient;
import uk.humbkr.xtream2jellyfin.validation.DomainValidator;
import uk.humbkr.xtream2jellyfin.xtream.model.Endpoint;
import uk.humbkr.xtream2jellyfin.xtream.model.Profile;
import uk.humbkr.xtream2jellyfin.xtream.model.ServerInfo;
import uk.humbkr.xtream2jellyfin.xtream.model.UserInfo;

import java.util.Set;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class XtreamProcessor implements AutoCloseable {

    private final String providerName;

    private final XtreamProviderConfig providerConfig;

    private final ConfigurableHttpClient httpClient;

    private final FileManager fileManager;

    private final DomainValidator domainValidator;

    private final List<BaseStreamsHandler> streamHandlers = new ArrayList<>(3);

    public XtreamProcessor(@NonNull AppSettings appSettings, @NonNull XtreamProviderConfig providerConfig) {

        this.providerName = providerConfig.getName();
        this.providerConfig = providerConfig;

        this.httpClient = new ConfigurableHttpClient(providerConfig.getHttpClientConfig());

        this.fileManager = this.createFileManager(appSettings);
        log.info("Using file manager: {}", this.fileManager.getClass().getSimpleName());

        // Create domain validator
        this.domainValidator = createDomainValidator(appSettings);

        if (providerConfig.getLiveSettings().isEnabled()) {
            streamHandlers.add(new LiveStreamsHandler(providerConfig, fileManager, appSettings, httpClient, domainValidator));
        }
        if (providerConfig.getSeriesSettings().isEnabled()) {
            streamHandlers.add(new SeriesStreamsHandler(providerConfig, fileManager, appSettings, httpClient, domainValidator));
        }
        if (providerConfig.getMoviesSettings().isEnabled()) {
            streamHandlers.add(new MoviesStreamsHandler(providerConfig, fileManager, appSettings, httpClient, domainValidator));
        }
    }

    private FileManager createFileManager(AppSettings appSettings) {
        String fileManagerType = appSettings.getFileManagerType();
        String mediaDir = appSettings.getMediaDir() + "/" + providerName;
        if ("cached".equalsIgnoreCase(fileManagerType)) {
            String cacheDir = appSettings.getCacheDir() + "/" + providerName;
            return new CachedFileManager(mediaDir, cacheDir);
        } else {
            return new SimpleFileManager(mediaDir);
        }
    }

    private DomainValidator createDomainValidator(AppSettings appSettings) {
        if (!appSettings.isDomainValidationEnabled()) {
            log.info("Domain validation is disabled");
            return null;
        }

        String cacheDir = appSettings.getCacheDir() + "/" + providerName;
        int timeout = appSettings.getDomainValidationTimeout();
        int failureThreshold = appSettings.getDomainValidationFailureThreshold();
        var whitelist = appSettings.getDomainValidationWhitelist();

        log.info("Domain validation enabled (timeout: {}ms, failure threshold: {}, whitelist: {})",
                timeout, failureThreshold, whitelist);
        return new DomainValidator(cacheDir, timeout, failureThreshold, whitelist);
    }

    public void run() {

        if (!this.checkBeforeProcessing()) {
            return;
        }

        log.info("[{}] Processing", providerName);

        if (!this.authenticate()) {
            log.error("[{}] Authentication failed, please check your credentials", providerName);
            return;
        }

        try {

            fileManager.onProcessStart();

            streamHandlers.forEach(BaseStreamsHandler::process);

            fileManager.onProcessEnd();

            reportBlacklistedDomains();

            log.info("{} processing completed", providerName);

        } catch (Exception ex) {
            if (ex.getMessage().contains("Authentication failed")) {
                log.error("Failed to start processing, Provider: {}, Error: Invalid Credentials", providerName);
            } else {
                log.error("Failed to start processing, Provider: {}, Error: {}", providerName, ex.getMessage(), ex);
            }
        }

        waitForNextIteration();
    }

    private boolean checkBeforeProcessing() {
        if (StringUtils.isBlank(providerConfig.getUsername()) || StringUtils.isBlank(providerConfig.getPassword())) {
            log.error("[{}] Failed to run, please set credentials", providerName);
            return false;
        }
        if (streamHandlers.isEmpty()) {
            log.error("[{}] No stream handlers enabled, please check configuration", providerName);
            return false;
        }
        return true;
    }

    private boolean authenticate() {
        boolean isAuth = false;

        BaseStreamsHandler firstHandler = streamHandlers.getFirst();
        Profile profile = firstHandler.getData(
                Endpoint.PLAYER,
                null,
                null,
                Profile.class
        );

        if (profile != null && profile.getUserInfo() != null) {
            UserInfo userInfo = profile.getUserInfo();
            ServerInfo serverInfo = profile.getServerInfo();

            Integer authenticated = userInfo.getAuth();
            String status = userInfo.getStatus();

            isAuth = authenticated != null && authenticated == 1 && "Active".equals(status);

            if (isAuth && serverInfo != null) {
                for (BaseStreamsHandler streamHandler : streamHandlers) {
                    streamHandler.setProviderUrl(serverInfo);
                }
            }
        }

        return isAuth;
    }

    private void waitForNextIteration() {
        long now = System.currentTimeMillis();
        long interval = 60L * providerConfig.getScanIntervalMinutes() * 1000;
        long nextIteration = now + interval;

        LocalDateTime nextIterationTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(nextIteration),
                ZoneId.systemDefault()
        );

        String nextIterationIso = nextIterationTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        log.info("Next iteration for {} at {}", providerName, nextIterationIso);

        try {
            Thread.sleep(interval);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while waiting for next iteration", e);
        }
    }

    private void postProcessing() {
//        if (postProcessingEnabled) {
//            String url = postProcessingUrl + "/Library/Refresh";
//            String serverDetails = "Jellyfin Server: " + postProcessingUrl;
//
//            try {
//                HttpRequest request = HttpRequest.newBuilder()
//                        .uri(URI.create(url))
//                        .header("X-Jellyfin-Token", jellyfinToken)
//                        .timeout(Duration.ofSeconds(30))
//                        .POST(HttpRequest.BodyPublishers.noBody())
//                        .build();
//
//                HttpResponse<String> response = httpClient.send(
//                        request,
//                        HttpResponse.BodyHandlers.ofString()
//                );
//
//                if (response.statusCode() >= 200 && response.statusCode() < 300) {
//                    log.info("Refresh library triggered, {}", serverDetails);
//                } else {
//                    log.error("Refresh library failed to trigger, {}, Error: {}",
//                            serverDetails, response.statusCode());
//                }
//            } catch (Exception e) {
//                log.error("Failed to trigger library refresh: {}", serverDetails, e);
//            }
//        }
    }

    private void reportBlacklistedDomains() {
        if (domainValidator == null) {
            return;
        }

        Set<String> blacklisted = domainValidator.getBlacklistedDomains();
        if (!blacklisted.isEmpty()) {
            log.warn("[{}] {} domains were blacklisted due to validation failures: {}",
                    providerName, blacklisted.size(), blacklisted);
        }

        // Save cache for persistence across runs
        domainValidator.saveCache();

        // Reset blacklisted domains for next iteration
        domainValidator.reset();
    }

    @Override
    public void close() {
        this.httpClient.close();
    }

}
