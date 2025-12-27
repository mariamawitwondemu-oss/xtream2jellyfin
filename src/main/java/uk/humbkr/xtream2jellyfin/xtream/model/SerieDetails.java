package uk.humbkr.xtream2jellyfin.xtream.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class SerieDetails {

    private List<SerieSeason> seasons;

    private SerieInfo info;

    private Map<String, List<SerieEpisode>> episodes;

}
