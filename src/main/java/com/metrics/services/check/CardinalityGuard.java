package com.metrics.services.check;

import com.fasterxml.jackson.databind.JsonNode;
import com.metrics.config.AppConfigStore;
import com.metrics.models.check.RawMetric;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CardinalityGuard {

    private final StringRedisTemplate redisTemplate;
    private final AppConfigStore appConfigStore;

    public boolean isAllowed(RawMetric raw) {
        return allow(raw.getMetricName(), raw.getLabels());
    }

    private boolean allow(String metricName, JsonNode labels) {

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
}
