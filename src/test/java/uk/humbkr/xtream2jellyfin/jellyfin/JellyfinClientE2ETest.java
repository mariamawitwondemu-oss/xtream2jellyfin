package uk.humbkr.xtream2jellyfin.jellyfin;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import uk.humbkr.xtream2jellyfin.config.JellyfinServer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * E2E test for JellyfinClient against a running Jellyfin server.
 * Remove @Disabled and fill in connection details to run.
 */
//@Disabled("Requires a running Jellyfin server — fill in connection details and remove @Disabled to run")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JellyfinClientE2ETest {

    // --- Fill in your local Jellyfin details ---
    private static final String PROTOCOL = "http";

    private static final String HOSTNAME = "localhost";

    private static final int PORT = 8096;

    private static final String TOKEN = "128d94fa0e734625b94c4bcdcef11155";

    private static final String MOVIES_LIBRARY_ID = "your_movies_library_id";

    private static final String SERIES_LIBRARY_ID = "a656b907eb3a73532e40e44b968d0225";
    // -------------------------------------------

    private JellyfinClient client;

    @BeforeAll
    void setUp() {
        JellyfinServer config = new JellyfinServer();
        config.setProtocol(PROTOCOL);
        config.setHostname(HOSTNAME);
        config.setPort(PORT);
        config.setToken(TOKEN);
        config.setMoviesLibraryId(MOVIES_LIBRARY_ID);
        config.setSeriesLibraryId(SERIES_LIBRARY_ID);

        client = new JellyfinClient(config);
    }

    @Test
    void refreshMoviesLibrary_shouldNotThrow() {
        // GIVEN / WHEN / THEN
        assertDoesNotThrow(() -> client.refreshMoviesLibrary());
    }

    @Test
    void refreshSeriesLibrary_shouldNotThrow() {
        // GIVEN / WHEN / THEN
        assertDoesNotThrow(() -> client.refreshSeriesLibrary());
    }

    @Test
    void refreshLibrary_withInvalidId_shouldNotThrow() {
        // GIVEN / WHEN / THEN
        assertDoesNotThrow(() -> client.refreshLibrary("invalid-id-000", "test"));
    }

    @Test
    void refreshLibrary_withNullId_shouldNotThrow() {
        // GIVEN / WHEN / THEN
        assertDoesNotThrow(() -> client.refreshLibrary(null, "test"));
    }

    @Test
    void refreshLibrary_withBlankId_shouldNotThrow() {
        // GIVEN / WHEN / THEN
        assertDoesNotThrow(() -> client.refreshLibrary("", "test"));
    }

}
