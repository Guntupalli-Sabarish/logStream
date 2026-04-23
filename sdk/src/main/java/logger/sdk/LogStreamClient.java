package logger.sdk;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
public class LogStreamClient implements AutoCloseable {
    private static final LogEntry POISON = LogEntry.builder("__POISON__").build();
    private final String          endpoint;
    private final int             batchSize;
    private final long            flushIntervalMs;
    private final ObjectMapper    mapper = new ObjectMapper();
    private final HttpClient      http;
    private final LinkedBlockingQueue<LogEntry> buffer;
    private final Thread flusherThread;
    private volatile boolean closed = false;
    private LogStreamClient(Builder b) {
        this.endpoint        = b.endpoint.replaceAll("/$", "") + "/api/logs/batch";
        this.batchSize       = b.batchSize;
        this.flushIntervalMs = b.flushIntervalMs;
        this.buffer          = new LinkedBlockingQueue<>(b.bufferCapacity);
        this.http            = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.flusherThread = Thread.ofVirtual().name("logstream-flusher").start(this::flushLoop);
    }
    public void log(LogEntry entry) {
        if (!closed) buffer.offer(entry);
    }
    public void info(String msg)                          { log(LogEntry.builder(msg).low().build()); }
    public void warn(String msg)                          { log(LogEntry.builder(msg).warn().build()); }
    public void error(String msg)                         { log(LogEntry.builder(msg).high().build()); }
    public void error(String msg, Throwable t)            { log(LogEntry.builder(msg).high().stackTrace(t).build()); }
    public void error(String msg, Throwable t, String tid){ log(LogEntry.builder(msg).high().stackTrace(t).traceId(tid).build()); }
    @Override
    public void close() {
        closed = true;
        buffer.offer(POISON);
        try { flusherThread.join(5_000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
    private void flushLoop() {
        List<LogEntry> batch = new ArrayList<>(batchSize);
        while (true) {
            try {
                LogEntry head = buffer.poll(flushIntervalMs, TimeUnit.MILLISECONDS);
                if (head == POISON) { flush(batch); break; }
                if (head != null) batch.add(head);
                buffer.drainTo(batch, batchSize - batch.size());
                if (!batch.isEmpty()) {
                    flush(batch);
                    batch.clear();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                flush(batch);
                break;
            }
        }
    }
    private void flush(List<LogEntry> entries) {
        if (entries.isEmpty()) return;
        try {
            String json = mapper.writeValueAsString(entries);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .timeout(Duration.ofSeconds(5))
                    .build();
            http.send(req, HttpResponse.BodyHandlers.discarding());
        } catch (IOException | InterruptedException e) {
            System.err.println("[LogStream SDK] Flush failed: " + e.getMessage());
        }
    }
    public static Builder builder(String endpoint) { return new Builder(endpoint); }
    public static class Builder {
        private final String endpoint;
        private int  batchSize       = 100;
        private long flushIntervalMs = 500;
        private int  bufferCapacity  = 1_000;
        private String service;
        Builder(String endpoint) { this.endpoint = endpoint; }
        public Builder batchSize(int n)         { this.batchSize       = n;       return this; }
        public Builder flushIntervalMs(long ms) { this.flushIntervalMs = ms;      return this; }
        public Builder bufferCapacity(int n)    { this.bufferCapacity  = n;       return this; }
        public Builder service(String s)        { this.service         = s;       return this; }
        public LogStreamClient build() { return new LogStreamClient(this); }
    }
}
