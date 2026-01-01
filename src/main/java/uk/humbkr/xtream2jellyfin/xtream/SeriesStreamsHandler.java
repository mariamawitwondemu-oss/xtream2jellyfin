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
import java.util.Collection;
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

    private String getSeriesFolderPath(SeriesItem seriesItem, SeriesInfo seriesInfo) {
        String seriesName = seriesItem.getName();
        String categoryId = String.valueOf(seriesItem.getCategoryId());

        String seriesCategory = categories.get(categoryId);

        StreamNameFormatContext context = StreamNameFormatContext.builder()
                .externalProviderId("tmdbid")
                .externalId(seriesInfo.getTmdb())
                .build();

        seriesName = mediaNameFormat.format(seriesName, context);

        Collection<String> folderPath = new ArrayList<>();
        folderPath.add(mediaDir);

        if (categoryFolder) {
            folderPath.add(seriesCategory);
        }

        folderPath.add(seriesName);

        return String.join("/", folderPath);
    }

    private void processSeriesStream(SeriesItem seriesItem) {
        try {
            String seriesId = String.valueOf(seriesItem.getSeriesId());

            logDebug("Updating stream for #" + seriesId);

            SeriesDetails details = getData(Endpoint.PLAYER, Action.SERIES_INFO, seriesId, SeriesDetails.class);

            if (details != null) {
                SeriesInfo info = details.getInfo();

                // Generate and write tvshow.nfo
                String seriesFolderPath = getSeriesFolderPath(seriesItem, info);
                if (writeMetadataNfo) {
                    String nfoPath = seriesFolderPath + "/tvshow.nfo";
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
                            processEpisode(seriesFolderPath, episode);
                        }
                    }
                }
            }

        } catch (Exception ex) {
            logError("Failed to process stream, Series: " + seriesItem.getName() + ", Error: " + ex.getMessage(), ex);
        }
    }

    private void processEpisode(String seriesFolderPath, SeriesEpisode episode) {
        String seriesName = StringUtils.substringAfterLast(seriesFolderPath, "/");

        try {
            String streamId = episode.getId();
            int episodeNumber = episode.getEpisodeNum();
            int seasonNumber = episode.getSeason();
            String containerExtension = episode.getContainerExtension();

            String seasonPad = String.format("%02d", seasonNumber);
            String episodeShort = String.format("%02d", episodeNumber);

            String episodeFile = String.format("%s - S%sE%s", seriesName, seasonPad, episodeShort);

            String seasonDir = "Season " + seasonPad;

            String episodeFilePath = seriesFolderPath + "/" + seasonDir + "/" + episodeFile + ".strm";

            String episodeStreamUrl = buildStreamUrl(streamId, containerExtension);

            addFile(episodeFilePath, episodeStreamUrl);

            // Generate and write episode NFO
            if (writeMetadataNfo) {
                String episodeNfoPath = seriesFolderPath + "/" + seasonDir + "/" + episodeFile + ".nfo";
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
