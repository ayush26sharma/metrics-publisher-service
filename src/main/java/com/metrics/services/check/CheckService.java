package com.metrics.services.check;

import com.metrics.models.check.AggKey;
import com.metrics.models.check.ProcessedMetric;
import com.metrics.models.fetch.FetchMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CheckService {

    private final RawMetricExtractor extractor;
    private final CardinalityGuard cardinalityGuard;
    private final MetricAggregator aggregator;

    public List<ProcessedMetric> process(FetchMessage message) {
        Map<AggKey, ProcessedMetric> state = new HashMap<>();

        extractor.extract(message).stream()
                .filter(cardinalityGuard::isAllowed)
                .forEach(raw -> aggregator.aggregate(state, raw));

        return List.copyOf(state.values());
    }
}
