package logger.service;

import logger.pojo.Log;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LoggerServiceTest {

    private Logger logger;

    @BeforeEach
    void setUp() {
        logger = new Logger();
        logger.init(); // Manually call @PostConstruct
    }

    @Test
    void addLog_storesLogInMemory() {
        Log log = buildLog("hello world");
        logger.addLog(log);

        List<Log> logs = logger.getLogs();
        assertEquals(1, logs.size());
        assertEquals("hello world", logs.get(0).getData());
    }

    @Test
    void addLog_maintainsInsertionOrder() {
        logger.addLog(buildLog("first"));
        logger.addLog(buildLog("second"));
        logger.addLog(buildLog("third"));

        List<Log> logs = logger.getLogs();
        assertEquals("first",  logs.get(0).getData());
        assertEquals("second", logs.get(1).getData());
        assertEquals("third",  logs.get(2).getData());
    }

    @Test
    void addLog_boundedBuffer_evictsOldestWhenFull() throws Exception {
        int maxLogs = getMaxLogs();

        // Fill the buffer to capacity
        for (int i = 0; i < maxLogs; i++) {
            logger.addLog(buildLog("log-" + i));
        }
        assertEquals(maxLogs, logger.getLogs().size());

        // Adding one more should evict the oldest
        logger.addLog(buildLog("overflow-log"));

        List<Log> logs = logger.getLogs();
        assertEquals(maxLogs, logs.size(), "Buffer should stay bounded at MAX_LOGS");
        assertEquals("log-1", logs.get(0).getData(), "Oldest log should have been evicted");
        assertEquals("overflow-log", logs.get(logs.size() - 1).getData());
    }

    @Test
    void clearLogs_removesAllInMemoryLogs() {
        logger.addLog(buildLog("a"));
        logger.addLog(buildLog("b"));

        logger.clearLogs();

        assertTrue(logger.getLogs().isEmpty(), "Logs should be empty after clear");
    }

    @Test
    void getLogs_returnsUnmodifiableSnapshot() {
        logger.addLog(buildLog("x"));
        List<Log> snapshot = logger.getLogs();

        assertThrows(UnsupportedOperationException.class, () -> snapshot.add(buildLog("y")));
    }

    // --- helpers ---

    private Log buildLog(String data) {
        Log log = new Log(data);
        log.setTimestamp(new java.sql.Timestamp(System.currentTimeMillis()));
        return log;
    }

    /** Reflectively reads the MAX_LOGS constant from Logger. */
    private int getMaxLogs() throws Exception {
        Field f = Logger.class.getDeclaredField("MAX_LOGS");
        f.setAccessible(true);
        return (int) f.get(null);
    }
}
