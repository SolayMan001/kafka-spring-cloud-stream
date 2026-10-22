package org.solaymanoumouou.kafkastream;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class PageEventFunctions {
    static final List<String> DEMO_PAGES = List.of("P1", "P2");

    private static final Logger LOGGER = LoggerFactory.getLogger(PageEventFunctions.class);

    @Bean
    Consumer<PageEvent> logVisits() {
        return event -> LOGGER.info("T2 page={} user={} duration={} ms",
                event.name(), event.user(), event.duration());
    }

    @Bean
    Supplier<PageEvent> generateVisits() {
        return () -> {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            String page = DEMO_PAGES.get(random.nextInt(DEMO_PAGES.size()));
            return new PageEvent(page, "demo", Instant.now(), random.nextLong(10, 1_010));
        };
    }
}
