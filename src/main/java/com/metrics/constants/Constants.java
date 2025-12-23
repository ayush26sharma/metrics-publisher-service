package com.metrics.constants;

import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;

public final class Constants {

    public static final class KafkaConstants {
        public static final String METRICS_RAW_TOPIC = "metrics-raw";
        public static final String METRICS_PROCESSED_TOPIC = "metrics-processed";
    }

    public static final class SqlConstants {

        public static final String SUM_SQL = """
        SELECT
          to_timestamp(
            floor(extract(epoch from ts) / ?) * ?
          ) AS bucket_ts,
          SUM(value) AS value
        FROM metric_samples
        WHERE metric_name = ?
          AND ts >= ?
          AND ts <= ?
          AND (?::jsonb IS NULL OR labels @> ?::jsonb)
        GROUP BY bucket_ts
        ORDER BY bucket_ts
        """;

        public static final String AVG_SQL = """
        SELECT
          to_timestamp(
            floor(extract(epoch from ts) / ?) * ?
          ) AS bucket_ts,
          AVG(value) AS value
        FROM metric_samples
        WHERE metric_name = ?
          AND ts >= ?
          AND ts <= ?
          AND (?::jsonb IS NULL OR labels @> ?::jsonb)
        GROUP BY bucket_ts
        ORDER BY bucket_ts
        """;

        public static final String RATE_SQL = """
        SELECT
          to_timestamp(
            floor(extract(epoch from ts) / ?) * ?
          ) AS bucket_ts,
          SUM(value) / ? AS value
        FROM metric_samples
        WHERE metric_name = ?
          AND ts >= ?
          AND ts <= ?
          AND (?::jsonb IS NULL OR labels @> ?::jsonb)
        GROUP BY bucket_ts
        ORDER BY bucket_ts
        """;

        public static final String P95_SQL = """
        SELECT
          to_timestamp(
            floor(extract(epoch from ts) / ?) * ?
          ) AS bucket_ts,
          sketch
        FROM metric_samples
        WHERE metric_name = ?
          AND ts >= ?
          AND ts <= ?
          AND (?::jsonb IS NULL OR labels @> ?::jsonb)
        ORDER BY bucket_ts
        """;
    }

}