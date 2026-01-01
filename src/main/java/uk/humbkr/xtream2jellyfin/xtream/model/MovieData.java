package uk.humbkr.xtream2jellyfin.xtream.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class MovieData {

    @JsonProperty("stream_id")
    private Integer streamId;

    private String name;

    private Long added;

    @JsonProperty("category_id")
    private String categoryId;

    @JsonProperty("category_ids")
    private List<Integer> categoryIds;

    @JsonProperty("container_extension")
    private String containerExtension;

    @JsonProperty("custom_sid")
    private String customSid;

    @JsonProperty("direct_source")
    private String directSource;

}
