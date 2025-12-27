package uk.humbkr.xtream2jellyfin.xtream;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import uk.humbkr.xtream2jellyfin.config.AppSettings;
import uk.humbkr.xtream2jellyfin.config.XtreamProviderConfig;
import uk.humbkr.xtream2jellyfin.filemanager.FileManager;
import uk.humbkr.xtream2jellyfin.metadata.NfoGenerator;
import uk.humbkr.xtream2jellyfin.nameformat.StreamNameFormatContext;
import uk.humbkr.xtream2jellyfin.xtream.model.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class SeriesStreamsHandler extends BaseStreamsHandler {

    public SeriesStreamsHandler(XtreamProviderConfig providerConfig, FileManager fileManager, AppSettings appSettings) {
        super(providerConfig, fileManager, appSettings, log);
    }

    @Override
    public MediaType getMediaType() {
        return MediaType.SERIES;
    }

    @Override
    protected void processItem(Object item) {
        processSeriesStream((SerieItem) item);
    }

    private String getStreamInfoPath(SerieItem serieItem, SerieInfo info) {
        String seriesName = serieItem.getName();
        String categoryId = String.valueOf(serieItem.getCategoryId());

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
                .year(serieItem.getReleaseDate() != null ? extractYearFromDate(serieItem.getReleaseDate()) : null)
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
        String basePath = String.join("/", basePathParts);

        return basePath + "/" + seriesNameClean + ".json";
    }

    private void processSeriesStream(SerieItem serieItem) {
        try {
            String seriesId = String.valueOf(serieItem.getSeriesId());

            logDebug("Updating stream for #" + seriesId);

            SerieDetails details = getData(Endpoint.PLAYER, Action.SERIES_INFO, seriesId, SerieDetails.class);

            if (details != null) {
                SerieInfo info = details.getInfo();

                long addedTimestamp = Long.parseLong(info.getLastModified());
                Instant date = Instant.ofEpochSecond(addedTimestamp);

                String streamInfoPath = getStreamInfoPath(serieItem, info);

                logDebug("processing series stream: " + streamInfoPath);

                if (writeMetadataJson) {
                    addFile(streamInfoPath, details, date);
                }

                // Generate and write tvshow.nfo
                String basePath = StringUtils.substringBeforeLast(streamInfoPath, "/");
                if (writeMetadataNfo) {
                    String nfoPath = basePath + "/tvshow.nfo";
                    String nfoContent = NfoGenerator.generateTvShowNfo(serieItem, info);
                    if (nfoContent != null) {
                        addFile(nfoPath, nfoContent, date);
                    }
                }

                Map<String, List<SerieEpisode>> episodesData = details.getEpisodes();

                if (episodesData != null) {
                    for (Map.Entry<String, List<SerieEpisode>> seasonEntry : episodesData.entrySet()) {
                        List<SerieEpisode> seasonData = seasonEntry.getValue();

                        for (SerieEpisode episode : seasonData) {
                            processEpisode(basePath, episode);
                        }
                    }
                }
            }

        } catch (Exception ex) {
            logError("Failed to process stream, Series: " + serieItem.getName() + ", Error: " + ex.getMessage(), ex);
        }
    }

    private void processEpisode(String basePath, SerieEpisode episode) {
        String seriesName = StringUtils.substringAfterLast(basePath, "/");

        try {
            String streamId = episode.getId();
            int episodeNumber = episode.getEpisodeNum();
            int seasonNumber = episode.getSeason();
            String containerExtension = episode.getContainerExtension();

            long addedTimestamp = Long.parseLong(episode.getAdded());

            String seasonPad = String.format("%02d", seasonNumber);
            String episodeShort = String.format("%02d", episodeNumber);

            String episodeFile = String.format("%s - S%sE%s", seriesName, seasonPad, episodeShort);

            String seasonDir = "Season " + seasonPad;

            String episodeFilePath = basePath + "/" + seasonDir + "/" + episodeFile + ".strm";

            String episodeStreamUrl = buildStreamUrl(streamId, containerExtension);

            Instant date = Instant.ofEpochSecond(addedTimestamp);

            addFile(episodeFilePath, episodeStreamUrl, date);

            // Generate and write episode NFO
            if (writeMetadataNfo) {
                String episodeNfoPath = basePath + "/" + seasonDir + "/" + episodeFile + ".nfo";
                String episodeNfoContent = NfoGenerator.generateEpisodeNfo(episode);
                if (episodeNfoContent != null) {
                    addFile(episodeNfoPath, episodeNfoContent, date);
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
