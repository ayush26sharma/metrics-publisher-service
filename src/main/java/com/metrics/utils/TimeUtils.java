package com.metrics.utils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;


public class TimeUtils {

    public static Duration chooseStep(Instant from, Instant to) {
        Duration range = Duration.between(from, to);

        if (range.toHours() <= 1) {
            return Duration.ofSeconds(10);
        } else if (range.toHours() <= 6) {
            return Duration.ofSeconds(30);
        } else if (range.toHours() <= 24) {
            return Duration.ofMinutes(1);
        } else {
            return Duration.ofMinutes(5);
        }
    }

    public static Instant alignDown(Instant ts, Duration step) {
        long seconds = ts.getEpochSecond();
        long stepSeconds = step.getSeconds();
        return Instant.ofEpochSecond((seconds / stepSeconds) * stepSeconds);
    }

    public static List<Instant> generateBuckets(Instant alignedFrom, Instant alignedTo, Duration step) {
        List<Instant> buckets = new ArrayList<>();
        Instant current = alignedFrom;

        while (!current.isAfter(alignedTo)) {
            buckets.add(current);
            current = current.plus(step);
        }

        return buckets;
    }

    public static Instant toInstant(Object dbValue) {

        if (dbValue instanceof Instant) return (Instant) dbValue;

        else if (dbValue instanceof OffsetDateTime) return ((OffsetDateTime) dbValue).toInstant();

        else if (dbValue instanceof LocalDateTime) {
            return ((LocalDateTime) dbValue)
                    .atZone(ZoneOffset.UTC)
                    .toInstant();
        }

        else if (dbValue instanceof Timestamp) return ((Timestamp) dbValue).toInstant();

        else throw new IllegalArgumentException("Unsupported timestamp type: " + dbValue.getClass());
    }

}
