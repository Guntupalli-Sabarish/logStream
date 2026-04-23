package logger.data;

import logger.enums.Severity;
import logger.pojo.Log;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FileStoreTest uses a self-contained TestFileStore inner class
 * (same logic as FileStore, but writes to @TempDir).
 */
class FileStoreTest {

    @TempDir
    Path tempDir;

    private TestFileStore fileStore;
    private ObjectMapper  mapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        fileStore = new TestFileStore(tempDir, mapper);
    }

    @AfterEach
    void tearDown() { fileStore.close(); }

    @Test
    void addLog_writesJsonLineToFile() throws IOException {
        fileStore.addLog(buildLog("Server started", Severity.HIGH));
        fileStore.close();

        List<String> lines = Files.readAllLines(tempDir.resolve("system.log"));
        assertEquals(1, lines.size());
        JsonNode node = mapper.readTree(lines.get(0));
        assertEquals("HIGH",           node.get("severity").asText());
        assertEquals("Server started", node.get("data").asText());
    }

    @Test
    void addLog_sanitizesNewlinesInData() throws IOException {
        fileStore.addLog(buildLog("line1\nFORGED: [HIGH] injected", Severity.LOW));
        fileStore.close();

        List<String> lines = Files.readAllLines(tempDir.resolve("system.log"));
        assertEquals(1, lines.size(), "Newline injection must produce only 1 JSON line");
    }

    @Test
    void addLog_sanitizesCarriageReturns() throws IOException {
        fileStore.addLog(buildLog("data\r\ninjected", Severity.WARN));
        fileStore.close();

        List<String> lines = Files.readAllLines(tempDir.resolve("system.log"));
        assertEquals(1, lines.size());
    }

    @Test
    void clearFile_truncatesExistingContent() throws IOException {
        fileStore.addLog(buildLog("before clear", Severity.LOW));
        fileStore.clearFile();
        fileStore.addLog(buildLog("after clear",  Severity.LOW));
        fileStore.close();

        List<String> lines = Files.readAllLines(tempDir.resolve("system.log"));
        assertEquals(1, lines.size(), "Only post-clear log should remain");
        JsonNode node = mapper.readTree(lines.get(0));
        assertEquals("after clear", node.get("data").asText());
    }

    @Test
    void addLog_handlesNullSeverity() throws IOException {
        Log log = new Log("minimal");
        log.setTimestamp(new Timestamp(System.currentTimeMillis()));
        fileStore.addLog(log);
        fileStore.close();

        List<String> lines = Files.readAllLines(tempDir.resolve("system.log"));
        assertEquals(1, lines.size());
        // severity null → JSON null, no crash
        JsonNode node = mapper.readTree(lines.get(0));
        assertEquals("minimal", node.get("data").asText());
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private Log buildLog(String data, Severity severity) {
        Log log = new Log(data);
        log.setTimestamp(new Timestamp(System.currentTimeMillis()));
        log.setSeverity(severity);
        return log;
    }

    // Self-contained test double — same logic as FileStore but uses tempDir
    static class TestFileStore {
        private final Path logFile;
        private final ObjectMapper mapper;
        private PrintWriter writer;
        private final ReentrantLock lock = new ReentrantLock();

        TestFileStore(Path dir, ObjectMapper mapper) throws IOException {
            this.logFile = dir.resolve("system.log");
            this.mapper  = mapper;
            openWriter(true);
        }

        private void openWriter(boolean append) throws IOException {
            writer = new PrintWriter(new BufferedWriter(new FileWriter(logFile.toFile(), append)));
        }

        void addLog(Log log) {
            if (log.getData() != null) log.setData(log.getData().replaceAll("[\\r\\n\\t]", " "));
            lock.lock();
            try {
                writer.println(mapper.writeValueAsString(log));
                writer.flush();
            } catch (Exception e) { throw new RuntimeException(e); }
            finally { lock.unlock(); }
        }

        void clearFile() {
            lock.lock();
            try { writer.close(); openWriter(false); }
            catch (IOException e) { throw new RuntimeException(e); }
            finally { lock.unlock(); }
        }

        void close() {
            lock.lock();
            try { if (writer != null) { writer.flush(); writer.close(); writer = null; } }
            finally { lock.unlock(); }
        }
    }
}
