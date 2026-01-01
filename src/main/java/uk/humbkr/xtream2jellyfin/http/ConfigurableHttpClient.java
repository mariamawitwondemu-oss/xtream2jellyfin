package uk.humbkr.xtream2jellyfin.http;

import lombok.extern.slf4j.Slf4j;
import uk.humbkr.xtream2jellyfin.config.HttpClientConfig;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Slf4j
public class ConfigurableHttpClient implements AutoCloseable {

    private final HttpClient httpClient;

    private final RateLimiter rateLimiter;

    private final int maxRetries;

    private final long retryDelayMillis;

    public ConfigurableHttpClient(HttpClientConfig config) {
        if (config == null) {
            config = new HttpClientConfig();
        }

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .build();

        this.rateLimiter = new RateLimiter(config.getRateLimit());
        this.maxRetries = config.getMaxRetries();
        this.retryDelayMillis = config.getRetryDelaySeconds() * 1000L;

        log.debug("ConfigurableHttpClient initialized: timeout={}s, rate_limit={}, max_retries={}, retry_delay={}s",
                config.getTimeoutSeconds(), config.getRateLimit(), config.getMaxRetries(), config.getRetryDelaySeconds());
    }

    public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler)
            throws IOException, InterruptedException {

        HttpResponse<T> response = null;
        IOException lastException = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                // Acquire rate limit permit before sending request
                rateLimiter.acquire();

                // Send the request
                response = httpClient.send(request, responseBodyHandler);

                // Check if response is successful
                int statusCode = response.statusCode();
                if (statusCode >= 200 && statusCode < 300) {
                    return response;
                }

                // Non-successful status code
                log.info("Request failed with status code: {}, attempt {}/{}", statusCode, attempt + 1, maxRetries + 1);

                lastException = new IOException("HTTP " + statusCode + ": " + response.body());

            } catch (IOException e) {
                lastException = e;
                log.info("Request failed with IOException: {}, attempt {}/{}", e.getMessage(), attempt + 1, maxRetries + 1);
            }

            // Wait before retry (except on last attempt)
            if (attempt < maxRetries && retryDelayMillis > 0) {
                log.info("Retrying in {} ms...", retryDelayMillis);
                Thread.sleep(retryDelayMillis);
            }
        }

        // All retries exhausted
        if (lastException != null) {
            throw lastException;
        }

        // Return last response even if not successful (shouldn't reach here normally)
        return response;
    }

    @Override
    public void close() {
        httpClient.close();
    }

}
