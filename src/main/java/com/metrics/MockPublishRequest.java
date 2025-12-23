package com.metrics;

import com.metrics.models.check.RawMetric;
import lombok.Data;

import java.util.List;

@Data
public class MockPublishRequest {
    private String serviceId;
    private List<RawMetric> metrics;
}
