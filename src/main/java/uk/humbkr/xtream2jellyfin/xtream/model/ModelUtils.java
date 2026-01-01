package uk.humbkr.xtream2jellyfin.xtream.model;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ModelUtils {

    /**
     * Check if a string is a YouTube video ID.
     * YouTube video IDs are 11 characters long and contain only letters, numbers, hyphens, and underscores.
     *
     * @param value the string to check
     * @return true if it's a YouTube video ID, false otherwise
     */
    public static boolean isYouTubeVideoId(String value) {
        if (value == null || value.length() != 11) {
            return false;
        }
        return value.matches("[a-zA-Z0-9_-]{11}");
    }

    public static String getYoutubeTrailerUrl(String youtubeTrailer) {
        if (isYouTubeVideoId(youtubeTrailer)) {
            return Constants.YOUTUBE_VIDEO_URL + youtubeTrailer;
        }
        return youtubeTrailer;
    }

}
