package com.metrics.services.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.metrics.config.AppConfigStore;
import com.metrics.exceptions.MetricFlushException;
import com.metrics.models.check.ProcessedMetric;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static com.metrics.constants.Constants.KafkaConstants.METRICS_PROCESSED_TOPIC;

@Component
@RequiredArgsConstructor
@Slf4j
public class StoreConsumer {

    private final MetricStoreRepository metricStoreRepository;
    private final ObjectMapper objectMapper;
    private final AppConfigStore appConfigStore;

    private final List<ProcessedMetric> buffer = new ArrayList<>();
    private Instant lastFlush = Instant.now();

    @KafkaListener(topics = METRICS_PROCESSED_TOPIC, groupId = "metrics-store")
    public void consume(String message) throws Exception {
        log.info("metrics-store incoming message: {}",message);

        ProcessedMetric metric = objectMapper.readValue(message, ProcessedMetric.class);
        buffer.add(metric);

        if (shouldFlush()) flush();

    }

//    @Scheduled(fixedRate = 2000)
    private void flush() {

        if (buffer.isEmpty()) return;

        try {
            metricStoreRepository.batchInsert(buffer);
        } catch (Exception e) {
            log.error("Batch insert failed: {}", e.getMessage(), e);
            throw new MetricFlushException();
        } finally {
            buffer.clear();
            lastFlush = Instant.now();
        }
    }

    private boolean shouldFlush() {
        return buffer.size() >= appConfigStore.getFlushBatchSize() ||
                Duration.between(lastFlush, Instant.now()).compareTo(appConfigStore.getFlushInterval()) > 0;
    }
}
