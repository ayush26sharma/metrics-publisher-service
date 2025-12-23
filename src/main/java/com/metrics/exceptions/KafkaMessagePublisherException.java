package com.metrics.exceptions;

import com.metrics.models.common.BaseMessage;

public class KafkaMessagePublisherException extends RuntimeException {
    public KafkaMessagePublisherException(String topic, String key, BaseMessage message) {
        super(String.format("Error while publishing message to topic: %s key: %s message: %s", topic, key, message.toString()));
    }
}