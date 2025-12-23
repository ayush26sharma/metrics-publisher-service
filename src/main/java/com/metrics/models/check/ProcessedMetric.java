package com.metrics.models.check;

import com.fasterxml.jackson.databind.JsonNode;
import com.metrics.models.common.BaseMessage;
import com.metrics.models.common.MetricType;
import lombok.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(callSuper = true)
public class ProcessedMetric extends BaseMessage {

    private String metricName;
    private MetricType metricType;
    private Instant processedTimestamp;
    private byte[] sketch;
    private Double value;
    private JsonNode labels;
}
