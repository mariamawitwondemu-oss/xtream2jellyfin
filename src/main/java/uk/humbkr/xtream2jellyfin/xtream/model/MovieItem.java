package uk.humbkr.xtream2jellyfin.xtream.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MovieItem implements StreamItem {

    private Integer num;

    private String name;

    @JsonProperty("stream_id")
    private Integer streamId;

    @JsonProperty("stream_type")
    private String streamType;

    @JsonProperty("stream_icon")
    private String streamIcon;

    private String rating;

    @JsonProperty("rating_5based")
    private Double rating5based;

    private Long added;

    @JsonProperty("category_id")
    private String categoryId;

    @JsonProperty("container_extension")
    private String containerExtension;

    @JsonProperty("custom_sid")
    private String customSid;

    @JsonProperty("direct_source")
    private String directSource;

    private String trailer;

    @JsonProperty("tmdb")
    private String tmdbId;

    public String getYoutubeTrailerUrl() {
        return ModelUtils.getYoutubeTrailerUrl(trailer);
    }

}
