package logger.model;

import java.util.List;
import java.util.Map;

public class LogStats {

    private int total;
    private Map<String, Long> bySeverity;
    private Map<String, Long> byService;
    private double ratePerMinute;
    private List<RateBucket> rateHistory; // last 10 one-minute buckets
    private String oldestTimestamp;
    private String newestTimestamp;

    public static class RateBucket {
        private final String minute; // "HH:mm"
        private final long count;

        public RateBucket(String minute, long count) {
            this.minute = minute;
            this.count = count;
        }

        public String getMinute() { return minute; }
        public long getCount() { return count; }
    }

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }

    public Map<String, Long> getBySeverity() { return bySeverity; }
    public void setBySeverity(Map<String, Long> bySeverity) { this.bySeverity = bySeverity; }

    public Map<String, Long> getByService() { return byService; }
    public void setByService(Map<String, Long> byService) { this.byService = byService; }

    public double getRatePerMinute() { return ratePerMinute; }
    public void setRatePerMinute(double ratePerMinute) { this.ratePerMinute = ratePerMinute; }

    public List<RateBucket> getRateHistory() { return rateHistory; }
    public void setRateHistory(List<RateBucket> rateHistory) { this.rateHistory = rateHistory; }

    public String getOldestTimestamp() { return oldestTimestamp; }
    public void setOldestTimestamp(String oldestTimestamp) { this.oldestTimestamp = oldestTimestamp; }

    public String getNewestTimestamp() { return newestTimestamp; }
    public void setNewestTimestamp(String newestTimestamp) { this.newestTimestamp = newestTimestamp; }

}
