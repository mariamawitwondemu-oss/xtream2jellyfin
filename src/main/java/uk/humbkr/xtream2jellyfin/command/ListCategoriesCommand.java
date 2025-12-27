package uk.humbkr.xtream2jellyfin.command;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import uk.humbkr.xtream2jellyfin.common.JsonUtils;
import uk.humbkr.xtream2jellyfin.config.XtreamProviderConfig;
import uk.humbkr.xtream2jellyfin.xtream.model.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Slf4j
public class ListCategoriesCommand {

    private final HttpClient httpClient;

    private final ObjectMapper objectMapper;

    private final XtreamProviderConfig providerConfig;

    public ListCategoriesCommand(XtreamProviderConfig providerConfig) {
        this.providerConfig = providerConfig;
        this.objectMapper = JsonUtils.getJsonMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    public void execute(MediaType mediaType) {
        try {
            log.info("Fetching {} categories from provider: {}", mediaType, providerConfig.getName());

            List<Category> categories = fetchCategories(mediaType);

            if (categories.isEmpty()) {
                System.out.println("No categories found.");
                return;
            }

            categories.sort(Comparator.comparing(Category::getCategoryId));

            System.out.println("\n" + mediaType.toString().toUpperCase() + " CATEGORIES");
            System.out.println("=".repeat(60));
            System.out.printf("%-15s %s%n", "CATEGORY ID", "CATEGORY NAME");
            System.out.println("-".repeat(60));

            for (Category category : categories) {
                System.out.printf("%-15s %s%n", category.getCategoryId(), category.getCategoryName());
            }

            System.out.println("-".repeat(60));
            System.out.println("Total: " + categories.size() + " categories\n");

        } catch (Exception e) {
            log.error("Failed to fetch categories: {}", e.getMessage(), e);
            System.err.println("Error: Failed to fetch categories - " + e.getMessage());
        }
    }

    private List<Category> fetchCategories(MediaType mediaType) throws IOException, InterruptedException {
        Action action = getCategoriesAction(mediaType);
        String url = buildUrl(action);

        log.debug("Fetching categories from URL: {}", url);

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
            String responseBody = response.body();
            return objectMapper.readValue(responseBody, new TypeReference<>() {
            });
        } else {
            log.error("HTTP error: {}", response.statusCode());
            return Collections.emptyList();
        }
    }

    private Action getCategoriesAction(MediaType mediaType) {
        return switch (mediaType) {
            case SERIES -> Action.SERIES_CATEGORIES;
            case MOVIE -> Action.VOD_CATEGORIES;
            case LIVE -> Action.LIVE_CATEGORIES;
        };
    }

    private String buildUrl(Action action) {
        String credentials = "username=" + providerConfig.getUsername() +
                "&password=" + providerConfig.getPassword();
        return providerConfig.getUrl() + "/" + Endpoint.PLAYER + ".php?" +
                credentials + "&action=" + action;
    }

}
