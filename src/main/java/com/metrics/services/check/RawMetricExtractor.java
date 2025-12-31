package com.metrics.services.check;

import com.fasterxml.jackson.databind.JsonNode;
import com.metrics.models.common.MetricType;
import com.metrics.models.fetch.FetchMessage;
import com.metrics.models.check.RawMetric;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static com.metrics.models.common.MetricType.COUNTER;

@Component
public class RawMetricExtractor {

    public List<RawMetric> extract(FetchMessage message) {
        JsonNode metricsNode = message.getRawPayload().get("metrics");
        if (metricsNode == null || !metricsNode.isArray()) {
            return List.of();
        }

        List<RawMetric> result = new ArrayList<>();
        for (JsonNode node : metricsNode) {
            RawMetric raw = parse(node, message.getServiceId());
            if (raw == null) continue;

            if (COUNTER.equals(raw.getMetricType()) && raw.getValue() <= 0) continue;

            result.add(raw);
        }
        return result;
    }

    private RawMetric parse(JsonNode node, String serviceId) {
        return RawMetric.builder()
                .metricName(serviceId + ":" + node.get("metricName").asText())
                .metricType(MetricType.valueOf(node.get("metricType").asText().toUpperCase()))
                .labels(node.get("labels"))
                .value(node.get("value").asDouble())
                .timestamp(Instant.parse(node.get("timestamp").asText()))
                .build();
    }
}
