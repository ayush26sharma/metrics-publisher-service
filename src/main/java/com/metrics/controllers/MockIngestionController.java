package com.metrics.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.metrics.MockPublishRequest;
import com.metrics.models.fetch.FetchMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/mock")
@Profile("test")
@RequiredArgsConstructor
public class MockIngestionController {

    private final KafkaTemplate<Object, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @PostMapping("/publish")
    public ResponseEntity<Void> publish(@RequestBody MockPublishRequest request) throws JsonProcessingException {

        FetchMessage msg = FetchMessage.from(request, objectMapper);
        String jsonMessage = objectMapper.writeValueAsString(msg);
//        System.out.println(jsonMessage);
        kafkaTemplate.send("metrics-raw", jsonMessage);
        return ResponseEntity.ok().build();
    }
}
