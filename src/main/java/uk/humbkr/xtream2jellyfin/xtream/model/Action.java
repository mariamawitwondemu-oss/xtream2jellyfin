package uk.humbkr.xtream2jellyfin.xtream.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
@AllArgsConstructor
@Getter
public enum Action {

    LIVE_CATEGORIES("get_live_categories", Category.class, List.class),
    LIVE_STREAMS("get_live_streams", Object.class, List.class),
    SERIES_CATEGORIES("get_series_categories", Category.class, List.class),
    SERIES_STREAMS("get_series", SerieItem.class, List.class),
    VOD_CATEGORIES("get_vod_categories", Category.class, List.class),
    VOD_STREAMS("get_vod_streams", Object.class, List.class),
    SERIES_INFO("get_series_info", SerieInfo.class),
    VOD_INFO("get_vod_info", Object.class),
    EPG_INFO("get_short_epg", Object.class);

    private final String value;

    private final Class<?> valueType;

    private Class<? extends Collection> collectionType;

    @Override
    public String toString() {
        return value;
    }
}
