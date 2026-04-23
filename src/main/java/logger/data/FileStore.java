package logger.data;

import logger.pojo.Log;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

public class FileStore implements Datastore {

    private static final String LOG_DIR = "logs";
    private static final String FILE_PATH = LOG_DIR + File.separator + "system.log";

    private final ReentrantLock writeLock = new ReentrantLock();
    private PrintWriter writer;

    public FileStore() {
        try {
            Path logDir = Paths.get(LOG_DIR);
            if (!Files.exists(logDir)) {
                Files.createDirectories(logDir);
            }
            openWriter(true); // append=true on startup
        } catch (IOException e) {
            System.err.println("Failed to initialize FileStore: " + e.getMessage());
        }
    }

    private void openWriter(boolean append) throws IOException {
        writer = new PrintWriter(new BufferedWriter(new FileWriter(FILE_PATH, append)));
    }

    @Override
    public void addLog(Log log) {
        String timestamp = log.getTimestamp() != null ? log.getTimestamp().toString() : "N/A";
        String severity  = log.getSeverity()  != null ? log.getSeverity().toString()  : "LOW";
        String rawData   = log.getData()       != null ? log.getData()                 : "";

        // Sanitize: replace newlines/tabs to prevent log injection
        String sanitizedData = rawData.replaceAll("[\\r\\n\\t]", " ");

        writeLock.lock();
        try {
            if (writer != null) {
                writer.println(String.format("[%s] [%s] %s", timestamp, severity, sanitizedData));
                writer.flush();
            }
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void clearFile() {
        writeLock.lock();
        try {
            // Close existing writer, reopen with append=false (truncate)
            if (writer != null) {
                writer.close();
            }
            openWriter(false);
        } catch (IOException e) {
            System.err.println("Failed to clear log file: " + e.getMessage());
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void close() {
        writeLock.lock();
        try {
            if (writer != null) {
                writer.flush();
                writer.close();
                writer = null;
            }
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void appendLog() throws TimeoutException {
        // Intended for future batch processing; individual writes handled by addLog()
    }

}
