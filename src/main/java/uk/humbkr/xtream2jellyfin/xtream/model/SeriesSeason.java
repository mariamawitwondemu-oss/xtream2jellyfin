package uk.humbkr.xtream2jellyfin.xtream.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SeriesSeason {

    private String name;

    @JsonProperty("episode_count")
    private Integer episodeCount;

    private String overview;

    @JsonProperty("air_date")
    private String airDate;

    private String cover;

    @JsonProperty("cover_tmdb")
    private String coverTmdb;

    @JsonProperty("season_number")
    private Integer seasonNumber;

    @JsonProperty("cover_big")
    private String coverBig;

}
