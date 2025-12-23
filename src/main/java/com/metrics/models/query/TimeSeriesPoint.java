package com.metrics.models.query;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class TimeSeriesPoint {
    private Instant ts;
    private double value;
}
