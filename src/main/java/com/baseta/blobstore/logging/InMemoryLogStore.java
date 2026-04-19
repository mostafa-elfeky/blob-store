package com.baseta.blobstore.logging;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class InMemoryLogStore {

    private static final int MAX_ENTRIES = 200;
    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final Deque<ApplicationLogEntry> entries = new ArrayDeque<>();

    public synchronized void add(long timestampMillis, String level, String logger, String message) {
        if (entries.size() == MAX_ENTRIES) {
            entries.removeFirst();
        }
        entries.addLast(new ApplicationLogEntry(
                TIMESTAMP_FORMATTER.format(Instant.ofEpochMilli(timestampMillis)),
                level,
                logger,
                message
        ));
    }

    public synchronized List<ApplicationLogEntry> recentEntries() {
        List<ApplicationLogEntry> recent = new ArrayList<>(entries);
        java.util.Collections.reverse(recent);
        return recent;
    }
}
