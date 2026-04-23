package logger.pojo;

import logger.enums.Severity;

import java.io.Serializable;
import java.sql.Timestamp;

public class Log implements Serializable {

    private String id;
    private String data;
    private Timestamp timestamp;
    private String threadId;
    private String threadName;
    private Severity severity;
    private String stackTrace;

    // Distributed tracing fields
    private String traceId;
    private String spanId;

    // Source metadata
    private String service;
    private String environment;

    public Log() {}

    public Log(String data) { this.data = data; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    public String getThreadId() { return threadId; }
    public void setThreadId(String threadId) { this.threadId = threadId; }

    public String getThreadName() { return threadName; }
    public void setThreadName(String threadName) { this.threadName = threadName; }

    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }

    public String getStackTrace() { return stackTrace; }
    public void setStackTrace(String stackTrace) { this.stackTrace = stackTrace; }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    public String getSpanId() { return spanId; }
    public void setSpanId(String spanId) { this.spanId = spanId; }

    public String getService() { return service; }
    public void setService(String service) { this.service = service; }

    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }

}
