package utils;

import app.Config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Appends one timestamped line per event to Config.LOG_FILE. A logging failure must never stop an agent. */
public final class Logger {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Object LOCK = new Object(); // shared by all agent threads so lines never interleave

    private Logger() {}

    public static void log(String agent, String message) {
        String line = LocalTime.now().format(TIME) + " [" + agent + "] " + message;
        System.out.println(line);
        synchronized (LOCK) {
            try {
                Path file = Path.of(Config.LOG_FILE);
                Files.createDirectories(file.getParent());
                Files.writeString(file, line + System.lineSeparator(),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                System.err.println("Could not write to log file: " + e.getMessage());
            }
        }
    }
}