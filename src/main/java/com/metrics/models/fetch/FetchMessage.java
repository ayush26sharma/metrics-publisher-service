package com.metrics.models.fetch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.metrics.MockPublishRequest;
import com.metrics.models.common.BaseMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
public class FetchMessage extends BaseMessage {

    private String serviceId;
    private Instant fetchTimestamp;
    private JsonNode rawPayload;

    public static FetchMessage from(
            MockPublishRequest request,
            ObjectMapper objectMapper
    ) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.set("metrics", objectMapper.valueToTree(request.getMetrics()));

        return new FetchMessage(
                request.getServiceId(),
                Instant.now(),
                payload
        );
    }
}
