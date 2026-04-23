package logger.sdk;
public class LogEntry {
    private String data;
    private String severity;   
    private String service;
    private String traceId;
    private String spanId;
    private String threadName;
    private String environment;
    private String stackTrace;
    LogEntry() {}
    private LogEntry(Builder b) {
        this.data        = b.data;
        this.severity    = b.severity;
        this.service     = b.service;
        this.traceId     = b.traceId;
        this.spanId      = b.spanId;
        this.threadName  = b.threadName != null ? b.threadName : Thread.currentThread().getName();
        this.environment = b.environment;
        this.stackTrace  = b.stackTrace;
    }
    public String getData()        { return data; }
    public String getSeverity()    { return severity; }
    public String getService()     { return service; }
    public String getTraceId()     { return traceId; }
    public String getSpanId()      { return spanId; }
    public String getThreadName()  { return threadName; }
    public String getEnvironment() { return environment; }
    public String getStackTrace()  { return stackTrace; }
    public static Builder builder(String message) { return new Builder(message); }
    public static class Builder {
        private final String data;
        private String severity   = "LOW";
        private String service;
        private String traceId;
        private String spanId;
        private String threadName;
        private String environment = "prod";
        private String stackTrace;
        Builder(String data) { this.data = data; }
        public Builder severity(String s)    { this.severity    = s; return this; }
        public Builder high()                { return severity("HIGH"); }
        public Builder warn()                { return severity("WARN"); }
        public Builder low()                 { return severity("LOW");  }
        public Builder service(String s)     { this.service     = s; return this; }
        public Builder traceId(String t)     { this.traceId     = t; return this; }
        public Builder spanId(String s)      { this.spanId      = s; return this; }
        public Builder threadName(String t)  { this.threadName  = t; return this; }
        public Builder environment(String e) { this.environment = e; return this; }
        public Builder stackTrace(Throwable t) {
            this.stackTrace = java.util.Arrays.stream(t.getStackTrace())
                .map(StackTraceElement::toString)
                .limit(10)
                .reduce(t.toString(), (a, b) -> a + "\n  at " + b);
            return this;
        }
        public LogEntry build() { return new LogEntry(this); }
    }
}
