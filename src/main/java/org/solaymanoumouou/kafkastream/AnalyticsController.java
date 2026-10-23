package org.solaymanoumouou.kafkastream;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;

import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;
import org.springframework.cloud.stream.binder.kafka.streams.InteractiveQueryService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@RestController
class AnalyticsController {
    private final InteractiveQueryService queries;

    AnalyticsController(InteractiveQueryService queries) {
        this.queries = queries;
    }

    @GetMapping(path = "/analytics", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<Snapshot> analytics() {
        return Flux.interval(Duration.ZERO, Duration.ofSeconds(1))
                .publishOn(Schedulers.boundedElastic())
                .map(tick -> currentSnapshot());
    }

    private Snapshot currentSnapshot() {
        long windowSeconds = VisitAnalytics.WINDOW_SIZE.toSeconds();
        long now = Instant.now().getEpochSecond();
        Instant start = Instant.ofEpochSecond(Math.floorDiv(now, windowSeconds) * windowSeconds);

        ReadOnlyWindowStore<String, Long> store = queries.getQueryableStore(
                VisitAnalytics.WINDOW_STORE, QueryableStoreTypes.windowStore());
        Map<String, Long> counts = new TreeMap<>();
        PageEventFunctions.DEMO_PAGES.forEach(page -> counts.put(page, 0L));
        try (var windows = store.fetchAll(start, start)) {
            windows.forEachRemaining(entry -> counts.put(entry.key.key(), entry.value));
        }
        return new Snapshot(start, counts);
    }

    record Snapshot(Instant windowStart, Map<String, Long> counts) {
    }
}
