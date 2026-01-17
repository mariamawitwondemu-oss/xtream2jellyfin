package uk.humbkr.xtream2jellyfin.xtream.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SeriesEpisodeInfo {

    @JsonProperty("movie_image")
    private String movieImage;

    private String plot;

    private Double rating;

    @JsonProperty("releasedate")
    private String releaseDate;

    @JsonProperty("duration_secs")
    private Integer durationSecs;

    private String duration;

    private Object video;

    private Object audio;

    private Integer bitrate;

}
