package org.solaymanoumouou.kafkastream;

import java.time.Instant;

import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
class PageEventController {
    private final StreamBridge streamBridge;

    PageEventController(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    @GetMapping("/publish")
    PageEvent publish(@RequestParam String name,
                      @RequestParam(defaultValue = "T2") String topic,
                      @RequestParam(defaultValue = "Solayman Oumouou") String user,
                      @RequestParam(defaultValue = "350") long duration) {
        String binding = switch (topic) {
            case "T2" -> "publishVisit-out-0";
            case "T3" -> "generateVisits-out-0";
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use topic T2 or T3");
        };
        if (name.isBlank() || name.length() > 80 || user.isBlank() || user.length() > 80 || duration < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide a valid name, user and duration");
        }

        PageEvent event = new PageEvent(name, user, Instant.now(), duration);
        if (!streamBridge.send(binding, event)) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Kafka binding rejected the event");
        }
        return event;
    }
}
