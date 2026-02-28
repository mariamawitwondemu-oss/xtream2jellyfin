package uk.humbkr.xtream2jellyfin.command;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import uk.humbkr.xtream2jellyfin.config.MediaSettings;
import uk.humbkr.xtream2jellyfin.config.XtreamProviderConfig;
import uk.humbkr.xtream2jellyfin.http.ConfigurableHttpClient;
import uk.humbkr.xtream2jellyfin.utils.JsonUtils;
import uk.humbkr.xtream2jellyfin.xtream.model.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class CountStreamsCommand {

    private final ConfigurableHttpClient httpClient;

    private final ObjectMapper objectMapper;

    private final XtreamProviderConfig providerConfig;

    public CountStreamsCommand(XtreamProviderConfig providerConfig) {
        this.providerConfig = providerConfig;
        this.objectMapper = JsonUtils.getJsonMapper();
        this.httpClient = new ConfigurableHttpClient(providerConfig.getHttpClientConfig());
    }

    public void execute(MediaType mediaType) {
        try {
            log.info("Counting {} streams from provider: {}", mediaType, providerConfig.getName());

            List<Category> categories = fetchCategories(mediaType);
            Map<String, String> categoryNames = categories.stream()
                    .collect(Collectors.toMap(Category::getCategoryId, Category::getCategoryName));

            MediaSettings mediaSettings = getMediaSettings(mediaType);

            Set<String> includedCategoryIds = categoryNames.keySet().stream()
                    .filter(id -> isIncluded(id, mediaSettings))
                    .collect(Collectors.toSet());

            Map<String, Integer> countPerCategory = countStreamsPerCategory(mediaType, includedCategoryIds);

            printTable(mediaType, categoryNames, countPerCategory);

        } catch (Exception e) {
            log.error("Failed to count streams: {}", e.getMessage(), e);
            System.err.println("Error: Failed to count streams - " + e.getMessage());
        }
    }

    private List<Category> fetchCategories(MediaType mediaType) throws IOException, InterruptedException {
        Action action = Constants.MEDIA_RESOLVERS.get(mediaType).get(Constants.MEDIA_RESOLVER_CATEGORIES);
        String responseBody = get(buildUrl(action));
        if (responseBody == null) return Collections.emptyList();
        return objectMapper.readValue(responseBody, new TypeReference<List<Category>>() {});
    }

    private Map<String, Integer> countStreamsPerCategory(MediaType mediaType, Set<String> includedCategoryIds)
            throws IOException, InterruptedException {
        Action action = Constants.MEDIA_RESOLVERS.get(mediaType).get(Constants.MEDIA_RESOLVER_STREAMS);
        String responseBody = get(buildUrl(action));
        if (responseBody == null) return Collections.emptyMap();

        Map<String, Integer> counts = new HashMap<>();

        if (mediaType == MediaType.LIVE) {
            List<Map<String, Object>> streams = objectMapper.readValue(responseBody, new TypeReference<>() {});
            for (Map<String, Object> stream : streams) {
                Object catId = stream.get("category_id");
                if (catId != null) {
                    String categoryId = String.valueOf(catId);
                    if (includedCategoryIds.contains(categoryId)) {
                        counts.merge(categoryId, 1, Integer::sum);
                    }
                }
            }
        } else {
            List<? extends StreamItem> streams;
            if (mediaType == MediaType.SERIES) {
                streams = objectMapper.readValue(responseBody, new TypeReference<List<SeriesItem>>() {});
            } else {
                streams = objectMapper.readValue(responseBody, new TypeReference<List<MovieItem>>() {});
            }
            for (StreamItem stream : streams) {
                String categoryId = stream.getCategoryId();
                if (categoryId != null && includedCategoryIds.contains(categoryId)) {
                    counts.merge(categoryId, 1, Integer::sum);
                }
            }
        }

        return counts;
    }

    private boolean isIncluded(String categoryId, MediaSettings settings) {
        if (settings == null) return true;
        List<String> includeCategoryIds = settings.getIncludeCategoryIds();
        List<String> excludeCategoryIds = settings.getExcludeCategoryIds();
        if (!includeCategoryIds.isEmpty()) return includeCategoryIds.contains(categoryId);
        return !excludeCategoryIds.contains(categoryId);
    }

    private MediaSettings getMediaSettings(MediaType mediaType) {
        return switch (mediaType) {
            case SERIES -> providerConfig.getSeriesSettings();
            case MOVIE -> providerConfig.getMoviesSettings();
            case LIVE -> providerConfig.getLiveSettings();
        };
    }

    private String get(String url) throws IOException, InterruptedException {
        log.debug("Fetching from URL: {}", url);

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

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return response.body();
        } else {
            log.error("HTTP error: {}", response.statusCode());
            return null;
        }
    }

    private void printTable(MediaType mediaType, Map<String, String> categoryNames, Map<String, Integer> countPerCategory) {
        List<String> sortedCategoryIds = new ArrayList<>(countPerCategory.keySet());
        sortedCategoryIds.sort(Comparator.naturalOrder());

        int totalStreams = countPerCategory.values().stream().mapToInt(Integer::intValue).sum();
        int categoryCount = countPerCategory.size();

        System.out.println("\n" + mediaType.toString().toUpperCase() + " STREAMS - " + providerConfig.getName());
        System.out.println("=".repeat(60));
        System.out.printf("%-15s %-32s %s%n", "CATEGORY ID", "CATEGORY NAME", "COUNT");
        System.out.println("-".repeat(60));

        for (String categoryId : sortedCategoryIds) {
            String name = categoryNames.getOrDefault(categoryId, "Unknown");
            int count = countPerCategory.get(categoryId);
            System.out.printf("%-15s %-32s %d%n", categoryId, name, count);
        }

        System.out.println("-".repeat(60));
        System.out.println("Total: " + totalStreams + " streams across " + categoryCount + " categories\n");
    }

    private String buildUrl(Action action) {
        String credentials = "username=" + providerConfig.getUsername() +
                "&password=" + providerConfig.getPassword();
        return providerConfig.getUrl() + "/" + Endpoint.PLAYER + ".php?" +
                credentials + "&action=" + action;
    }

}
