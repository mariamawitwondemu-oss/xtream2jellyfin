package uk.humbkr.xtream2jellyfin.xtream.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SerieSeason {

    @JsonProperty("air_date")
    private String airDate;

    @JsonProperty("episode_count")
    private Integer episodeCount;

    private Integer id;

    private String name;

    private String overview;

    @JsonProperty("season_number")
    private Integer seasonNumber;

    private String cover;

    @JsonProperty("cover_big")
    private String coverBig;

}
