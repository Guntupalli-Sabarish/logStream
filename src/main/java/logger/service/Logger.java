package logger.service;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import logger.alert.AlertEngine;
import logger.data.FileStore;
import logger.model.LogQuery;
import logger.pojo.Log;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
@Service
public class Logger {
    private static final int MAX_LOGS      = 5_000;
    private static final int QUEUE_CAPACITY = 10_000;
    private static final Log POISON_PILL   = new Log("__POISON__");
    private final ArrayDeque<Log>          logStore    = new ArrayDeque<>(MAX_LOGS + 1);
    private final ReadWriteLock            storeLock   = new ReentrantReadWriteLock();
    private final LinkedBlockingQueue<Log> queue       = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
    private final FileStore          fileStore;
    private final SseEmitterRegistry emitterRegistry;
    private final AlertEngine        alertEngine;
    private Thread consumerThread;
    public Logger(FileStore fileStore,
                  SseEmitterRegistry emitterRegistry,
                  AlertEngine alertEngine) {
        this.fileStore       = fileStore;
        this.emitterRegistry = emitterRegistry;
        this.alertEngine     = alertEngine;
    }
    @PostConstruct
    public void init() {
        consumerThread = new Thread(this::processQueue, "log-consumer");
        consumerThread.setDaemon(false);
        consumerThread.start();
    }
    @PreDestroy
    public void shutdown() {
        queue.offer(POISON_PILL);
        try {
            consumerThread.join(5_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        fileStore.close();
    }
    public void addLog(Log log) {
        storeLock.writeLock().lock();
        try {
            if (logStore.size() >= MAX_LOGS) logStore.pollFirst();
            logStore.addLast(log);
        } finally {
            storeLock.writeLock().unlock();
        }
        queue.offer(log);              
        emitterRegistry.broadcast(log); 
        alertEngine.evaluate(log);     
    }
    private void processQueue() {
        while (true) {
            try {
                Log log = queue.take();
                if (log == POISON_PILL) {
                    Log remaining;
                    while ((remaining = queue.poll(100, TimeUnit.MILLISECONDS)) != null) {
                        if (remaining != POISON_PILL) fileStore.addLog(remaining);
                    }
                    break;
                }
                fileStore.addLog(log);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    public List<Log> getLogs() {
        storeLock.readLock().lock();
        try {
            return Collections.unmodifiableList(new ArrayList<>(logStore));
        } finally {
            storeLock.readLock().unlock();
        }
    }
    public List<Log> queryLogs(LogQuery query) {
        storeLock.readLock().lock();
        try {
            Stream<Log> stream = logStore.stream();
            if (query.getSeverity() != null) {
                stream = stream.filter(l -> query.getSeverity().equals(l.getSeverity()));
            }
            if (isSet(query.getService())) {
                stream = stream.filter(l -> query.getService().equals(l.getService()));
            }
            if (isSet(query.getTraceId())) {
                stream = stream.filter(l -> query.getTraceId().equals(l.getTraceId()));
            }
            if (isSet(query.getSearch())) {
                Pattern p = buildPattern(query.getSearch());
                stream = stream.filter(l -> l.getData() != null && p.matcher(l.getData()).find());
            }
            if (query.getFrom() != null) {
                stream = stream.filter(l -> l.getTimestamp() != null
                        && l.getTimestamp().getTime() >= query.getFrom());
            }
            if (query.getTo() != null) {
                stream = stream.filter(l -> l.getTimestamp() != null
                        && l.getTimestamp().getTime() <= query.getTo());
            }
            int limit = query.getLimit() > 0 ? query.getLimit() : 500;
            return stream.limit(limit).collect(Collectors.toList());
        } finally {
            storeLock.readLock().unlock();
        }
    }
    public void clearLogs() {
        storeLock.writeLock().lock();
        try {
            logStore.clear();
        } finally {
            storeLock.writeLock().unlock();
        }
        fileStore.clearFile();
        queue.clear();
    }
    private boolean isSet(String s) { return s != null && !s.isBlank(); }
    private Pattern buildPattern(String search) {
        try {
            return Pattern.compile(search, Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException e) {
            return Pattern.compile(Pattern.quote(search), Pattern.CASE_INSENSITIVE);
        }
    }
}
