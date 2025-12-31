package com.metrics.services.check;

import com.fasterxml.jackson.databind.JsonNode;
import com.metrics.models.check.AggKey;
import com.metrics.models.check.ProcessedMetric;
import com.metrics.models.check.RawMetric;
import com.metrics.utils.TDigestSketch;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class MetricAggregator {

    private static final int WINDOW_SECONDS = 10;

    public void aggregate(Map<AggKey, ProcessedMetric> state, RawMetric raw) {
        AggKey key = buildAggKey(raw);

        state.compute(key, (k, existing) -> {
            if (existing == null) {
                return createInitialMetric(raw, k.getAlignedTs());
            }
            applyAggregation(existing, raw);
            return existing;
        });
    }

    private AggKey buildAggKey(RawMetric raw) {
        return AggKey.builder()
                .metricName(raw.getMetricName())
                .metricType(raw.getMetricType())
                .alignedTs(align(raw.getTimestamp()))
                .seriesKey(buildSeriesKey(raw.getMetricName(), raw.getLabels()))
                .build();
    }

    private String buildSeriesKey(String metricName, JsonNode labels) {
        if (labels == null || labels.isEmpty()) {
            return metricName;
        }

        List<String> parts = new ArrayList<>();
        labels.fieldNames()
                .forEachRemaining(f -> parts.add(f + "=" + labels.get(f).asText()));

        Collections.sort(parts);
        return metricName + "|" + String.join(",", parts);
    }


    private Instant align(Instant ts) {
        long epoch = ts.getEpochSecond();
        return Instant.ofEpochSecond((epoch / WINDOW_SECONDS) * WINDOW_SECONDS);
    }

    private ProcessedMetric createInitialMetric(RawMetric raw, Instant alignedTs) {
        ProcessedMetric metric = ProcessedMetric.builder()
                .metricName(raw.getMetricName())
                .metricType(raw.getMetricType())
                .processedTimestamp(alignedTs)
                .value(raw.getValue())
                .labels(raw.getLabels())
                .build();

        TDigestSketch sketch = TDigestSketch.create();
        sketch.digest.add(raw.getValue());
        metric.setSketch(sketch.serialize());

        return metric;
    }

    private void applyAggregation(ProcessedMetric existing, RawMetric raw) {
        switch (existing.getMetricType()) {
            case COUNTER -> existing.setValue(existing.getValue() + raw.getValue());
            case GAUGE -> existing.setValue(raw.getValue());
            default -> throw new IllegalStateException("Unsupported metric type");
        }

        TDigestSketch existingSketch = TDigestSketch.deserialize(existing.getSketch());
        existingSketch.digest.add(raw.getValue());
        existing.setSketch(existingSketch.serialize());
    }
}
