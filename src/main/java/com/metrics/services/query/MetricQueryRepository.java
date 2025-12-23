package com.metrics.services.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.metrics.models.common.Operation;
import com.metrics.models.query.QueryRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static com.metrics.constants.Constants.SqlConstants.AVG_SQL;
import static com.metrics.constants.Constants.SqlConstants.P95_SQL;
import static com.metrics.constants.Constants.SqlConstants.RATE_SQL;
import static com.metrics.constants.Constants.SqlConstants.SUM_SQL;

@Repository
@RequiredArgsConstructor
public class MetricQueryRepository {

    private final JdbcTemplate jdbcTemplate;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public List<Map<String, Object>> query(String sql, Object[] args) {
        return jdbcTemplate.queryForList(sql, args);
    }

    public String build(Operation operation) {
        return switch (operation) {
            case SUM  -> SUM_SQL;
            case AVG  -> AVG_SQL;
            case RATE -> RATE_SQL;
            case P95  -> P95_SQL;
            default -> throw new IllegalArgumentException("Unsupported operation: " + operation);
        };
    }

    public Object[] args(QueryRequest request, Instant from, Instant to, long stepSeconds, Operation operation) {
        String labelsJson = toJson(request.getLabels());

        if (operation == Operation.RATE) {
            return new Object[] {
                    stepSeconds,
                    stepSeconds,
                    stepSeconds,
                    request.getMetricName(),
                    java.sql.Timestamp.from(from),
                    java.sql.Timestamp.from(to),
                    labelsJson,
                    labelsJson
            };
        }

        return new Object[] {
                stepSeconds,
                stepSeconds,
                request.getMetricName(),
                java.sql.Timestamp.from(from),
                java.sql.Timestamp.from(to),
                labelsJson,
                labelsJson
        };
    }

    private static String toJson(Object labels) {
        if (labels == null || ((java.util.Map<?, ?>) labels).isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(labels);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to serialize labels", e);
        }
    }
}
