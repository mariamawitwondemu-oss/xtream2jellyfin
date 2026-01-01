package uk.humbkr.xtream2jellyfin.xtream.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SeriesEpisode {

    private String id;

    @JsonProperty("episode_num")
    private Integer episodeNum;

    private String title;

    @JsonProperty("container_extension")
    private String containerExtension;

    private SeriesEpisodeInfo info;

    @JsonProperty("custom_sid")
    private String customSid;

    private String added;

    private Integer season;

    @JsonProperty("direct_source")
    private String directSource;

}
