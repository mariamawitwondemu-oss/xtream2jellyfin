package uk.humbkr.xtream2jellyfin.xtream.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SeriesEpisodeInfo {

    private Integer id;

    @JsonProperty("air_date")
    private String airDate;

    @JsonProperty("movie_image")
    private String movieImage;

    @JsonProperty("duration_secs")
    private Integer durationSecs;

    private String duration;

    private Object video;

    private Object audio;

    private Integer bitrate;

    private Double rating;

}
