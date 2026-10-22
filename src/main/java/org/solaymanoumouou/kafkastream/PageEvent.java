package org.solaymanoumouou.kafkastream;

import java.time.Instant;

public record PageEvent(String name, String user, Instant date, long duration) {
}
