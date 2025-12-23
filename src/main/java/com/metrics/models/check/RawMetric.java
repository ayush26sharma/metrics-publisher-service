package com.metrics.models.check;

import com.fasterxml.jackson.databind.JsonNode;
import com.metrics.models.common.MetricType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Builder
@Getter
@Setter
public class RawMetric{
    String metricName;
    MetricType metricType;
    JsonNode labels;
    double value;
    Instant timestamp;
}
