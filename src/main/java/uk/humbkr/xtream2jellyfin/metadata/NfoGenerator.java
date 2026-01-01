package uk.humbkr.xtream2jellyfin.metadata;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.humbkr.xtream2jellyfin.common.RegexUtils;
import uk.humbkr.xtream2jellyfin.common.StringUtils;
import uk.humbkr.xtream2jellyfin.common.XmlUtils;
import uk.humbkr.xtream2jellyfin.metadata.nfo.EpisodeNfo;
import uk.humbkr.xtream2jellyfin.metadata.nfo.MovieNfo;
import uk.humbkr.xtream2jellyfin.metadata.nfo.TvShowNfo;
import uk.humbkr.xtream2jellyfin.xtream.model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class NfoGenerator {

    /**
     * Generate NFO XML content for a TV show
     *
     * @param seriesItem The series item metadata from Xtream
     * @param info       The series info metadata from Xtream
     * @return NFO XML content as String, or null if generation fails
     */
    public static String generateTvShowNfo(SeriesItem seriesItem, SeriesInfo info) {
        try {
            TvShowNfo nfo = buildTvShowNfo(seriesItem, info);
            return XmlUtils.getXmlMapper().writeValueAsString(nfo);
        } catch (JsonProcessingException e) {
            log.error("Failed to generate TV show NFO", e);
            return null;
        }
    }

    /**
     * Generate NFO XML content for an episode
     *
     * @param episode The episode metadata from Xtream
     * @return NFO XML content as String, or null if generation fails
     */
    public static String generateEpisodeNfo(SeriesEpisode episode) {
        try {
            EpisodeNfo nfo = buildEpisodeNfo(episode);
            return XmlUtils.getXmlMapper().writeValueAsString(nfo);
        } catch (JsonProcessingException e) {
            log.error("Failed to generate episode NFO", e);
            return null;
        }
    }

    /**
     * Generate NFO file for a movie (typed model version).
     *
     * @param movie The movie item from Xtream
     * @return NFO XML content as String, or null if generation fails
     */
    public static String generateMovieNfo(MovieItem movie) {
        try {
            MovieNfo nfo = buildMovieNfo(movie);
            return XmlUtils.getXmlMapper().writeValueAsString(nfo);
        } catch (JsonProcessingException e) {
            log.error("Failed to generate movie NFO", e);
            return null;
        }
    }

    public static String generateMovieNfo(MovieItem movieItem, MovieDetails movieDetails) {
        MovieNfo movieNfo = buildMovieNfo(movieDetails);
        if (StringUtils.isBlank(movieNfo.getTrailer())) {
            // Override trailer URL from MovieItem if not defined in MovieDetails
            movieNfo.setTrailer(movieItem.getYoutubeTrailerUrl());
        }
        try {
            return XmlUtils.getXmlMapper().writeValueAsString(movieNfo);
        } catch (JsonProcessingException e) {
            log.error("Failed to generate movie NFO", e);
            return null;
        }
    }

    private static TvShowNfo buildTvShowNfo(SeriesItem seriesItem, SeriesInfo info) {
        TvShowNfo.TvShowNfoBuilder builder = TvShowNfo.builder();

        // Title
        String title = seriesItem.getName();
        if (StringUtils.isNotBlank(title)) {
            builder.title(cleanTitle(title));
        }

        // Plot
        String plot = info.getPlot();
        if (StringUtils.isNotBlank(plot)) {
            builder.plot(plot);
        }

        // Premiered
        String premiered = seriesItem.getReleaseDate();
        if (premiered == null) {
            premiered = info.getReleaseDate();
        }
        if (StringUtils.isNotBlank(premiered)) {
            builder.premiered(premiered);
        }

        // Rating
        String rating = info.getRating();
        if (StringUtils.isNotBlank(rating)) {
            try {
                builder.userRating(Double.parseDouble(rating));
            } catch (NumberFormatException e) {
                log.debug("Failed to parse rating: {}", rating);
            }
        }

        // Unique IDs
        List<TvShowNfo.UniqueId> uniqueIds = new ArrayList<>();
        String tmdbId = info.getTmdb();
        if (StringUtils.isNotBlank(tmdbId)) {
            uniqueIds.add(TvShowNfo.UniqueId.builder()
                    .type("tmdb")
                    .isDefault(true)
                    .value(tmdbId)
                    .build());
        }
        if (!uniqueIds.isEmpty()) {
            builder.uniqueIds(uniqueIds);
        }

        // Genres
        String genre = info.getGenre();
        if (StringUtils.isNotBlank(genre)) {
            List<String> genres = Arrays.stream(genre.split("/"))
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toList());
            if (!genres.isEmpty()) {
                builder.genres(genres);
            }
        }

        // Actors
        String cast = info.getCast();
        if (StringUtils.isNotBlank(cast)) {
            List<TvShowNfo.Actor> actors = Arrays.stream(cast.split(","))
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .map(name -> TvShowNfo.Actor.builder().name(name).build())
                    .collect(Collectors.toList());
            if (!actors.isEmpty()) {
                builder.actors(actors);
            }
        }

        // Directors
        String director = info.getDirector();
        if (StringUtils.isNotBlank(director)) {
            List<String> directors = Arrays.stream(director.split(","))
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toList());
            if (!directors.isEmpty()) {
                builder.directors(directors);
            }
        }

        // Runtime
        String episodeRunTime = info.getEpisodeRunTime();
        if (StringUtils.isNotBlank(episodeRunTime)) {
            try {
                builder.runtime(Integer.parseInt(episodeRunTime));
            } catch (NumberFormatException e) {
                log.debug("Failed to parse runtime: {}", episodeRunTime);
            }
        }

        // Trailer
        String youtubeTrailer = info.getYoutubeTrailerUrl();
        if (StringUtils.isNotBlank(youtubeTrailer)) {
            builder.trailer(youtubeTrailer);
        }

        // Thumbs
        List<TvShowNfo.Thumb> thumbs = new ArrayList<>();

        // Add poster from cover
        String cover = info.getCover();
        if (StringUtils.isNotBlank(cover)) {
            thumbs.add(TvShowNfo.Thumb.builder()
                    .aspect("poster")
                    .url(cover)
                    .build());
        }

        // Add fanart from backdrop_path
        List<String> backdropPaths = info.getBackdropPath();
        if (backdropPaths != null && !backdropPaths.isEmpty()) {
            for (String backdropUrl : backdropPaths) {
                if (StringUtils.isNotBlank(backdropUrl)) {
                    thumbs.add(TvShowNfo.Thumb.builder()
                            .aspect("fanart")
                            .url(backdropUrl)
                            .build());
                }
            }
        }

        if (!thumbs.isEmpty()) {
            builder.thumbs(thumbs);
        }

        return builder.build();
    }

    private static EpisodeNfo buildEpisodeNfo(SeriesEpisode episode) {
        EpisodeNfo.EpisodeNfoBuilder builder = EpisodeNfo.builder();

        // Title
        String title = episode.getTitle();
        if (StringUtils.isNotBlank(title)) {
            builder.title(extractEpisodeTitle(title));
        }

        // Season
        Integer season = episode.getSeason();
        if (season != null) {
            builder.season(season);
        }

        // Episode
        Integer episodeNum = episode.getEpisodeNum();
        if (episodeNum != null) {
            builder.episode(episodeNum);
        }

        // Get info
        if (episode.getInfo() != null) {
            var info = episode.getInfo();

            // Aired date
            String airDate = info.getAirDate();
            if (StringUtils.isNotBlank(airDate)) {
                builder.aired(airDate);
            }

            // Rating
            Double rating = info.getRating();
            if (rating != null) {
                builder.userRating(rating);
            }
        }

        return builder.build();
    }

    private static MovieNfo buildMovieNfo(MovieItem movie) {
        MovieNfo.MovieNfoBuilder builder = MovieNfo.builder();

        // Title
        String name = movie.getName();
        if (StringUtils.isNotBlank(name)) {
            String cleanedTitle = cleanTitle(name);
            builder.title(cleanedTitle);
            builder.originalTitle(cleanedTitle);
        }

        // Rating
        String rating = movie.getRating();
        if (StringUtils.isNotBlank(rating)) {
            try {
                builder.userRating(Double.parseDouble(rating));
            } catch (NumberFormatException e) {
                log.debug("Failed to parse rating: {}", rating);
            }
        }

        // Unique IDs
        List<MovieNfo.UniqueId> uniqueIds = new ArrayList<>();

        String tmdbId = movie.getTmdbId();
        if (StringUtils.isNotBlank(tmdbId)) {
            uniqueIds.add(MovieNfo.UniqueId.builder()
                    .type("tmdb")
                    .isDefault(true)
                    .value(tmdbId)
                    .build());
        }

        if (!uniqueIds.isEmpty()) {
            builder.uniqueIds(uniqueIds);
        }

        // Trailer
        String trailerUrl = movie.getYoutubeTrailerUrl();
        if (StringUtils.isNotBlank(trailerUrl)) {
            builder.trailer(trailerUrl);
        }

        // Thumbs (poster from stream_icon)
        List<MovieNfo.Thumb> thumbs = new ArrayList<>();
        String streamIcon = movie.getStreamIcon();
        if (StringUtils.isNotBlank(streamIcon)) {
            thumbs.add(MovieNfo.Thumb.builder()
                    .aspect("poster")
                    .url(streamIcon)
                    .build());
        }
        if (!thumbs.isEmpty()) {
            builder.thumbs(thumbs);
        }

        return builder.build();
    }

    private static MovieNfo buildMovieNfo(MovieDetails movieDetails) {
        if (movieDetails == null || movieDetails.getInfo() == null) {
            return MovieNfo.builder().build();
        }

        var info = movieDetails.getInfo();
        MovieNfo.MovieNfoBuilder builder = MovieNfo.builder();

        // Title
        String name = info.getName();
        if (StringUtils.isNotBlank(name)) {
            String cleanedTitle = cleanTitle(name);
            builder.title(cleanedTitle);
        }

        // Original title
        String originalName = info.getOriginalName();
        if (StringUtils.isNotBlank(originalName)) {
            builder.originalTitle(cleanTitle(originalName));
        } else if (StringUtils.isNotBlank(name)) {
            builder.originalTitle(cleanTitle(name));
        }

        // Plot
        String plot = info.getPlot();
        if (StringUtils.isBlank(plot)) {
            plot = info.getDescription();
        }
        if (StringUtils.isNotBlank(plot)) {
            builder.plot(plot);
        }

        // Premiered and year
        String premiered = info.getReleaseDate();
        if (StringUtils.isNotBlank(premiered)) {
            builder.premiered(premiered);
            if (premiered.length() >= 4) {
                builder.year(premiered.substring(0, 4));
            }
        }

        // Rating
        String rating = info.getRating();
        if (StringUtils.isNotBlank(rating)) {
            try {
                builder.userRating(Double.parseDouble(rating));
            } catch (NumberFormatException e) {
                log.debug("Failed to parse rating: {}", rating);
            }
        }

        // Unique IDs
        List<MovieNfo.UniqueId> uniqueIds = new ArrayList<>();

        String tmdbId = info.getTmdbId();
        if (StringUtils.isNotBlank(tmdbId)) {
            uniqueIds.add(MovieNfo.UniqueId.builder()
                    .type("tmdb")
                    .isDefault(true)
                    .value(tmdbId)
                    .build());
        }

        if (!uniqueIds.isEmpty()) {
            builder.uniqueIds(uniqueIds);
        }

        // Genres
        String genre = info.getGenre();
        if (StringUtils.isNotBlank(genre)) {
            List<String> genres = Arrays.stream(genre.split("/"))
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toList());
            if (!genres.isEmpty()) {
                builder.genres(genres);
            }
        }

        // Actors (prefer 'actors' field, fallback to 'cast')
        String actors = info.getActors();
        if (actors == null || actors.isBlank()) {
            actors = info.getCast();
        }
        if (StringUtils.isNotBlank(actors)) {
            List<MovieNfo.Actor> actorList = Arrays.stream(actors.split(","))
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .map(actorName -> MovieNfo.Actor.builder().name(actorName).build())
                    .collect(Collectors.toList());
            if (!actorList.isEmpty()) {
                builder.actors(actorList);
            }
        }

        // Directors
        String director = info.getDirector();
        if (StringUtils.isNotBlank(director)) {
            List<String> directors = Arrays.stream(director.split(","))
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toList());
            if (!directors.isEmpty()) {
                builder.directors(directors);
            }
        }

        // Runtime (from durationSecs)
        Integer durationSecs = info.getDurationSecs();
        if (durationSecs != null && durationSecs > 0) {
            builder.runtime(durationSecs / 60); // Convert seconds to minutes
        }

        return builder.build();
    }

    /**
     * Clean title by removing metadata tags like (MULTI), [4K], etc.
     */
    private static String cleanTitle(String title) {
        if (title == null) {
            return null;
        }

        // Remove patterns like (MULTI), [4K], |IMAX UHD|, etc.
        title = RegexUtils.replaceAll(title, "\\s*\\([^)]*\\)\\s*$", ""); // Remove trailing (...)
        title = RegexUtils.replaceAll(title, "\\s*\\[[^]]*\\]\\s*$", ""); // Remove trailing [...]
        title = RegexUtils.replaceAll(title, "^\\|[^|]*\\|\\s*", "");     // Remove leading |...|
        return title.trim();
    }

    /**
     * Extract episode title from full title format like "Series Name - S01E01 - Episode Title"
     */
    private static String extractEpisodeTitle(String fullTitle) {
        if (fullTitle == null) {
            return null;
        }
        // Try to extract title after the last dash
        int lastDash = fullTitle.lastIndexOf(" - ");
        if (lastDash > 0 && lastDash < fullTitle.length() - 3) {
            String episodeTitle = fullTitle.substring(lastDash + 3);
            if (StringUtils.isNotBlank(episodeTitle)) {
                return episodeTitle.trim();
            }
        }
        return fullTitle;
    }

}
