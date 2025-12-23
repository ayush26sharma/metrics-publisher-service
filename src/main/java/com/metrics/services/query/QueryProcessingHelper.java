package com.metrics.services.query;

import com.metrics.models.common.Operation;
import com.metrics.models.query.QuantileSketch;
import com.metrics.models.query.TimeSeriesPoint;
import com.metrics.utils.SketchSerde;
import com.metrics.utils.TimeUtils;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class QueryProcessingHelper {

    List<TimeSeriesPoint> fillGapsAndBuildSeries(
            Instant from,
            Instant to,
            Duration step,
            Map<Instant, Double> bucketToValue)
    {
        List<TimeSeriesPoint> series = new ArrayList<>();

        Instant current = from;
        while (!current.isAfter(to)) {
            double value = bucketToValue.getOrDefault(current, 0.0);
            series.add(new TimeSeriesPoint(current, value));
            current = current.plus(step);
        }

        return series;
    }

    Map<Instant, Double> executeAggregation(Operation operation, List<Map<String, Object>> rows) {
        return switch (operation) {
            case SUM, AVG, RATE -> aggregateNumeric(rows);
            case P95 -> {
                Map<Instant, List<byte[]>> grouped = groupSketchesByBucket(rows);
                yield computeP95(grouped);
            }
            default -> throw new IllegalArgumentException("Unsupported operation: " + operation);
        };
    }

    private Map<Instant, Double> computeP95(Map<Instant, List<byte[]>> bucketToSketches) {
        Map<Instant, Double> result = new HashMap<>();

        for (var entry : bucketToSketches.entrySet()) {
            Instant ts = entry.getKey();
            List<byte[]> sketches = entry.getValue();

            QuantileSketch merged = null;

            for (byte[] raw : sketches) {
                if (raw == null || raw.length == 0) continue;

                QuantileSketch sketch = SketchSerde.deserialize(raw);
                if (merged == null) merged = sketch;
                else merged.merge(sketch);
            }

            double p95 = 0.0;
            if (merged != null) {
                double q = merged.getQuantile(0.95);
                if (!Double.isNaN(q) && !Double.isInfinite(q)) {
                    p95 = q;
                }
            }

            result.put(ts, p95);
        }

        return result;
    }


    private Map<Instant, List<byte[]>> groupSketchesByBucket (List<Map<String, Object>> rows) {
        Map<Instant, List<byte[]>> grouped = new HashMap<>();

        for (Map<String, Object> row : rows) {
            Instant ts = TimeUtils.toInstant(row.get("bucket_ts"));
            byte[] sketch = (byte[]) row.get("sketch");
            grouped.computeIfAbsent(ts, k -> new ArrayList<>()).add(sketch);
        }

        return grouped;
    }

    private Map<Instant, Double> aggregateNumeric (List<Map<String, Object>> rows) {
        Map<Instant, Double> result = new HashMap<>();

        for (Map<String, Object> row : rows) {
            Instant ts = TimeUtils.toInstant(row.get("bucket_ts"));
            Double value = ((Number) row.get("value")).doubleValue();
            result.put(ts, value);
        }

        return result;
    }
}
