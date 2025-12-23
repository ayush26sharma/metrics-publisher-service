package com.metrics.models.check;

import com.metrics.models.common.MetricType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;

@Builder
@Getter
@Setter
public class AggKey{
    String metricName;
    MetricType metricType;
    Instant alignedTs;
    String seriesKey;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AggKey aggKey = (AggKey) o;

        return Objects.equals(metricName, aggKey.metricName)
                && metricType == aggKey.metricType
                && Objects.equals(alignedTs, aggKey.alignedTs)
                && Objects.equals(seriesKey, aggKey.seriesKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(metricName, metricType, alignedTs, seriesKey);
    }
}
