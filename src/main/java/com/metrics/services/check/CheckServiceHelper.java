package com.metrics.services.check;

import com.fasterxml.jackson.databind.JsonNode;
import com.metrics.config.AppConfigStore;
import com.metrics.models.common.MetricType;
import com.metrics.models.check.ProcessedMetric;
import com.metrics.models.check.AggKey;
import com.metrics.models.fetch.FetchMessage;
import com.metrics.models.check.RawMetric;
import com.metrics.models.query.QuantileSketch;
import com.metrics.utils.TDigestSketch;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.metrics.models.common.MetricType.COUNTER;

@Service
@RequiredArgsConstructor
public class CheckServiceHelper {

    private static final int WINDOW_SECONDS = 10;
    private final StringRedisTemplate redisTemplate;
    private final AppConfigStore appConfigStore;

    List<RawMetric> extractRawMetrics(FetchMessage message) {
        JsonNode metricsNode = message.getRawPayload().get("metrics");
        String serviceId = message.getServiceId();
        if (metricsNode == null || !metricsNode.isArray()) {
            return List.of();
        }

        List<RawMetric> result = new ArrayList<>();
        for (JsonNode node : metricsNode) {
            RawMetric rawMetric = parseRawMetric(node, serviceId);
            if(rawMetric != null && COUNTER.equals(rawMetric.getMetricType()) && rawMetric.getValue()<=0) {
                continue;
            }
            result.add(rawMetric);
        }
        return result;
    }


    private RawMetric parseRawMetric(JsonNode node, String serviceId) {
        return RawMetric.builder()
                .metricName(serviceId.concat(":".concat(node.get("metricName").asText())))
                .metricType(MetricType.valueOf(node.get("metricType").asText().toUpperCase()))
                .labels(node.get("labels"))
                .value(node.get("value").asDouble())
                .timestamp(Instant.parse(node.get("timestamp").asText())).build();
    }


    boolean isAllowed(RawMetric raw) {
        return allow(raw.getMetricName(), raw.getLabels());
    }


    public boolean allow(String metricName, JsonNode labels) {

        if (labels != null && labels.size() > appConfigStore.getLabelSizeLimit()) {
            return false;
        }

        String seriesKey = buildSeriesKey(metricName, labels);
        String redisKey = "metric:series:" + metricName;

        Long count = redisTemplate.opsForSet().size(redisKey);

        if (count != null && count >= appConfigStore.getMetricSeriesLimit()) {
            return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(redisKey, seriesKey));
        }

        redisTemplate.opsForSet().add(redisKey, seriesKey);
        return true;
    }


    private AggKey buildAggKey(RawMetric raw) {
        return AggKey.builder()
                .metricName(raw.getMetricName())
                .metricType(raw.getMetricType())
                .alignedTs(align(raw.getTimestamp()))
                .seriesKey(buildSeriesKey(raw.getMetricName(), raw.getLabels())).build();
    }


    private Instant align(Instant ts) {
        long epoch = ts.getEpochSecond();
        return Instant.ofEpochSecond((epoch / WINDOW_SECONDS) * WINDOW_SECONDS);
    }


    void aggregate(Map<AggKey, ProcessedMetric> state, RawMetric raw) {
        AggKey key = buildAggKey(raw);
        state.compute(key, (k, existing) -> {

            if (existing == null) return createInitialMetric(raw, k.getAlignedTs());

            applyAggregation(existing, raw);
            return existing;
        });
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
        sketch.merge(singleValueSketch(raw.getValue()));
        metric.setSketch(sketch.serialize());

        return metric;
    }


    private void applyAggregation(ProcessedMetric existing, RawMetric raw) {
        switch (existing.getMetricType()) {
            case COUNTER -> existing.setValue(existing.getValue() + raw.getValue());
            case GAUGE -> existing.setValue(raw.getValue());
            default -> throw new IllegalStateException("Unsupported metric type: " + existing.getMetricType());
        }

        TDigestSketch existingSketch = TDigestSketch.deserialize(existing.getSketch());
        TDigestSketch incoming = TDigestSketch.create();
        incoming.digest.add(raw.getValue());

        existingSketch.merge(incoming);
        existing.setSketch(existingSketch.serialize());
    }


    private QuantileSketch singleValueSketch(double value) {
        TDigestSketch sketch = TDigestSketch.create();
        sketch.merge(
                TDigestSketch.deserialize(
                        TDigestSketch.create().serialize()
                )
        );
        sketch.getQuantile(0.0);
        sketch.digest.add(value);
        return sketch;
    }


    private String buildSeriesKey(String metricName, JsonNode labels) {
        if (labels == null || labels.isEmpty()) {
            return metricName;
        }

        List<String> parts = new ArrayList<>();
        labels.fieldNames().forEachRemaining(
                f -> parts.add(f + "=" + labels.get(f).asText())
        );
        Collections.sort(parts);

        return metricName + "|" + String.join(",", parts);
    }

}
