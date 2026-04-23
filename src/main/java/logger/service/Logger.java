package logger.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import logger.data.FileStore;
import logger.pojo.Log;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class Logger {

    private static final int MAX_LOGS = 5_000;
    private static final int QUEUE_CAPACITY = 10_000;

    // Sentinel log used to signal the consumer thread to stop
    private static final Log POISON_PILL = new Log("__POISON__");

    private final ArrayDeque<Log> logStore = new ArrayDeque<>(MAX_LOGS + 1);
    private final ReadWriteLock storeLock = new ReentrantReadWriteLock();

    private final LinkedBlockingQueue<Log> logsProcessingQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);

    private final FileStore fileStore = new FileStore();

    private Thread consumerThread;

    @PostConstruct
    public void init() {
        consumerThread = new Thread(this::processQueue, "log-consumer");
        consumerThread.setDaemon(false); // non-daemon: JVM won't exit while draining
        consumerThread.start();
    }

    @PreDestroy
    public void shutdown() {
        // Offer the poison pill to unblock the consumer's take()
        logsProcessingQueue.offer(POISON_PILL);
        try {
            // Give the consumer up to 5 seconds to drain and exit
            consumerThread.join(5_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        fileStore.close();
    }

    /**
     * PRODUCER: Non-blocking. Drops the log if the queue is full.
     */
    public void addLog(Log log) {
        storeLock.writeLock().lock();
        try {
            if (logStore.size() >= MAX_LOGS) {
                logStore.pollFirst(); // Evict oldest to stay bounded
            }
            logStore.addLast(log);
        } finally {
            storeLock.writeLock().unlock();
        }

        // Non-blocking offer — silently drop if queue is full (backpressure policy: drop)
        logsProcessingQueue.offer(log);
    }

    /**
     * CONSUMER: Runs in background, takes from queue, writes to disk.
     */
    private void processQueue() {
        while (true) {
            try {
                Log log = logsProcessingQueue.take();
                if (log == POISON_PILL) {
                    // Drain remaining items before stopping
                    Log remaining;
                    while ((remaining = logsProcessingQueue.poll(100, TimeUnit.MILLISECONDS)) != null) {
                        if (remaining != POISON_PILL) {
                            fileStore.addLog(remaining);
                        }
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

    public void clearLogs() {
        storeLock.writeLock().lock();
        try {
            logStore.clear();
        } finally {
            storeLock.writeLock().unlock();
        }
        logsProcessingQueue.clear();
        fileStore.clearFile();
    }

}
