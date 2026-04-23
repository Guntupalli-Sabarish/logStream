package logger.service;
import logger.alert.AlertEngine;
import logger.alert.AlertRule;
import logger.alert.WebhookNotifier;
import logger.data.FileStore;
import logger.pojo.Log;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import java.lang.reflect.Field;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class LoggerServiceTest {
    private Logger logger;
    @BeforeEach
    void setUp() {
        FileStore mockFileStore     = mock(FileStore.class);
        SseEmitterRegistry mockSse  = mock(SseEmitterRegistry.class);
        WebhookNotifier mockHook    = mock(WebhookNotifier.class);
        AlertEngine alertEngine     = new AlertEngine(mockHook);
        logger = new Logger(mockFileStore, mockSse, alertEngine);
        logger.init();
    }
    @AfterEach
    void tearDown() {
        logger.shutdown();
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
        for (int i = 0; i < maxLogs; i++) logger.addLog(buildLog("log-" + i));
        assertEquals(maxLogs, logger.getLogs().size());
        logger.addLog(buildLog("overflow-log"));
        List<Log> logs = logger.getLogs();
        assertEquals(maxLogs, logs.size(), "Buffer must stay bounded at MAX_LOGS");
        assertEquals("log-1",        logs.get(0).getData(), "Oldest should be evicted");
        assertEquals("overflow-log", logs.get(logs.size() - 1).getData());
    }
    @Test
    void clearLogs_removesAllInMemoryLogs() {
        logger.addLog(buildLog("a"));
        logger.addLog(buildLog("b"));
        logger.clearLogs();
        assertTrue(logger.getLogs().isEmpty());
    }
    @Test
    void getLogs_returnsUnmodifiableSnapshot() {
        logger.addLog(buildLog("x"));
        List<Log> snapshot = logger.getLogs();
        assertThrows(UnsupportedOperationException.class, () -> snapshot.add(buildLog("y")));
    }
    private Log buildLog(String data) {
        Log log = new Log(data);
        log.setTimestamp(new java.sql.Timestamp(System.currentTimeMillis()));
        return log;
    }
    private int getMaxLogs() throws Exception {
        Field f = Logger.class.getDeclaredField("MAX_LOGS");
        f.setAccessible(true);
        return (int) f.get(null);
    }
}
