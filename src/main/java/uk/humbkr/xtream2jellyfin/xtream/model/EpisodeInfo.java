package uk.humbkr.xtream2jellyfin.xtream.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class EpisodeInfo {

    @JsonProperty("tmdb_id")
    private Integer tmdbId;

    private String releasedate;

    private String plot;

    @JsonProperty("duration_secs")
    private Integer durationSecs;

    private String duration;

    @JsonProperty("movie_image")
    private String movieImage;

    private Map<String, Object> video;

    private Map<String, Object> audio;

    private Integer bitrate;

    private Double rating;

    private String season;

}
