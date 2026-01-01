package uk.humbkr.xtream2jellyfin.xtream;

import lombok.extern.slf4j.Slf4j;
import uk.humbkr.xtream2jellyfin.common.StringUtils;
import uk.humbkr.xtream2jellyfin.config.AppSettings;
import uk.humbkr.xtream2jellyfin.config.XtreamProviderConfig;
import uk.humbkr.xtream2jellyfin.filemanager.FileManager;
import uk.humbkr.xtream2jellyfin.http.ConfigurableHttpClient;
import uk.humbkr.xtream2jellyfin.metadata.NfoGenerator;
import uk.humbkr.xtream2jellyfin.nameformat.StreamNameFormatContext;
import uk.humbkr.xtream2jellyfin.xtream.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class SeriesStreamsHandler extends BaseStreamsHandler {

    public SeriesStreamsHandler(XtreamProviderConfig providerConfig, FileManager fileManager,
                                AppSettings appSettings, ConfigurableHttpClient httpClient) {
        super(providerConfig, fileManager, appSettings, httpClient, log);
    }

    @Override
    public MediaType getMediaType() {
        return MediaType.SERIES;
    }

    @Override
    protected void processItem(Object item) {
        processSeriesStream((SeriesItem) item);
    }

    private String getSeriesBasePath(SeriesItem seriesItem, SeriesInfo info) {
        String seriesName = seriesItem.getName();
        String categoryId = String.valueOf(seriesItem.getCategoryId());

        String seriesCategory = categories.get(categoryId);

        // Format series name with Jellyfin-compatible naming
        String tmdbId = (info != null && info.getCategoryId() != null) ? info.getCategoryId() : null;
        String tvdbId = null;

        // Prefer TVDB ID for series, fallback to TMDB
        String externalProviderId = null;
        String externalId = null;
        if (tvdbId != null && !tvdbId.isEmpty()) {
            externalProviderId = "tvdbid";
            externalId = tvdbId;
        } else if (tmdbId != null && !tmdbId.isEmpty()) {
            externalProviderId = "tmdbid";
            externalId = tmdbId;
        }

        StreamNameFormatContext context = StreamNameFormatContext.builder()
                .year(seriesItem.getReleaseDate() != null ? extractYearFromDate(seriesItem.getReleaseDate()) : null)
                .externalProviderId(externalProviderId)
                .externalId(externalId)
                .build();

        String seriesNameClean = mediaNameFormat.format(seriesName, context);

        if (!seriesName.equals(seriesNameClean)) {
            logDebug("Cleaned series name: '" + seriesName + "' to '" + seriesNameClean + "'");
        }

        List<String> basePathParts = new ArrayList<>();
        basePathParts.add(mediaDir);

        if (categoryFolder) {
            basePathParts.add(seriesCategory);
        }

        basePathParts.add(seriesNameClean);

        return String.join("/", basePathParts) + "/" + seriesNameClean;
    }

    private void processSeriesStream(SeriesItem seriesItem) {
        try {
            String seriesId = String.valueOf(seriesItem.getSeriesId());

            logDebug("Updating stream for #" + seriesId);

            SeriesDetails details = getData(Endpoint.PLAYER, Action.SERIES_INFO, seriesId, SeriesDetails.class);

            if (details != null) {
                SeriesInfo info = details.getInfo();

                long addedTimestamp = Long.parseLong(info.getLastModified());

                // Generate and write tvshow.nfo
                String basePath = getSeriesBasePath(seriesItem, info);
                if (writeMetadataNfo) {
                    String nfoPath = basePath + "/tvshow.nfo";
                    String nfoContent = NfoGenerator.generateTvShowNfo(seriesItem, info);
                    if (nfoContent != null) {
                        addFile(nfoPath, nfoContent);
                    }
                }

                Map<String, List<SeriesEpisode>> episodesData = details.getEpisodes();

                if (episodesData != null) {
                    for (Map.Entry<String, List<SeriesEpisode>> seasonEntry : episodesData.entrySet()) {
                        List<SeriesEpisode> seasonData = seasonEntry.getValue();

                        for (SeriesEpisode episode : seasonData) {
                            processEpisode(basePath, episode);
                        }
                    }
                }
            }

        } catch (Exception ex) {
            logError("Failed to process stream, Series: " + seriesItem.getName() + ", Error: " + ex.getMessage(), ex);
        }
    }

    private void processEpisode(String basePath, SeriesEpisode episode) {
        String seriesName = StringUtils.substringAfterLast(basePath, "/");

        try {
            String streamId = episode.getId();
            int episodeNumber = episode.getEpisodeNum();
            int seasonNumber = episode.getSeason();
            String containerExtension = episode.getContainerExtension();

            String seasonPad = String.format("%02d", seasonNumber);
            String episodeShort = String.format("%02d", episodeNumber);

            String episodeFile = String.format("%s - S%sE%s", seriesName, seasonPad, episodeShort);

            String seasonDir = "Season " + seasonPad;

            String episodeFilePath = basePath + "/" + seasonDir + "/" + episodeFile + ".strm";

            String episodeStreamUrl = buildStreamUrl(streamId, containerExtension);

            addFile(episodeFilePath, episodeStreamUrl);

            // Generate and write episode NFO
            if (writeMetadataNfo) {
                String episodeNfoPath = basePath + "/" + seasonDir + "/" + episodeFile + ".nfo";
                String episodeNfoContent = NfoGenerator.generateEpisodeNfo(episode);
                if (episodeNfoContent != null) {
                    addFile(episodeNfoPath, episodeNfoContent);
                }
            }

        } catch (Exception ex) {
            logError("Failed to process series, Series: " + seriesName +
                    ", Episode: " + episode.getTitle() + ", Error: " + ex.getMessage(), ex);
        }
    }

    private String extractYearFromDate(String date) {
        if (date != null && date.length() >= 4) {
            return date.substring(0, 4);
        }
        return null;
    }

}
