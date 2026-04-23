package logger.controller;
import logger.alert.AlertEngine;
import logger.alert.AlertRule;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Collection;
@RestController
@RequestMapping("/api/alerts")
public class AlertController {
    private final AlertEngine alertEngine;
    public AlertController(AlertEngine alertEngine) {
        this.alertEngine = alertEngine;
    }
    @GetMapping("/rules")
    public Collection<AlertRule> getRules() {
        return alertEngine.getRules();
    }
    @PostMapping("/rules")
    public AlertRule addRule(@RequestBody AlertRule rule) {
        alertEngine.addRule(rule);
        return rule;
    }
    @DeleteMapping("/rules/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable String id) {
        alertEngine.removeRule(id);
        return ResponseEntity.noContent().build();
    }
}
