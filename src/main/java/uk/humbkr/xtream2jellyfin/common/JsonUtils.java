package uk.humbkr.xtream2jellyfin.common;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JsonUtils {

    @Getter
    private static final JsonMapper jsonMapper = initializeJsonMapper();

    private static JsonMapper initializeJsonMapper() {
        JsonMapper jsonMapper = new JsonMapper();
        jsonMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        jsonMapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        return jsonMapper;
    }

}
