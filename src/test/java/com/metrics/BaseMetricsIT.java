package com.metrics;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseMetricsIT {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected static final int STEP = 10;

    protected String iso(Instant ts) {
        return ts.toString();
    }

    protected void publish(String serviceId, List<Map<String, Object>> metrics) throws Exception {
        mockMvc.perform(post("/internal/mock/publish")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("serviceId", serviceId, "metrics", metrics)
                )))
                .andExpect(status().isOk());
    }

    protected List<Map<String, Object>> query(Map<String, Object> body) throws Exception {
        MvcResult result = mockMvc.perform(post("/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return objectMapper.convertValue(
                root.get("series"),
                new TypeReference<List<Map<String, Object>>>() {}
        );
    }

    protected List<Map<String, Object>> nonZero(List<Map<String, Object>> series) {
        return series.stream()
                .filter(p -> ((Number) p.get("value")).doubleValue() != 0)
                .toList();
    }
}
