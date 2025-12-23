package com.metrics.models.query;

import com.metrics.models.common.Operation;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
public class QueryRequest {

    private String metricName;
    private Operation operation;
    private Map<String, String> labels;
    private Instant from;
    private Instant to;
}
