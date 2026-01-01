package uk.humbkr.xtream2jellyfin.xtream.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

public class MovieInfoDeserializer extends JsonDeserializer<MovieInfo> {

    @Override
    public MovieInfo deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (parser.currentToken() == JsonToken.START_OBJECT) {
            // Standard object format
            return parser.readValueAs(MovieInfo.class);
        } else if (parser.currentToken() == JsonToken.START_ARRAY) {
            // Empty array format - skip it and return null
            parser.skipChildren();
            return null;
        }

        // Return null if neither format
        return null;
    }
}
