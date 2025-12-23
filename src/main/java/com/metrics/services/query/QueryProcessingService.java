package com.metrics.services.query;

import com.metrics.models.query.QueryRequest;
import com.metrics.models.query.QueryResponse;
import com.metrics.models.query.TimeSeriesPoint;
import com.metrics.utils.TimeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class QueryProcessingService {

    private final MetricQueryRepository repository;
    private final QueryProcessingHelper queryProcessingHelper;

    public QueryResponse execute(QueryRequest request) {

        Duration step = TimeUtils.chooseStep(request.getFrom(), request.getTo());
        Instant from = TimeUtils.alignDown(request.getFrom(), step);
        Instant to = TimeUtils.alignDown(request.getTo(), step);

        String sql = repository.build(request.getOperation());
        Object[] args = repository.args(request, from, to, step.getSeconds(), request.getOperation());

        List<Map<String, Object>> rows = repository.query(sql, args);

        Map<Instant, Double> bucketToValue = queryProcessingHelper.executeAggregation(request.getOperation(), rows);
        List<TimeSeriesPoint> series = queryProcessingHelper.fillGapsAndBuildSeries(from, to, step, bucketToValue);

        return new QueryResponse(series);
    }

}
