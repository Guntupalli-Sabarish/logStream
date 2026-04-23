package logger.alert;

import logger.enums.Severity;
import java.util.UUID;

public class AlertRule {

    private String id;
    private Severity severity;
    private int threshold;       // number of logs in window before firing
    private int windowSeconds;   // sliding window size
    private int cooldownMinutes; // minimum gap between repeated alerts for this rule

    public AlertRule() {
        this.id = UUID.randomUUID().toString();
    }

    public AlertRule(Severity severity, int threshold, int windowSeconds, int cooldownMinutes) {
        this.id = UUID.randomUUID().toString();
        this.severity = severity;
        this.threshold = threshold;
        this.windowSeconds = windowSeconds;
        this.cooldownMinutes = cooldownMinutes;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }

    public int getThreshold() { return threshold; }
    public void setThreshold(int threshold) { this.threshold = threshold; }

    public int getWindowSeconds() { return windowSeconds; }
    public void setWindowSeconds(int windowSeconds) { this.windowSeconds = windowSeconds; }

    public int getCooldownMinutes() { return cooldownMinutes; }
    public void setCooldownMinutes(int cooldownMinutes) { this.cooldownMinutes = cooldownMinutes; }

}
