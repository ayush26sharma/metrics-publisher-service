package com.metrics.services.check;

import com.metrics.models.check.ProcessedMetric;
import com.metrics.models.check.AggKey;
import com.metrics.models.fetch.FetchMessage;
import com.metrics.models.check.RawMetric;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckService {

    private final CheckServiceHelper checkServiceHelper;

    public List<ProcessedMetric> process(FetchMessage message) {

        Map<AggKey, ProcessedMetric> aggregated = new HashMap<>();
        for (RawMetric raw : checkServiceHelper.extractRawMetrics(message)) {
            if (!checkServiceHelper.isAllowed(raw)) {
                continue;
            }
            checkServiceHelper.aggregate(aggregated, raw);
        }
        return new ArrayList<>(aggregated.values());
    }


}

