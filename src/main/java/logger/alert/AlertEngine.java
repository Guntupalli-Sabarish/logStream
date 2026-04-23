package logger.alert;

import logger.enums.Severity;
import logger.pojo.Log;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
public class AlertEngine {

    // Per-severity sliding window of log timestamps (epoch ms)
    private final Map<Severity, ConcurrentLinkedDeque<Long>> windows = new ConcurrentHashMap<>();

    // Per-rule last-fired timestamp (epoch ms), for cooldown enforcement
    private final Map<String, Long> lastFired = new ConcurrentHashMap<>();

    // Active rules, keyed by rule ID
    private final Map<String, AlertRule> rules = new ConcurrentHashMap<>();

    private final WebhookNotifier notifier;

    public AlertEngine(WebhookNotifier notifier) {
        this.notifier = notifier;
        // Initialize sliding windows for all severities upfront
        for (Severity s : Severity.values()) {
            windows.put(s, new ConcurrentLinkedDeque<>());
        }
    }

    /**
     * Called on every addLog(). Evaluates all rules against the updated window.
     * Runs on the producer thread — must be fast (no I/O).
     */
    public void evaluate(Log log) {
        Severity sev = log.getSeverity();
        if (sev == null) return;

        long now = System.currentTimeMillis();
        ConcurrentLinkedDeque<Long> window = windows.get(sev);
        window.addLast(now);

        for (AlertRule rule : rules.values()) {
            if (!rule.getSeverity().equals(sev)) continue;

            long windowMs = rule.getWindowSeconds() * 1_000L;

            // Trim entries older than the window
            while (!window.isEmpty() && now - window.peekFirst() > windowMs) {
                window.pollFirst();
            }

            if (window.size() >= rule.getThreshold()) {
                long cooldownMs = rule.getCooldownMinutes() * 60_000L;
                Long fired = lastFired.get(rule.getId());
                if (fired == null || now - fired >= cooldownMs) {
                    lastFired.put(rule.getId(), now);
                    // Offload webhook call to a daemon thread so it never blocks the producer
                    int count = window.size();
                    Thread t = new Thread(() -> notifier.send(rule, log, count));
                    t.setDaemon(true);
                    t.start();
                }
            }
        }
    }

    public void addRule(AlertRule rule) { rules.put(rule.getId(), rule); }

    public void removeRule(String id) { rules.remove(id); }

    public Collection<AlertRule> getRules() { return rules.values(); }

}
