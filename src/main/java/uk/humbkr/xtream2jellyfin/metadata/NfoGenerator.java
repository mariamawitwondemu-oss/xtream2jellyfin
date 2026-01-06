package uk.humbkr.xtream2jellyfin.metadata;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.humbkr.xtream2jellyfin.common.RegexUtils;
import uk.humbkr.xtream2jellyfin.common.StringUtils;
import uk.humbkr.xtream2jellyfin.common.XmlUtils;
import uk.humbkr.xtream2jellyfin.metadata.nfo.EpisodeNfo;
import uk.humbkr.xtream2jellyfin.metadata.nfo.MovieNfo;
import uk.humbkr.xtream2jellyfin.metadata.nfo.SeasonNfo;
import uk.humbkr.xtream2jellyfin.metadata.nfo.TvShowNfo;
import uk.humbkr.xtream2jellyfin.validation.DomainValidator;
import uk.humbkr.xtream2jellyfin.validation.UrlFilterUtils;
import uk.humbkr.xtream2jellyfin.xtream.model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class NfoGenerator {

    /**
     * Generate NFO XML content for a TV show with domain validation
     *
     * @param seriesItem The series item metadata from Xtream
     * @param info       The series info metadata from Xtream
     * @param validator  The domain validator to filter URLs
     * @return NFO XML content as String, or null if generation fails
     */
    public static String generateTvShowNfo(SeriesItem seriesItem, SeriesInfo info, DomainValidator validator) {
        try {
            TvShowNfo nfo = buildTvShowNfo(seriesItem, info, validator);
            return XmlUtils.getXmlMapper().writeValueAsString(nfo);
        } catch (JsonProcessingException e) {
            log.error("Failed to generate TV show NFO", e);
            return null;
        }
    }

    /**
     * Generate NFO XML content for an episode with domain validation
     *
     * @param episode   The episode metadata from Xtream
     * @param validator The domain validator to filter URLs
     * @return NFO XML content as String, or null if generation fails
     */
    public static String generateEpisodeNfo(SeriesEpisode episode, DomainValidator validator) {
        try {
            EpisodeNfo nfo = buildEpisodeNfo(episode, validator);
            return XmlUtils.getXmlMapper().writeValueAsString(nfo);
        } catch (JsonProcessingException e) {
            log.error("Failed to generate episode NFO", e);
            return null;
        }
    }

    public static String generateMovieNfo(MovieItem movieItem, MovieDetails movieDetails, DomainValidator validator) {
        MovieNfo movieNfo = buildMovieNfo(movieDetails, validator);
        if (StringUtils.isBlank(movieNfo.getTrailer())) {
            // Override trailer URL from MovieItem (trailers are not validated)
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
            List<String> genres = Arrays.stream(RegexUtils.split(genre, "/"))
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
            List<TvShowNfo.Actor> actors = Arrays.stream(RegexUtils.split(cast, ","))
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
            List<String> directors = Arrays.stream(RegexUtils.split(director, ","))
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
            String airDate = info.getReleaseDate();
            if (StringUtils.isNotBlank(airDate)) {
                builder.aired(airDate);
            }

            // Rating
            Double rating = info.getRating();
            if (rating != null) {
                builder.userRating(rating);
            }

            // Thumbs - episode screenshot/poster
            List<EpisodeNfo.Thumb> thumbs = new ArrayList<>();
            String movieImage = info.getMovieImage();
            if (StringUtils.isNotBlank(movieImage)) {
                thumbs.add(EpisodeNfo.Thumb.builder()
                        .aspect("thumb")
                        .url(movieImage)
                        .build());
            }
            if (!thumbs.isEmpty()) {
                builder.thumbs(thumbs);
            }
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
            List<String> genres = Arrays.stream(RegexUtils.split(genre, "/"))
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
            List<MovieNfo.Actor> actorList = Arrays.stream(RegexUtils.split(actors, ","))
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
            List<String> directors = Arrays.stream(RegexUtils.split(director, ","))
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

    /**
     * Generate NFO XML content for a season with domain validation
     *
     * @param season    The season metadata from Xtream
     * @param tmdbId    The TMDB ID for the series
     * @param validator The domain validator to filter URLs
     * @return NFO XML content as String, or null if generation fails
     */
    public static String generateSeasonNfo(SeriesSeason season, String tmdbId, DomainValidator validator) {
        try {
            SeasonNfo nfo = buildSeasonNfo(season, tmdbId, validator);
            return XmlUtils.getXmlMapper().writeValueAsString(nfo);
        } catch (JsonProcessingException e) {
            log.error("Failed to generate season NFO", e);
            return null;
        }
    }

    private static SeasonNfo buildSeasonNfo(SeriesSeason season, String tmdbId) {
        SeasonNfo.SeasonNfoBuilder builder = SeasonNfo.builder();

        // Title
        if (StringUtils.isNotBlank(season.getName())) {
            builder.title(season.getName());
        }

        // Season number
        if (season.getSeasonNumber() != null) {
            builder.seasonNumber(season.getSeasonNumber());
        }

        // Premiered (air date)
        if (StringUtils.isNotBlank(season.getAirDate())) {
            builder.premiered(season.getAirDate());

            // Extract year from air date (assuming format YYYY-MM-DD)
            try {
                String year = season.getAirDate().substring(0, 4);
                builder.year(Integer.parseInt(year));
            } catch (Exception e) {
                log.debug("Failed to parse year from air date: {}", season.getAirDate());
            }
        }

        // Unique IDs
        List<SeasonNfo.UniqueId> uniqueIds = new ArrayList<>();
        if (StringUtils.isNotBlank(tmdbId)) {
            uniqueIds.add(SeasonNfo.UniqueId.builder()
                    .type("tmdb")
                    .isDefault(true)
                    .value(tmdbId)
                    .build());
        }

        if (!uniqueIds.isEmpty()) {
            builder.uniqueIds(uniqueIds);
        }

        // Thumbs
        List<SeasonNfo.Thumb> thumbs = new ArrayList<>();
        if (StringUtils.isNotBlank(season.getCoverTmdb())) {
            thumbs.add(SeasonNfo.Thumb.builder()
                    .aspect("poster")
                    .url(season.getCoverTmdb())
                    .build());
        }

        if (!thumbs.isEmpty()) {
            builder.thumbs(thumbs);
        }

        return builder.build();
    }

    // ========== Overloaded build methods with DomainValidator ==========

    private static TvShowNfo buildTvShowNfo(SeriesItem seriesItem, SeriesInfo info, DomainValidator validator) {
        TvShowNfo nfo = buildTvShowNfo(seriesItem, info);

        // Filter thumbs (trailers are NOT validated)
        if (nfo.getThumbs() != null) {
            List<TvShowNfo.Thumb> filteredThumbs = filterTvShowThumbs(nfo.getThumbs(), validator);
            nfo.setThumbs(filteredThumbs.isEmpty() ? null : filteredThumbs);
        }

        return nfo;
    }

    private static EpisodeNfo buildEpisodeNfo(SeriesEpisode episode, DomainValidator validator) {
        EpisodeNfo nfo = buildEpisodeNfo(episode);

        // Filter thumbs
        if (nfo.getThumbs() != null) {
            List<EpisodeNfo.Thumb> filteredThumbs = filterEpisodeThumbs(nfo.getThumbs(), validator);
            nfo.setThumbs(filteredThumbs.isEmpty() ? null : filteredThumbs);
        }

        return nfo;
    }

    private static MovieNfo buildMovieNfo(MovieDetails movieDetails, DomainValidator validator) {
        MovieNfo nfo = buildMovieNfo(movieDetails);

        // Filter thumbs (trailers are NOT validated)
        if (nfo.getThumbs() != null) {
            List<MovieNfo.Thumb> filteredThumbs = filterMovieThumbs(nfo.getThumbs(), validator);
            nfo.setThumbs(filteredThumbs.isEmpty() ? null : filteredThumbs);
        }

        return nfo;
    }

    private static SeasonNfo buildSeasonNfo(SeriesSeason season, String tmdbId, DomainValidator validator) {
        SeasonNfo nfo = buildSeasonNfo(season, tmdbId);

        // Filter thumbs
        if (nfo.getThumbs() != null) {
            List<SeasonNfo.Thumb> filteredThumbs = filterSeasonThumbs(nfo.getThumbs(), validator);
            nfo.setThumbs(filteredThumbs.isEmpty() ? null : filteredThumbs);
        }

        return nfo;
    }

    // ========== Helper methods to filter thumbs ==========

    private static List<TvShowNfo.Thumb> filterTvShowThumbs(List<TvShowNfo.Thumb> thumbs, DomainValidator validator) {
        if (validator == null || thumbs == null) {
            return thumbs;
        }
        return thumbs.stream()
                .filter(t -> UrlFilterUtils.isUrlWithValidDomain(t.getUrl(), validator))
                .collect(Collectors.toList());
    }

    private static List<EpisodeNfo.Thumb> filterEpisodeThumbs(List<EpisodeNfo.Thumb> thumbs, DomainValidator validator) {
        if (validator == null || thumbs == null) {
            return thumbs;
        }
        return thumbs.stream()
                .filter(t -> UrlFilterUtils.isUrlWithValidDomain(t.getUrl(), validator))
                .collect(Collectors.toList());
    }

    private static List<MovieNfo.Thumb> filterMovieThumbs(List<MovieNfo.Thumb> thumbs, DomainValidator validator) {
        if (validator == null || thumbs == null) {
            return thumbs;
        }
        return thumbs.stream()
                .filter(t -> UrlFilterUtils.isUrlWithValidDomain(t.getUrl(), validator))
                .collect(Collectors.toList());
    }

    private static List<SeasonNfo.Thumb> filterSeasonThumbs(List<SeasonNfo.Thumb> thumbs, DomainValidator validator) {
        if (validator == null || thumbs == null) {
            return thumbs;
        }
        return thumbs.stream()
                .filter(t -> UrlFilterUtils.isUrlWithValidDomain(t.getUrl(), validator))
                .collect(Collectors.toList());
    }

}
