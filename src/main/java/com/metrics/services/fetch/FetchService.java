package com.metrics.services.fetch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.metrics.config.FetchProperties;
import com.metrics.exceptions.MetricFetchException;
import com.metrics.models.common.ServiceConfig;
import com.metrics.kafka.MessagePublisher;
import com.metrics.models.fetch.FetchMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.time.Instant;

import static com.metrics.constants.Constants.KafkaConstants.METRICS_RAW_TOPIC;

@Service
@Slf4j
@RequiredArgsConstructor
public class FetchService {

    private final FetchProperties fetchProperties;
    private final MessagePublisher messagePublisher;
    private final ObjectMapper objectMapper;

    private final RestTemplate restTemplate = new RestTemplate();

    public void pollAllServices() {
        for (ServiceConfig service : fetchProperties.getServices()) {
            pollSingleService(service);
        }
    }

    private void pollSingleService(ServiceConfig service) {
        try {
            String response = restTemplate.getForObject(service.getUrl(), String.class);
            JsonNode payload = objectMapper.readTree(response);

            FetchMessage message = FetchMessage.builder()
                    .serviceId(service.getId())
                    .fetchTimestamp(Instant.now())
                    .rawPayload(payload)
                    .build();

            messagePublisher.publish(METRICS_RAW_TOPIC, service.getId(), message);

        } catch (Exception e) {
            log.error("Fetch failed for service {}, reason: {}", service.getId(), e.getMessage(), e);
            throw new MetricFetchException(service.getId());
        }
    }
}
