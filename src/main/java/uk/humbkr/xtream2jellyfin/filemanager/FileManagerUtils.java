package uk.humbkr.xtream2jellyfin.filemanager;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class FileManagerUtils {

    public static void prepareDirectory(String directoryPath) {
        try {
            Path path = Paths.get(directoryPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            log.error("Failed to create directory: {}", directoryPath, e);
        }
    }

}
