package logger.controller;

import logger.pojo.Log;
import logger.service.Logger;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/logs")
public class LogController {

    private final Logger loggerService;

    public LogController(Logger loggerService) {
        this.loggerService = loggerService;
    }

    @GetMapping
    public List<Log> getLogs() {
        return loggerService.getLogs();
    }

    @PostMapping
    public Log addLog(@RequestBody Log log) {
        // Assign stable server-side ID
        if (log.getId() == null || log.getId().isBlank()) {
            log.setId(UUID.randomUUID().toString());
        }
        // Set timestamp if not provided by client
        if (log.getTimestamp() == null) {
            log.setTimestamp(new java.sql.Timestamp(System.currentTimeMillis()));
        }
        // Set threadName to the request-handling thread if not provided
        if (log.getThreadName() == null || log.getThreadName().isBlank()) {
            log.setThreadName(Thread.currentThread().getName());
        }
        loggerService.addLog(log);
        return log;
    }

    @DeleteMapping
    public void clearLogs() {
        loggerService.clearLogs();
    }

}
