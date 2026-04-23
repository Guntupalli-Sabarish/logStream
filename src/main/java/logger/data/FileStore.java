package logger.data;
import com.fasterxml.jackson.databind.ObjectMapper;
import logger.pojo.Log;
import org.springframework.stereotype.Component;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private Log sanitize(Log log) {
        if (log.getData() == null) return log;
        Log copy = new Log();
        copy.setId(log.getId());
        copy.setData(log.getData().replaceAll("[\\r\\n\\t]", " "));
        copy.setTimestamp(log.getTimestamp());
        copy.setSeverity(log.getSeverity());
        copy.setService(log.getService());
        copy.setThreadName(log.getThreadName());
        copy.setThreadId(log.getThreadId());
        copy.setTraceId(log.getTraceId());
        copy.setSpanId(log.getSpanId());
        copy.setEnvironment(log.getEnvironment());
        copy.setStackTrace(log.getStackTrace());
        return copy;
    }
    @Override
    public void clearFile() {
        writeLock.lock();
        try {
            if (writer != null) writer.close();
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
}
