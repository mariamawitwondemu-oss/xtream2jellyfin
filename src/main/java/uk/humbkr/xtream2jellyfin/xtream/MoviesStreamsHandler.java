package uk.humbkr.xtream2jellyfin.xtream;

import lombok.extern.slf4j.Slf4j;
import uk.humbkr.xtream2jellyfin.config.AppSettings;
import uk.humbkr.xtream2jellyfin.config.XtreamProviderConfig;
import uk.humbkr.xtream2jellyfin.filemanager.FileManager;
import uk.humbkr.xtream2jellyfin.http.ConfigurableHttpClient;
import uk.humbkr.xtream2jellyfin.metadata.NfoGenerator;
import uk.humbkr.xtream2jellyfin.nameformat.StreamNameFormatContext;
import uk.humbkr.xtream2jellyfin.xtream.model.*;

import java.util.ArrayList;
import java.util.Collection;

@Slf4j
public class MoviesStreamsHandler extends BaseStreamsHandler {

    public MoviesStreamsHandler(XtreamProviderConfig providerConfig,
                                FileManager fileManager,
                                AppSettings appSettings,
                                ConfigurableHttpClient httpClient) {
        super(providerConfig, fileManager, appSettings, httpClient, log);
    }

    @Override
    public MediaType getMediaType() {
        return MediaType.MOVIE;
    }

    @Override
    protected void processItem(Object item) {
        MovieItem movieItem = (MovieItem) item;
        processMovieStream(movieItem);
    }

    private void processMovieStream(MovieItem movieItem) {
        String movieName = movieItem.getName();
        String categoryId = movieItem.getCategoryId();

        String movieId = String.valueOf(movieItem.getStreamId());
        String containerExtension = movieItem.getContainerExtension();
        String movieCategory = categories.get(categoryId);

        StreamNameFormatContext context = StreamNameFormatContext.builder()
                .externalProviderId("tmdbid")
                .externalId(movieItem.getTmdbId())
                .build();

        movieName = mediaNameFormat.format(movieName, context);

        Collection<String> movieFolder = new ArrayList<>();
        movieFolder.add(mediaDir + "s");

        if (categoryFolder) {
            movieFolder.add(movieCategory);
        }

        movieFolder.add(movieName);

        String moviePath = String.join("/", movieFolder) + "/" + movieName;

        String streamFile = moviePath + ".strm";
        String streamUrl = buildStreamUrl(movieId, containerExtension);

        addFile(streamFile, streamUrl);

        // Generate and write movie NFO
        if (writeMetadataNfo) {
            MovieDetails movieDetails = getData(Endpoint.PLAYER, Action.VOD_INFO, movieId, MovieDetails.class);
            String nfoFile = moviePath + ".nfo";
            String nfoContent = NfoGenerator.generateMovieNfo(movieItem, movieDetails);
            if (nfoContent != null) {
                addFile(nfoFile, nfoContent);
            }
        }
    }

}
