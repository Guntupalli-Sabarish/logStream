package logger.alert;

import logger.pojo.Log;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class WebhookNotifier {

    @Value("${alert.webhook.url:}")
    private String webhookUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Sends a generic JSON POST to the configured webhook URL.
     * Compatible with Slack incoming webhooks (uses "text" field).
     * Set alert.webhook.url in application.properties or as an env var.
     */
    public void send(AlertRule rule, Log triggerLog, int count) {
        if (webhookUrl == null || webhookUrl.isBlank()) return;

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("text", String.format(
                "🚨 Alert fired: %d *%s* logs in %d seconds (rule: %s)",
                count, rule.getSeverity(), rule.getWindowSeconds(), rule.getId()));
        payload.put("severity",      rule.getSeverity().name());
        payload.put("count",         count);
        payload.put("windowSeconds", rule.getWindowSeconds());
        payload.put("ruleId",        rule.getId());
        payload.put("triggerMessage", triggerLog.getData());
        payload.put("service",       triggerLog.getService());
        payload.put("firedAt",       Instant.now().toString());

        try {
            restTemplate.postForEntity(webhookUrl, payload, String.class);
        } catch (Exception e) {
            System.err.println("Webhook delivery failed: " + e.getMessage());
        }
    }

}
