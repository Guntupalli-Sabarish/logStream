package logger.controller;
import logger.alert.AlertEngine;
import logger.alert.AlertRule;
import logger.enums.Severity;
import logger.model.LogQuery;
import logger.model.LogStats;
import logger.pojo.Log;
import logger.service.Logger;
import logger.service.StatsService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import logger.service.SseEmitterRegistry;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
@RestController
@RequestMapping("/api/logs")
public class LogController {
    private final Logger            loggerService;
    private final StatsService      statsService;
    private final SseEmitterRegistry emitterRegistry;
    public LogController(Logger loggerService,
                         StatsService statsService,
                         SseEmitterRegistry emitterRegistry) {
        this.loggerService   = loggerService;
        this.statsService    = statsService;
        this.emitterRegistry = emitterRegistry;
    }
    @GetMapping
    public List<Log> getLogs(
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String service,
            @RequestParam(required = false) String traceId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long   from,
            @RequestParam(required = false) Long   to,
            @RequestParam(required = false, defaultValue = "500") int limit) {
        boolean hasFilter = severity != null || service != null || traceId != null
                || search != null || from != null || to != null;
        if (!hasFilter) {
            return loggerService.getLogs();
        }
        LogQuery query = new LogQuery();
        if (severity != null) {
            try { query.setSeverity(Severity.valueOf(severity.toUpperCase())); }
            catch (IllegalArgumentException ignored) {}
        }
        query.setService(service);
        query.setTraceId(traceId);
        query.setSearch(search);
        query.setFrom(from);
        query.setTo(to);
        query.setLimit(limit);
        return loggerService.queryLogs(query);
    }
    @PostMapping
    public Log addLog(@RequestBody Log log) {
        enrich(log);
        loggerService.addLog(log);
        return log;
    }
    @PostMapping("/batch")
    public List<Log> addBatch(@RequestBody List<Log> logs) {
        logs.forEach(log -> {
            enrich(log);
            loggerService.addLog(log);
        });
        return logs;
    }
    @DeleteMapping
    public void clearLogs() {
        loggerService.clearLogs();
    }
    @GetMapping("/stats")
    public LogStats getStats() {
        return statsService.compute(loggerService.getLogs());
    }
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return emitterRegistry.register();
    }
    private void enrich(Log log) {
        if (log.getId() == null || log.getId().isBlank()) {
            log.setId(UUID.randomUUID().toString());
        }
        if (log.getTimestamp() == null) {
            log.setTimestamp(new Timestamp(System.currentTimeMillis()));
        }
        if (log.getThreadName() == null || log.getThreadName().isBlank()) {
            log.setThreadName(Thread.currentThread().getName());
        }
        if (log.getEnvironment() == null || log.getEnvironment().isBlank()) {
            log.setEnvironment("dev");
        }
    }
}
