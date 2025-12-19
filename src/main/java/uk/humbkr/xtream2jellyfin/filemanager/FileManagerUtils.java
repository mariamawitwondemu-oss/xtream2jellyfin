package uk.humbkr.xtream2jellyfin.filemanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.humbkr.xtream2jellyfin.common.JsonUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

    @SuppressWarnings("unchecked")
    public static <T> T readFileContent(String path) {
        try {
            Path filePath = Paths.get(path);
            if (!Files.exists(filePath)) {
                return null;
            }
            String stringContent = Files.readString(filePath, StandardCharsets.UTF_8);
            if (path.endsWith(".json")) {
                return (T) JsonUtils.getJsonMapper().readValue(stringContent, Object.class);
            }
            return (T) stringContent;
        } catch (IOException e) {
            log.error("Failed to read file: {}", path, e);
            return null;
        }

    }

    public static Object get(String path, Object defaultValue) {
        try {
            Path filePath = Paths.get(path);
            if (!Files.exists(filePath)) {
                return defaultValue;
            }

            String content = Files.readString(filePath, StandardCharsets.UTF_8);

            if (path.endsWith(".json")) {
                ObjectMapper mapper = JsonUtils.getJsonMapper();
                return mapper.readValue(content, Object.class);
            }

            return content;
        } catch (IOException e) {
            log.error("Failed to read file: {}", path, e);
            return defaultValue;
        }
    }

}
