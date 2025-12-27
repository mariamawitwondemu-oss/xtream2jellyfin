package uk.humbkr.xtream2jellyfin.xtream.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Endpoint {
    PLAYER("player_api"),
    EPG("xmltv");

    private final String value;

    public boolean isJson() {
        return this == PLAYER;
    }

    @Override
    public String toString() {
        return value;
    }
}
