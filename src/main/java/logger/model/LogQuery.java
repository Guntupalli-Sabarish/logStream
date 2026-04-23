package logger.model;
import logger.enums.Severity;
public class LogQuery {
    private Severity severity;
    private String service;
    private String traceId;
    private String search;   
    private Long from;       
    private Long to;         
    private int limit = 500; 
    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }
    public String getService() { return service; }
    public void setService(String service) { this.service = service; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public String getSearch() { return search; }
    public void setSearch(String search) { this.search = search; }
    public Long getFrom() { return from; }
    public void setFrom(Long from) { this.from = from; }
    public Long getTo() { return to; }
    public void setTo(Long to) { this.to = to; }
    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = Math.min(limit > 0 ? limit : 500, 500); }
}
