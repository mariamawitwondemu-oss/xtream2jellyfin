package uk.humbkr.xtream2jellyfin.xtream.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class MovieInfo {

    @JsonProperty("tmdb_id")
    private String tmdbId;

    private String name;

    @JsonProperty("o_name")
    private String originalName;

    @JsonProperty("cover_big")
    private String coverBig;

    @JsonProperty("movie_image")
    private String movieImage;

    @JsonProperty("releasedate")
    private String releaseDate;

    @JsonProperty("youtube_trailer")
    private String youtubeTrailer;

    private String director;

    private String actors;

    private String cast;

    private String description;

    private String plot;

    private String age;

    private String country;

    private String genre;

    @JsonProperty("backdrop_path")
    private List<String> backdropPath;

    @JsonProperty("duration_secs")
    private Integer durationSecs;

    private String duration;

    private String bitrate;

    private String rating;

    private String status;

    public String getYoutubeTrailerUrl() {
        return ModelUtils.getYoutubeTrailerUrl(youtubeTrailer);
    }

}
