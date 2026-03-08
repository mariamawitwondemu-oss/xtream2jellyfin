package uk.humbkr.xtream2jellyfin.xtream;

import org.junit.jupiter.api.Test;
import uk.humbkr.xtream2jellyfin.xtream.model.ServerInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BaseStreamsHandlerParseProviderUrlTest {

    // --- canonical format (bare hostname + separate fields) ---

    @Test
    void whenBareHostnameWithPortAndProtocolFields_thenBuildsUrl() {
        // GIVEN
        ServerInfo serverInfo = serverInfo("example.com", "8080", "", "http");

        // WHEN
        String result = BaseStreamsHandler.parseProviderUrl(serverInfo);

        // THEN
        assertEquals("http://example.com:8080", result);
    }

    @Test
    void whenBareHostnameHttps_thenUsesHttpsPort() {
        // GIVEN
        ServerInfo serverInfo = serverInfo("example.com", "", "8443", "https");

        // WHEN
        String result = BaseStreamsHandler.parseProviderUrl(serverInfo);

        // THEN
        assertEquals("https://example.com:8443", result);
    }

    @Test
    void whenBareHostnameNoPort_thenOmitsPort() {
        // GIVEN
        ServerInfo serverInfo = serverInfo("example.com", "", "", "http");

        // WHEN
        String result = BaseStreamsHandler.parseProviderUrl(serverInfo);

        // THEN
        assertEquals("http://example.com", result);
    }

    @Test
    void whenBareIpAddress_thenBuildsUrl() {
        // GIVEN
        ServerInfo serverInfo = serverInfo("192.168.1.100", "8080", "", "http");

        // WHEN
        String result = BaseStreamsHandler.parseProviderUrl(serverInfo);

        // THEN
        assertEquals("http://192.168.1.100:8080", result);
    }

    @Test
    void whenNoProtocolField_thenDefaultsToHttp() {
        // GIVEN
        ServerInfo serverInfo = serverInfo("example.com", "8080", "", null);

        // WHEN
        String result = BaseStreamsHandler.parseProviderUrl(serverInfo);

        // THEN
        assertEquals("http://example.com:8080", result);
    }

    // --- full URL in url field (non-standard, e.g. iptvnator mock) ---

    @Test
    void whenFullUrlInUrlField_thenExtractsAllParts() {
        // GIVEN — iptvnator mock style: full URL in url field, matching separate fields
        ServerInfo serverInfo = serverInfo("http://localhost:3211", "3211", "", "http");

        // WHEN
        String result = BaseStreamsHandler.parseProviderUrl(serverInfo);

        // THEN
        assertEquals("http://localhost:3211", result);
    }

    @Test
    void whenFullUrlWithoutPort_thenOmitsPort() {
        // GIVEN
        ServerInfo serverInfo = serverInfo("http://example.com", "8080", "", "http");

        // WHEN
        String result = BaseStreamsHandler.parseProviderUrl(serverInfo);

        // THEN — port from URL (none) takes precedence over port field
        assertEquals("http://example.com", result);
    }

    @Test
    void whenFullUrlProtocolDiffersFromField_thenUrlProtocolWins() {
        // GIVEN
        ServerInfo serverInfo = serverInfo("https://example.com:8443", "", "9090", "http");

        // WHEN
        String result = BaseStreamsHandler.parseProviderUrl(serverInfo);

        // THEN — https from URL wins over http field; port 8443 from URL wins over 9090 field
        assertEquals("https://example.com:8443", result);
    }

    @Test
    void whenFullUrlPortDiffersFromField_thenUrlPortWins() {
        // GIVEN
        ServerInfo serverInfo = serverInfo("http://example.com:9090", "8080", "", "http");

        // WHEN
        String result = BaseStreamsHandler.parseProviderUrl(serverInfo);

        // THEN — port 9090 from URL wins over 8080 from field
        assertEquals("http://example.com:9090", result);
    }

    // --- blank / missing url ---

    @Test
    void whenUrlIsBlank_thenReturnsEmpty() {
        // GIVEN
        ServerInfo serverInfo = serverInfo("", "8080", "", "http");

        // WHEN
        String result = BaseStreamsHandler.parseProviderUrl(serverInfo);

        // THEN
        assertEquals("", result);
    }

    @Test
    void whenUrlIsNull_thenReturnsEmpty() {
        // GIVEN
        ServerInfo serverInfo = serverInfo(null, "8080", "", "http");

        // WHEN
        String result = BaseStreamsHandler.parseProviderUrl(serverInfo);

        // THEN
        assertEquals("", result);
    }

    // --- malformed port values ---

    @Test
    void whenPortFieldIsNonNumeric_thenPortIsOmitted() {
        // GIVEN
        ServerInfo serverInfo = serverInfo("example.com", "abc", "", "http");

        // WHEN
        String result = BaseStreamsHandler.parseProviderUrl(serverInfo);

        // THEN
        assertEquals("http://example.com", result);
    }

    @Test
    void whenPortFieldIsWhitespaceOnly_thenPortIsOmitted() {
        // GIVEN
        ServerInfo serverInfo = serverInfo("example.com", "   ", "", "http");

        // WHEN
        String result = BaseStreamsHandler.parseProviderUrl(serverInfo);

        // THEN
        assertEquals("http://example.com", result);
    }

    // --- helpers ---

    private static ServerInfo serverInfo(String url, String port, String httpsPort, String serverProtocol) {
        ServerInfo s = new ServerInfo();
        s.setUrl(url);
        s.setPort(port);
        s.setHttpsPort(httpsPort);
        s.setServerProtocol(serverProtocol);
        return s;
    }
}
