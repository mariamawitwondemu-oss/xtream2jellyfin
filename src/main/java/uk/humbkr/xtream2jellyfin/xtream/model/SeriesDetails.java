package uk.humbkr.xtream2jellyfin.xtream.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class SeriesDetails {

    private List<SeriesSeason> seasons;

    private SeriesInfo info;

    @JsonDeserialize(using = SeriesEpisodesDeserializer.class)
    private Map<String, List<SeriesEpisode>> episodes;

}
