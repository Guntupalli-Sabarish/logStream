package logger.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import logger.pojo.Log;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class FileStore implements Datastore {

    private static final String LOG_DIR = "logs";
    private static final String FILE_PATH = LOG_DIR + File.separator + "system.log";

    private final ReentrantLock writeLock = new ReentrantLock();
    private final ObjectMapper objectMapper;
    private PrintWriter writer;

    public FileStore(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        try {
            Path logDir = Paths.get(LOG_DIR);
            if (!Files.exists(logDir)) {
                Files.createDirectories(logDir);
            }
            openWriter(true);
        } catch (IOException e) {
            System.err.println("Failed to initialize FileStore: " + e.getMessage());
        }
    }

    private void openWriter(boolean append) throws IOException {
        writer = new PrintWriter(new BufferedWriter(new FileWriter(FILE_PATH, append)));
    }

    @Override
    public void addLog(Log log) {
        writeLock.lock();
        try {
            if (writer != null) {
                // JSON Lines: one JSON object per line (sanitize data field)
                Log sanitized = sanitize(log);
                writer.println(objectMapper.writeValueAsString(sanitized));
                writer.flush();
            }
        } catch (Exception e) {
            System.err.println("Failed to write log to file: " + e.getMessage());
        } finally {
            writeLock.unlock();
        }
    }

    /** Replace control characters in log.data to prevent log injection. */
    private Log sanitize(Log log) {
        if (log.getData() == null) return log;
        // We serialize to JSON so newlines are escaped automatically by Jackson,
        // but sanitize anyway for defense-in-depth.
        log.setData(log.getData().replaceAll("[\\r\\n\\t]", " "));
        return log;
    }

    @Override
    public void clearFile() {
        writeLock.lock();
        try {
            if (writer != null) writer.close();
            openWriter(false); // truncate
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
        // reserved for future batch processing
    }

}
