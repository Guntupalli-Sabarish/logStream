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

    /**
     * Computes all stats from the current in-memory snapshot.
     * O(n) scan at most 5,000 entries — fast enough on-demand.
     */
    public LogStats compute(List<Log> logs) {
        LogStats stats = new LogStats();
        stats.setTotal(logs.size());

        // Severity distribution
        Map<String, Long> bySeverity = logs.stream()
                .collect(Collectors.groupingBy(
                        l -> l.getSeverity() != null ? l.getSeverity().name() : "UNKNOWN",
                        Collectors.counting()));
        stats.setBySeverity(bySeverity);

        // Service distribution (only logs that carry a service name)
        Map<String, Long> byService = logs.stream()
                .filter(l -> l.getService() != null && !l.getService().isBlank())
                .collect(Collectors.groupingBy(Log::getService, Collectors.counting()));
        stats.setByService(byService);

        // Rate history — last 10 one-minute buckets (newest last)
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

        // Rate per minute averaged over the last 5 minutes
        Instant fiveMinAgo = now.minus(5, ChronoUnit.MINUTES);
        long recentCount = logs.stream()
                .filter(l -> l.getTimestamp() != null)
                .filter(l -> l.getTimestamp().toInstant().isAfter(fiveMinAgo))
                .count();
        stats.setRatePerMinute(Math.round((recentCount / 5.0) * 10.0) / 10.0);

        // Oldest / newest timestamps
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
