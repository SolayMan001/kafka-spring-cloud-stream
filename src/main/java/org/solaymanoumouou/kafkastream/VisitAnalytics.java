package org.solaymanoumouou.kafkastream;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KGroupedStream;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class VisitAnalytics {
    static final String WINDOW_STORE = "count-store";
    static final Duration WINDOW_SIZE = Duration.ofSeconds(5);

    @Bean
    @SuppressWarnings("unchecked")
    Function<KStream<String, PageEvent>, KStream<String, Long>[]> processVisits() {
        return events -> {
            KStream<String, Long> durations = events
                    .filter((key, event) -> event != null && event.duration() > 100)
                    .map((key, event) -> KeyValue.pair(event.name(), event.duration()));
            KGroupedStream<String, Long> byPage = durations.groupByKey(
                    Grouped.with(Serdes.String(), Serdes.Long()));
            KStream<String, Long> totals = byPage.count().toStream();
            KStream<String, Long> windowCounts = byPage
                    .windowedBy(TimeWindows.ofSizeWithNoGrace(WINDOW_SIZE))
                    .count(Materialized.as(WINDOW_STORE))
                    .toStream()
                    .selectKey((window, count) -> window.key());

            return List.of(durations, totals, windowCounts).toArray(KStream[]::new);
        };
    }
}
