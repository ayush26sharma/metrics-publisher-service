package com.metrics.models.query;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class QueryResponse {
    private List<TimeSeriesPoint> series;
}
