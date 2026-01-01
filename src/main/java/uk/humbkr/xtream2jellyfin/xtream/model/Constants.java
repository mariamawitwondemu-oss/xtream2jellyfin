package uk.humbkr.xtream2jellyfin.xtream.model;

import java.util.Map;

public class Constants {

    // Context Parameters
    public static final Map<Action, String> CONTEXT_PARAMETER = Map.of(
            Action.SERIES_INFO, "series_id",
            Action.VOD_INFO, "vod_id",
            Action.EPG_INFO, "stream_id"
    );

    // Media Resolver Keys
    public static final String MEDIA_RESOLVER_CATEGORIES = "categories";

    public static final String MEDIA_RESOLVER_STREAMS = "streams";

    public static final String MEDIA_RESOLVER_INFO = "info";

    // Media Resolvers
    public static final Map<MediaType, Map<String, Action>> MEDIA_RESOLVERS = Map.of(
            MediaType.LIVE, Map.of(
                    MEDIA_RESOLVER_STREAMS, Action.LIVE_STREAMS,
                    MEDIA_RESOLVER_CATEGORIES, Action.LIVE_CATEGORIES,
                    MEDIA_RESOLVER_INFO, Action.EPG_INFO
            ),
            MediaType.MOVIE, Map.of(
                    MEDIA_RESOLVER_STREAMS, Action.VOD_STREAMS,
                    MEDIA_RESOLVER_CATEGORIES, Action.VOD_CATEGORIES,
                    MEDIA_RESOLVER_INFO, Action.VOD_INFO
            ),
            MediaType.SERIES, Map.of(
                    MEDIA_RESOLVER_STREAMS, Action.SERIES_STREAMS,
                    MEDIA_RESOLVER_CATEGORIES, Action.SERIES_CATEGORIES,
                    MEDIA_RESOLVER_INFO, Action.SERIES_INFO
            )
    );

    // Load Data Actions
    public static final String[] LOAD_DATA_ACTIONS = {
            MEDIA_RESOLVER_STREAMS,
            MEDIA_RESOLVER_CATEGORIES
    };

    // HTTP Headers
    public static final String DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/117.0.0.0 Safari/537.36";

    public static final Map<String, String> HEADERS = Map.of(
            "Upgrade-Insecure-Requests", "1",
            "User-Agent", DEFAULT_USER_AGENT
    );

    // Various Constants

    public static final String YOUTUBE_VIDEO_URL = "https://www.youtube.com/watch?v=";

}
