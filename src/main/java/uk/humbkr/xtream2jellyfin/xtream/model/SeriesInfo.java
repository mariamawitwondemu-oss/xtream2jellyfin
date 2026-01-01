package uk.humbkr.xtream2jellyfin.xtream.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class SeriesInfo {

    private String name;

    private String cover;

    private String plot;

    private String cast;

    private String director;

    private String genre;

    @JsonProperty("release_date")
    private String releaseDate;

    @JsonProperty("last_modified")
    private String lastModified;

    private String rating;

    @JsonProperty("rating_5based")
    private Double rating5based;

    @JsonProperty("backdrop_path")
    private List<String> backdropPath;

    @JsonProperty("youtube_trailer")
    private String youtubeTrailer;

    private String tmdb;

    @JsonProperty("episode_run_time")
    private String episodeRunTime;

    @JsonProperty("category_id")
    private String categoryId;

    public String getYoutubeTrailerUrl() {
        return ModelUtils.getYoutubeTrailerUrl(youtubeTrailer);
    }

}
