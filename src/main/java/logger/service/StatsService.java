package logger.service;
import logger.model.LogStats;
import logger.pojo.Log;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
@Service
public class StatsService {
    private static final DateTimeFormatter BUCKET_FMT =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());
    public LogStats compute(List<Log> logs) {
        LogStats stats = new LogStats();
        stats.setTotal(logs.size());
        Map<String, Long> bySeverity = logs.stream()
                .collect(Collectors.groupingBy(
                        l -> l.getSeverity() != null ? l.getSeverity().name() : "UNKNOWN",
                        Collectors.counting()));
        stats.setBySeverity(bySeverity);
        Map<String, Long> byService = logs.stream()
                .filter(l -> l.getService() != null && !l.getService().isBlank())
                .collect(Collectors.groupingBy(Log::getService, Collectors.counting()));
        stats.setByService(byService);
        Instant now = Instant.now();
        List<LogStats.RateBucket> rateHistory = new ArrayList<>();
        for (int i = 9; i >= 0; i--) {
            Instant bucketStart = now.truncatedTo(ChronoUnit.MINUTES).minus(i, ChronoUnit.MINUTES);
            Instant bucketEnd   = bucketStart.plus(1, ChronoUnit.MINUTES);
            long count = logs.stream()
                    .filter(l -> l.getTimestamp() != null)
                    .filter(l -> {
                        Instant ts = l.getTimestamp().toInstant();
                        return !ts.isBefore(bucketStart) && ts.isBefore(bucketEnd);
                    })
                    .count();
            rateHistory.add(new LogStats.RateBucket(BUCKET_FMT.format(bucketStart), count));
        }
        stats.setRateHistory(rateHistory);
        Instant fiveMinAgo = now.minus(5, ChronoUnit.MINUTES);
        long recentCount = logs.stream()
                .filter(l -> l.getTimestamp() != null)
                .filter(l -> l.getTimestamp().toInstant().isAfter(fiveMinAgo))
                .count();
        stats.setRatePerMinute(Math.round((recentCount / 5.0) * 10.0) / 10.0);
        logs.stream()
                .filter(l -> l.getTimestamp() != null)
                .map(l -> l.getTimestamp().toInstant())
                .min(Comparator.naturalOrder())
                .ifPresent(ts -> stats.setOldestTimestamp(ts.toString()));
        logs.stream()
                .filter(l -> l.getTimestamp() != null)
                .map(l -> l.getTimestamp().toInstant())
                .max(Comparator.naturalOrder())
                .ifPresent(ts -> stats.setNewestTimestamp(ts.toString()));
        return stats;
    }
}
