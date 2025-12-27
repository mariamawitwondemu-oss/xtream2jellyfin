package uk.humbkr.xtream2jellyfin.filemanager;

/**
 * Interface for managing file operations related to stream processing.
 */
public interface FileManager {

    /**
     * Hook method called at the start of the processing.
     */
    void onProcessStart();

    /**
     * Hook method called at the end of the processing.
     */
    void onProcessEnd();

    void save(String path, Object content, String date);

}
