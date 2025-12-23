package com.metrics.services.store;

import com.metrics.models.check.ProcessedMetric;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;

@Repository
@RequiredArgsConstructor
@Slf4j
public class MetricStoreRepository {

    private final JdbcTemplate jdbcTemplate;
    private static final String INSERT_SQL = """
        INSERT INTO metric_samples (
            ts, metric_name, metric_type, value, sketch, labels
        ) VALUES (?, ?, ?, ?, ?, ?)
        """;

    public void batchInsert(List<ProcessedMetric> batch) {
        log.info("Batch insert begun");
        jdbcTemplate.batchUpdate(
            INSERT_SQL,
            batch,
            batch.size(),
            (statement, metric) -> {
                statement.setObject(1, Timestamp.from(metric.getProcessedTimestamp()));
                statement.setString(2, metric.getMetricName());
                statement.setString(3, String.valueOf(metric.getMetricType()));
                statement.setObject(4, metric.getValue());
                statement.setBytes(5, metric.getSketch());
                statement.setObject(6, metric.getLabels(), Types.OTHER);
            }
        );
        log.info("Batch insert successful");
    }
}
