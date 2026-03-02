package uk.humbkr.xtream2jellyfin.xtream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import org.slf4j.Logger;
import uk.humbkr.xtream2jellyfin.config.AppSettings;
import uk.humbkr.xtream2jellyfin.config.MediaSettings;
import uk.humbkr.xtream2jellyfin.config.XtreamProviderConfig;
import uk.humbkr.xtream2jellyfin.filemanager.FileManager;
import uk.humbkr.xtream2jellyfin.http.ConfigurableHttpClient;
import uk.humbkr.xtream2jellyfin.nameformat.CategoryNameFormat;
import uk.humbkr.xtream2jellyfin.nameformat.StreamNameFormat;
import uk.humbkr.xtream2jellyfin.utils.JsonUtils;
import uk.humbkr.xtream2jellyfin.utils.RegexUtils;
import uk.humbkr.xtream2jellyfin.utils.StringUtils;
import uk.humbkr.xtream2jellyfin.validation.DomainValidator;
import uk.humbkr.xtream2jellyfin.xtream.model.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.net.URI;
public abstract class BaseStreamsHandler {

    protected final ObjectMapper objectMapper;

    protected final ConfigurableHttpClient httpClient;

    protected final FileManager fileManager;

    protected final String providerName;

    protected final String username;

    protected final String password;

    protected final Map<String, String> categoryNameCleanupPatterns;

    protected final Map<String, String> nameCleanupPatterns;

    protected final List<String> includeCategoryIds;

    protected final List<String> excludeCategoryIds;

    protected final boolean categoryFolder;

    protected final boolean writeMetadataNfo;

    protected final String mediaDir;

    protected final Map<String, Action> resolvers;

    protected final StreamNameFormat mediaNameFormat;

    protected final CategoryNameFormat categoryNameFormat;

    protected final DomainValidator domainValidator;

    private final Logger log;

    protected String providerUrl;

    protected Map<String, Object> data;

    protected Map<String, String> categories;

    protected int streamsCount = 0;

    protected int streamProcessed = 0;

    protected int streamsSkipped = 0;

    protected long processingStartTime = 0;

    public BaseStreamsHandler(XtreamProviderConfig providerConfig, FileManager fileManager,
                              AppSettings appSettings, ConfigurableHttpClient httpClient,
                              DomainValidator domainValidator, Logger log) {
        this.log = log;
        this.objectMapper = JsonUtils.getJsonMapper();
        this.httpClient = httpClient;
        this.domainValidator = domainValidator;

        this.fileManager = fileManager;
        this.providerName = Objects.requireNonNull(providerConfig.getName());
        this.writeMetadataNfo = appSettings.isWriteMetadataNfo();

        this.username = providerConfig.getUsername();
        this.password = providerConfig.getPassword();
        this.providerUrl = providerConfig.getUrl();
        this.categoryNameCleanupPatterns = providerConfig.getCategoryNameCleanupPatterns();

        String baseMediaDir = appSettings.getMediaDir();
        this.mediaDir = baseMediaDir + "/" + providerName + "/" + getMediaType();

        MediaSettings mediaSettings = getMediaSettingsForType(providerConfig);

        this.nameCleanupPatterns = mediaSettings.getNameCleanupPatterns();
        this.includeCategoryIds = mediaSettings.getIncludeCategoryIds();
        this.excludeCategoryIds = mediaSettings.getExcludeCategoryIds();
        this.categoryFolder = mediaSettings.isCategoryFolder();

        this.resolvers = Constants.MEDIA_RESOLVERS.get(getMediaType());
        this.data = new HashMap<>();
        this.categories = new HashMap<>();

        this.mediaNameFormat = new StreamNameFormat(mediaSettings.getNameFormat(), mediaSettings.getNameCleanupPatterns());
        this.categoryNameFormat = new CategoryNameFormat(this.categoryNameCleanupPatterns);
    }

    private MediaSettings getMediaSettingsForType(XtreamProviderConfig config) {
        return switch (this.getMediaType()) {
            case LIVE -> config.getLiveSettings();
            case MOVIE -> {
                config.getMoviesSettings().setNameFormat("${name} (${year}) [${externalProviderId}-${externalId}]");
                yield config.getMoviesSettings();
            }
            case SERIES -> {
                config.getSeriesSettings().setNameFormat("${name} (${year}) [${externalProviderId}-${externalId}]");
                yield config.getSeriesSettings();
            }
        };
    }

    public abstract MediaType getMediaType();

    protected void processItem(Object item) {
        // To be overridden by subclasses
    }

    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> getStreams() {
        return (List<Map<String, Object>>) data.get(Constants.MEDIA_RESOLVER_STREAMS);
    }

    @SuppressWarnings("unchecked")
    protected List<Category> getCategories() {
        return (List<Category>) data.get(Constants.MEDIA_RESOLVER_CATEGORIES);
    }

    //public void setProviderUrl(ServerInfo serverInfo) {
    //    String url = serverInfo.getUrl();
    //    String serverProtocol = serverInfo.getServerProtocol();
    //    this.providerUrl = serverProtocol + "://" + url;
    //}
    
    public void setProviderUrl(ServerInfo serverInfo) {
        String serverProtocol = serverInfo.getServerProtocol();
        String serverHost = serverInfo.getUrl(); // often "host" without ":port"
    
        // Parse currently configured providerUrl to preserve an explicit port (if present)
        int configuredPort = -1;
        try {
            URI configured = URI.create(this.providerUrl);
            configuredPort = configured.getPort(); // -1 if none specified
        } catch (Exception ignored) {
            // If misconfigured, fall back to serverInfo
        }
    
        // If serverInfo doesn't include a port but config did, keep the configured port
        if (configuredPort != -1 && serverHost != null && !serverHost.contains(":")) {
            serverHost = serverHost + ":" + configuredPort;
        }
    
        this.providerUrl = serverProtocol + "://" + serverHost;
    }
    public void process() {
        try {
            processingStartTime = System.currentTimeMillis();

            loadData();
            loadCategories();

            logInfo("Loading streams");

            processStreams();

            long executionTime = System.currentTimeMillis() - processingStartTime;

            data.clear();
            categories.clear();

            logInfo(String.format("Complete processing, Total: %d, Processed: %d, Skipped: %d, Duration: %.3f seconds",
                    streamsCount, streamProcessed, streamsSkipped, executionTime / 1000.0));

        } catch (Exception ex) {
            logError("Failed to process: " + ex.getMessage(), ex);
        }
    }

    protected void processStreams() {

        // Get reference to streams list before removing from data map
        @SuppressWarnings("unchecked")
        List<StreamItem> allStreams = (List<StreamItem>) data.remove(Constants.MEDIA_RESOLVER_STREAMS);

        // Count total streams
        int totalStreamsCount = allStreams.size();

        logInfo("Total streams available: " + totalStreamsCount);

        resetCounters(totalStreamsCount);

        // Process streams one at a time, allowing GC to collect each stream after processing
        Iterator<StreamItem> streamsIterator = allStreams.iterator();
        while (streamsIterator.hasNext()) {
            StreamItem streamItem = streamsIterator.next();
            String streamName = streamItem.getName();
            if (!canProcess(streamItem)) {
                logDebug("Skipping stream: " + streamName);
                streamsSkipped++;
            } else {
                try {
                    processItem(streamItem);
                } catch (Exception ex) {
                    logError("Failed to process " + getMediaType() + " stream, ID: " + streamName + ", Error: " + ex.getMessage(), ex);
                }
                updateCounters();
            }
            streamsIterator.remove();

        }

        // Clear the entire list to release all references
        allStreams.clear();
    }

    protected boolean canProcess(StreamItem streamItem) {
        String streamName = streamItem.getName();
        String categoryId = streamItem.getCategoryId();

        if (streamName == null) {
            return false;
        }

        // If include_category_ids is set, it takes precedence over exclude_category_ids
        if (!includeCategoryIds.isEmpty()) {
            return includeCategoryIds.contains(categoryId);
        }

        // Otherwise, use exclude_category_ids logic
        return !excludeCategoryIds.contains(categoryId);
    }

    protected void loadData() {
        try {
            long startTime = System.currentTimeMillis();

            data = new HashMap<>();
            categories = new HashMap<>();

            List<Object[]> allDataPoints = getDataPoints();

            for (Object[] dataPoints : allDataPoints) {
                Endpoint endpoint = (Endpoint) dataPoints[0];
                String dataPoint = (String) dataPoints[1];
                Action action = (Action) dataPoints[2];

                loadDataPoint(endpoint, dataPoint, action);
            }

            long executionTime = System.currentTimeMillis() - startTime;

            logInfo(String.format("Loaded %d lists, Duration: %.3f seconds", allDataPoints.size(), executionTime / 1000.0));

        } catch (Exception ex) {
            logError("Failed to load data: " + ex.getMessage(), ex);
        }
    }

    protected List<Object[]> getDataPoints() {
        List<Object[]> args = new ArrayList<>();

        for (String resolverAction : Constants.LOAD_DATA_ACTIONS) {
            Action action = resolvers.get(resolverAction);
            if (action != null) {
                args.add(new Object[]{Endpoint.PLAYER, resolverAction, action});
            }
        }

        return args;
    }

    protected void loadDataPoint(Endpoint endpoint, String dataPoint, Action action) {
        try {
            logDebug("Load endpoint data, Endpoint: " + endpoint);

            Object dataResult = getData(endpoint, action, null);

            if (dataResult != null) {
                if (endpoint == Endpoint.PLAYER) {
                    data.put(dataPoint, dataResult);
                }

                extraDataLoading(endpoint, dataResult);

                logInfo("Endpoint '" + endpoint + "' data loaded, Action: " + dataPoint);
            }

        } catch (Exception ex) {
            logError("Failed to load endpoint data, Endpoint: " + endpoint + ", Error: " + ex.getMessage(), ex);
        }
    }

    protected void extraDataLoading(Endpoint endpoint, Object data) {
        // To be overridden by subclasses
    }

    protected void loadCategories() {
        try {
            long startTime = System.currentTimeMillis();
            logInfo("Loading categories");

            for (Category category : getCategories()) {
                String categoryId = category.getCategoryId();
                String categoryName = category.getCategoryName();

                categoryName = cleanCategoryName(categoryName);

                categories.put(categoryId, capitalize(categoryName));
            }

            long executionTime = System.currentTimeMillis() - startTime;

            logDebug("Categories loaded: " + categories);

            logInfo(String.format("Loaded %d categories, Duration: %.3f seconds", getCategories().size(), executionTime / 1000.0));

        } catch (Exception ex) {
            logError("Failed to load categories: " + ex.getMessage(), ex);
        }
    }

    protected String cleanCategoryName(String categoryName) {
        if (StringUtils.isBlank(categoryName)) {
            return categoryName;
        }
        String cleanedCategoryName = categoryNameFormat.format(categoryName);
        if (!categoryName.equals(cleanedCategoryName)) {
            logDebug(String.format("Cleaned category name: '%s' -> '%s'", categoryName, cleanedCategoryName));
        }
        return cleanedCategoryName;
    }

    protected String cleanNameRegex(String text) {
        if (text != null) {
            for (Map.Entry<String, String> entry : nameCleanupPatterns.entrySet()) {
                String regexFind = entry.getKey();
                String regexReplace = entry.getValue();

                text = RegexUtils.replaceAll(text, regexFind, regexReplace);
            }
            text = text.trim();
        }
        return text;
    }

    protected String buildUrl(Endpoint endpoint, Action action, String contextId) {
        String credentials = "username" + "=" + username + "&" +
                "password" + "=" + password;
        String url = providerUrl + "/" + endpoint + ".php?" + credentials;

        if (action != null && endpoint == Endpoint.PLAYER) {
            url += "&action=" + action;

            if (contextId != null) {
                String contextKey = Constants.CONTEXT_PARAMETER.get(action);
                if (contextKey != null) {
                    url += "&" + contextKey + "=" + contextId;
                }
            }
        }

        logDebug("Built URL: " + url);

        return url;
    }

    public Object getData(Endpoint endpoint, Action action, String contextId) {
        String url = buildUrl(endpoint, action, contextId);

        logDebug("Fetching data from URL: " + url);

        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .GET();

            for (Map.Entry<String, String> header : Constants.HEADERS.entrySet()) {
                requestBuilder.header(header.getKey(), header.getValue());
            }

            HttpResponse<String> response = httpClient.send(
                    requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            String responseBody = response.body();

            if (endpoint.isJson()) {
                if (action.getCollectionType() != null) {
                    CollectionType collectionType = objectMapper.getTypeFactory()
                            .constructCollectionType(action.getCollectionType(), action.getValueType());
                    return objectMapper.readValue(responseBody, collectionType);
                } else {
                    return objectMapper.readValue(responseBody, action.getValueType());
                }
            } else {
                return action.getValueType().cast(responseBody);
            }

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            logError("Request failed: " + e.getMessage(), e);
            return null;
        }
    }

    protected <T> T getData(Endpoint endpoint, Action action, String contextId, Class<T> clazz) {
        String url = buildUrl(endpoint, action, contextId);

        logDebug("Fetching data from URL: " + url);

        String responseBody = null;
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .GET();

            for (Map.Entry<String, String> header : Constants.HEADERS.entrySet()) {
                requestBuilder.header(header.getKey(), header.getValue());
            }

            HttpResponse<String> response = httpClient.send(
                    requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            responseBody = response.body();

            if (endpoint.isJson()) {
                return objectMapper.readValue(responseBody, clazz);
            } else {
                return clazz.cast(responseBody);
            }

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            logError("Request failed: " + e.getMessage(), e);
            logInfo("Response body: " + responseBody);
            return null;
        }
    }

    protected String buildStreamUrl(String streamId, String ext) {
        return providerUrl + "/" + getMediaType() + "/" + username + "/" + password + "/" + streamId + "." + ext;
    }

    protected void addFile(String filePath, Object content) {
        fileManager.save(filePath, content);
    }

    protected void resetCounters(int streams) {
        this.streamsCount = streams;
        this.streamProcessed = 0;
        this.streamsSkipped = 0;
    }

    protected void updateCounters() {
        long executionTime = System.currentTimeMillis() - processingStartTime;

        streamProcessed++;

        int totalHandled = streamProcessed + streamsSkipped;
        double progress = (double) totalHandled / streamsCount;
        double progressLeftRatio = 1.0 / progress;
        double expectedDuration = progressLeftRatio * executionTime;
        double timeLeft = expectedDuration - executionTime;

        logInfo(String.format("Progress: %d / %d (%.1f%%), Processed: %d, Skipped: %d, Estimated time left: %.2f seconds",
                totalHandled, streamsCount, progress * 100, streamProcessed, streamsSkipped, timeLeft / 1000.0));
    }

    protected void logError(String message, Exception ex) {
        log.error("[{}::{}] {}", providerName, getMediaType(), message, ex);
    }

    protected void logWarning(String message) {
        log.warn("[{}::{}] {}", providerName, getMediaType(), message);
    }

    protected void logInfo(String message) {
        log.info("[{}::{}] {}", providerName, getMediaType(), message);
    }

    protected void logDebug(String message) {
        log.debug("[{}::{}] {}", providerName, getMediaType(), message);
    }

    protected void logTrace(String message) {
        log.trace("[{}::{}] {}", providerName, getMediaType(), message);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

}
