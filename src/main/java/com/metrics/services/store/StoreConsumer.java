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
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

import static com.metrics.constants.Constants.KafkaConstants.METRICS_PROCESSED_TOPIC;


@Component
@RequiredArgsConstructor
@Slf4j
public class StoreConsumer {

    private final MetricStoreRepository metricStoreRepository;
    private final ObjectMapper objectMapper;
    private final AppConfigStore appConfigStore;

    private final Queue<ProcessedMetric> buffer = new ConcurrentLinkedQueue<>();
    private volatile Instant lastFlush = Instant.now();
    private final ReentrantLock flushLock = new ReentrantLock();


    @KafkaListener(topics = METRICS_PROCESSED_TOPIC, groupId = "metrics-store")
    public void consume(String message) throws Exception {
        ProcessedMetric metric = objectMapper.readValue(message, ProcessedMetric.class);
        buffer.add(metric);

        if (shouldFlush()) {
            flushLock.lock();
            try {
                if (shouldFlush()) flush();
            } finally {
                flushLock.unlock();
            }
        }
    }


//  @Scheduled(fixedRate = 2000)
    private void flush() {
        List<ProcessedMetric> batch = new ArrayList<>();
        ProcessedMetric m;
        while ((m = buffer.poll()) != null && batch.size() < appConfigStore.getFlushBatchSize()) {
            batch.add(m);
        }
        if (!batch.isEmpty()) {
            metricStoreRepository.batchInsert(batch);
            lastFlush = Instant.now();
        }
    }

    private boolean shouldFlush() {
        return buffer.size() >= appConfigStore.getFlushBatchSize() ||
                Duration.between(lastFlush, Instant.now()).compareTo(appConfigStore.getFlushInterval()) > 0;
    }
}
