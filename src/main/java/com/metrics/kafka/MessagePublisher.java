package com.metrics.kafka;

import com.metrics.exceptions.KafkaMessagePublisherException;
import com.metrics.models.common.BaseMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessagePublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publish(String topic, String key, BaseMessage message) {
        try {
            String payload = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(topic, key, payload);
        } catch (Exception e) {
            log.error("Failed to publish message to Kafka: {}", e.getMessage(), e);
            throw new KafkaMessagePublisherException(topic, key, message);
        }
    }
}
