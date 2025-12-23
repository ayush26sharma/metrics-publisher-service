package com.metrics.services.check;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.metrics.exceptions.MetricProcessException;
import com.metrics.kafka.MessagePublisher;
import com.metrics.models.check.ProcessedMetric;
import com.metrics.models.fetch.FetchMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.util.List;

import static com.metrics.constants.Constants.KafkaConstants.METRICS_PROCESSED_TOPIC;
import static com.metrics.constants.Constants.KafkaConstants.METRICS_RAW_TOPIC;


@Component
@Slf4j
@RequiredArgsConstructor
public class CheckConsumer {

    private final ObjectMapper objectMapper;
    private final CheckService checkService;
    private final MessagePublisher messagePublisher;

    @KafkaListener(topics = METRICS_RAW_TOPIC, groupId = "check-group")
    public void consume(String message) {
        try {
            log.info("metrics-check received incoming message: {}",message);
            FetchMessage fetchMessage = objectMapper.readValue(message, FetchMessage.class);

            List<ProcessedMetric> processedMetricList =  checkService.process(fetchMessage);
            processedMetricList.forEach(metric ->
                    messagePublisher.publish(
                            METRICS_PROCESSED_TOPIC,
                            metric.getMetricName(),
                            metric
                    )
            );

        } catch (Exception e) {
            log.error("Check failed to parse message: {}", e.getMessage(), e);
            throw new MetricProcessException(message);
        }
    }
}
