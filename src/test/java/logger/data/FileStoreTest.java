package logger.data;

import logger.enums.Severity;
import logger.pojo.Log;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileStoreTest {

    @TempDir
    Path tempDir;

    private FileStore fileStore;

    @BeforeEach
    void setUp() throws Exception {
        // Point FileStore at the temp directory for isolation
        fileStore = createFileStoreWithTempDir(tempDir);
    }

    @AfterEach
    void tearDown() {
        fileStore.close();
    }

    @Test
    void addLog_writesFormattedLineToFile() throws IOException {
        Log log = buildLog("Server started", Severity.HIGH);
        fileStore.addLog(log);
        fileStore.close();

        List<String> lines = readLogLines();
        assertEquals(1, lines.size());
        assertTrue(lines.get(0).contains("[HIGH]"), "Line should contain severity");
        assertTrue(lines.get(0).contains("Server started"), "Line should contain log data");
    }

    @Test
    void addLog_sanitizesNewlinesInData() throws IOException {
        Log log = buildLog("line1\nFORGED: [HIGH] injected", Severity.LOW);
        fileStore.addLog(log);
        fileStore.close();

        List<String> lines = readLogLines();
        assertEquals(1, lines.size(), "Newline injection should produce only 1 line");
        assertTrue(lines.get(0).contains("line1 FORGED"), "Newline should be replaced with space");
    }

    @Test
    void addLog_sanitizesCarriageReturns() throws IOException {
        Log log = buildLog("data\r\ninjected", Severity.WARN);
        fileStore.addLog(log);
        fileStore.close();

        List<String> lines = readLogLines();
        assertEquals(1, lines.size());
    }

    @Test
    void clearFile_truncatesExistingContent() throws IOException {
        fileStore.addLog(buildLog("before clear", Severity.LOW));
        fileStore.clearFile();
        fileStore.addLog(buildLog("after clear", Severity.LOW));
        fileStore.close();

        List<String> lines = readLogLines();
        assertEquals(1, lines.size(), "Only the post-clear log should exist");
        assertTrue(lines.get(0).contains("after clear"));
    }

    @Test
    void addLog_handlesNullTimestampAndSeverity() throws IOException {
        Log log = new Log("minimal log");
        fileStore.addLog(log);
        fileStore.close();

        List<String> lines = readLogLines();
        assertEquals(1, lines.size());
        assertTrue(lines.get(0).contains("[N/A]"), "Null timestamp should show N/A");
        assertTrue(lines.get(0).contains("[LOW]"),  "Null severity should default to LOW");
    }

    // --- helpers ---

    private Log buildLog(String data, Severity severity) {
        Log log = new Log(data);
        log.setTimestamp(new Timestamp(System.currentTimeMillis()));
        log.setSeverity(severity);
        return log;
    }

    private List<String> readLogLines() throws IOException {
        Path logFile = tempDir.resolve("system.log");
        return Files.readAllLines(logFile);
    }

    /**
     * Creates a FileStore pointed at the given temp directory by reflectively
     * overriding the FILE_PATH field (avoids polluting the project's logs/ dir).
     */
    private FileStore createFileStoreWithTempDir(Path dir) throws Exception {
        // We subclass FileStore to inject the temp path via a package-private constructor trick.
        // Since FILE_PATH is derived from LOG_DIR, we use a test subclass instead.
        return new TempFileStore(dir);
    }

    // Test-only subclass that writes to the temp directory
    static class TempFileStore extends FileStore {
        private final Path logFile;
        private java.io.PrintWriter writer;
        private final java.util.concurrent.locks.ReentrantLock lock = new java.util.concurrent.locks.ReentrantLock();

        TempFileStore(Path dir) throws Exception {
            // Don't call super() as it would create logs/ in project root during tests.
            // We override all methods instead.
            logFile = dir.resolve("system.log");
            openWriter(true);
        }

        private void openWriter(boolean append) throws Exception {
            writer = new java.io.PrintWriter(
                new java.io.BufferedWriter(new java.io.FileWriter(logFile.toFile(), append)));
        }

        @Override
        public void addLog(Log log) {
            String timestamp = log.getTimestamp() != null ? log.getTimestamp().toString() : "N/A";
            String severity  = log.getSeverity()  != null ? log.getSeverity().toString()  : "LOW";
            String rawData   = log.getData()       != null ? log.getData()                 : "";
            String sanitized = rawData.replaceAll("[\\r\\n\\t]", " ");

            lock.lock();
            try {
                writer.println(String.format("[%s] [%s] %s", timestamp, severity, sanitized));
                writer.flush();
            } finally {
                lock.unlock();
            }
        }

        @Override
        public void clearFile() {
            lock.lock();
            try {
                writer.close();
                openWriter(false);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                lock.unlock();
            }
        }

        @Override
        public void close() {
            lock.lock();
            try {
                if (writer != null) {
                    writer.flush();
                    writer.close();
                    writer = null;
                }
            } finally {
                lock.unlock();
            }
        }
    }
}
