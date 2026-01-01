package uk.humbkr.xtream2jellyfin.xtream.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SeriesEpisodesDeserializer extends JsonDeserializer<Map<String, List<SeriesEpisode>>> {

    @Override
    public Map<String, List<SeriesEpisode>> deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (parser.currentToken() == JsonToken.START_OBJECT) {
            // Standard map format: {"1": [...], "2": [...]}
            return parser.readValueAs(new TypeReference<Map<String, List<SeriesEpisode>>>() {});
        } else if (parser.currentToken() == JsonToken.START_ARRAY) {
            // Array of arrays format: [[...], [...]]
            List<List<SeriesEpisode>> episodesList = parser.readValueAs(new TypeReference<List<List<SeriesEpisode>>>() {});

            // Convert to map with season index as key
            Map<String, List<SeriesEpisode>> episodesMap = new HashMap<>();
            for (int i = 0; i < episodesList.size(); i++) {
                episodesMap.put(String.valueOf(i), episodesList.get(i));
            }
            return episodesMap;
        }

        // Return empty map if neither format
        return new HashMap<>();
    }
}
