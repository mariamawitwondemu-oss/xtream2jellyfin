package uk.humbkr.xtream2jellyfin.xtream.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MediaType {
    LIVE("live"),
    SERIES("series"),
    MOVIE("movie");

    private final String value;

    @Override
    public String toString() {
        return value;
    }
}
