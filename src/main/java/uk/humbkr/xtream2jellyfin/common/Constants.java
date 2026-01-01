package uk.humbkr.xtream2jellyfin.common;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class Constants {

    // Configuration
    public static final String CONFIG_FILE = "config/config.yaml";

    public static final int DEFAULT_SCAN_INTERVAL = 360; // 6 hours in minutes

}
