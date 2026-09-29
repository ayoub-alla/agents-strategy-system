package utils;

import app.Config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Output-path helper shared by the report services. */
public final class FileUtils {
    private FileUtils() {}

    // outputFile( userChosenPathOrBlank , ".pdf" ) -> creates the folder if missing and returns .../strategy.pdf
    public static Path outputFile(String dir, String extension) throws IOException {
        Path folder = Path.of((dir == null || dir.isBlank()) ? Config.OUTPUT_PATH : dir);
        Files.createDirectories(folder);
        return folder.resolve(Config.REPORT_NAME + extension);
    }
}