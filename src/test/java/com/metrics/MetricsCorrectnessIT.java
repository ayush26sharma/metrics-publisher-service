package com.metrics;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class MetricsCorrectnessIT extends BaseMetricsIT {

    @Test
    void alwaysReturnsTimeseries() throws Exception {
        Instant now = Instant.now();

        List<Map<String, Object>> series = query(Map.of(
                "metricName", "nonexistent_metric",
                "operation", "SUM",
                "from", iso(now),
                "to", iso(now.plusSeconds(STEP))
        ));

        assertNotNull(series);
    }

    @Test
    void counterSumCorrect() throws Exception {
        Instant base = Instant.parse("2025-01-01T10:00:00Z");

        publish("svc-correct", List.of(
                Map.of("metricName","counter_a3","metricType","COUNTER","value",4,"timestamp",iso(base.plusSeconds(1)),"labels",Map.of("k","v")),
                Map.of("metricName","counter_a3","metricType","COUNTER","value",6,"timestamp",iso(base.plusSeconds(9)),"labels",Map.of("k","v"))
        ));

        Thread.sleep(3000);

        List<Map<String, Object>> nz = nonZero(query(Map.of(
                "metricName","svc-correct:counter_a3",
                "operation","SUM",
                "from",iso(base),
                "to",iso(base.plusSeconds(STEP)),
                "labels",Map.of("k","v")
        )));

        assertEquals(1, nz.size());
        assertEquals(10.0, ((Number) nz.get(0).get("value")).doubleValue());
    }

    @Test
    void counterNeverNegative() throws Exception {
        Instant base = Instant.now();

        publish("svc-counter", List.of(
                Map.of("metricName","counter_monotonic","metricType","COUNTER","value",-5,"timestamp",iso(base),"labels",Map.of())
        ));

        Thread.sleep(3000);

        List<Map<String, Object>> series = query(Map.of(
                "metricName","svc-counter:counter_monotonic",
                "operation","SUM",
                "from",iso(base),
                "to",iso(base.plusSeconds(STEP))
        ));

        assertTrue(series.stream()
                .allMatch(p -> ((Number)p.get("value")).doubleValue() >= 0));
    }

    @Test
    void rateCorrectness() throws Exception {
        Instant base = Instant.now();

        publish("svc-rate", List.of(
                Map.of("metricName","rate_metric01","metricType","COUNTER","value",20,"timestamp",iso(base),"labels",Map.of()),
                Map.of("metricName","rate_metric01","metricType","COUNTER","value",30,"timestamp",iso(base.plusSeconds(STEP)),"labels",Map.of())
        ));

        Thread.sleep(2000);

        List<Double> values = nonZero(query(Map.of(
                "metricName","svc-rate:rate_metric01",
                "operation","RATE",
                "from",iso(base),
                "to",iso(base.plusSeconds(STEP))
        ))).stream()
                .map(p -> ((Number)p.get("value")).doubleValue())
                .toList();

        assertTrue(values.stream().min(Double::compare).orElse(0.0) > 0);
        assertTrue(values.stream().max(Double::compare).orElse(0.0) <= 30.0 / STEP);
    }

    @Test
    void gaugeBucketLocality() throws Exception {
        Instant base = Instant.now();

        publish("svc-gauge", List.of(
                Map.of("metricName","gauge_x1","metricType","GAUGE","value",5,"timestamp",iso(base),"labels",Map.of()),
                Map.of("metricName","gauge_x1","metricType","GAUGE","value",9,"timestamp",iso(base.plusSeconds(5)),"labels",Map.of())
        ));

        Thread.sleep(3000);

        List<Double> values = nonZero(query(Map.of(
                "metricName","svc-gauge:gauge_x1",
                "operation","AVG",
                "from",iso(base),
                "to",iso(base.plusSeconds(STEP))
        ))).stream()
                .map(p -> ((Number)p.get("value")).doubleValue())
                .sorted()
                .toList();

        assertEquals(List.of(9.0), values);
    }

    @Test
    void gaugeNoCarryForward() throws Exception {
        Instant base = Instant.now();

        publish("svc-gauge", List.of(
                Map.of("metricName","gauge_cf","metricType","GAUGE","value",42,"timestamp",iso(base),"labels",Map.of())
        ));

        Thread.sleep(3000);

        List<Map<String, Object>> series = query(Map.of(
                "metricName","svc-gauge:gauge_cf",
                "operation","AVG",
                "from",iso(base.plusSeconds(STEP)),
                "to",iso(base.plusSeconds(STEP))
        ));

        assertTrue(series.stream()
                .allMatch(p -> ((Number)p.get("value")).doubleValue() == 0));
    }

    @Test
    void zeroFill() throws Exception {
        Instant base = Instant.now();

        publish("svc-gap", List.of(
                Map.of("metricName","gap_metric","metricType","COUNTER","value",1,"timestamp",iso(base),"labels",Map.of())
        ));

        Thread.sleep(3000);

        List<Map<String, Object>> series = query(Map.of(
                "metricName","svc-gap:gap_metric",
                "operation","SUM",
                "from",iso(base),
                "to",iso(base.plusSeconds(STEP * 3))
        ));

        assertTrue(series.size() >= 3);
        assertEquals(0.0, ((Number) series.get(1).get("value")).doubleValue());
    }

    @Test
    void labelFilterExactMatch() throws Exception {
        Instant base = Instant.now();

        publish("svc-label", List.of(
                Map.of("metricName","label_metric1","metricType","COUNTER","value",5,"timestamp",iso(base),"labels",Map.of("a","1")),
                Map.of("metricName","label_metric1","metricType","COUNTER","value",7,"timestamp",iso(base),"labels",Map.of("a","2"))
        ));

        Thread.sleep(3000);

        List<Map<String, Object>> nz = nonZero(query(Map.of(
                "metricName","svc-label:label_metric1",
                "operation","SUM",
                "from",iso(base),
                "to",iso(base.plusSeconds(STEP)),
                "labels",Map.of("a","1")
        )));

        assertEquals(5.0, ((Number) nz.get(0).get("value")).doubleValue());
    }

    @Test
    void percentileBounds() throws Exception {
        Instant base = Instant.now();

        for (int v : List.of(10,20,30,40,50)) {
            publish("svc-pctl", List.of(
                    Map.of("metricName","latency_ms017","metricType","GAUGE","value",v,"timestamp",iso(base),"labels",Map.of())
            ));
        }

        Thread.sleep(5000);

        double p95 = nonZero(query(Map.of(
                "metricName","svc-pctl:latency_ms017",
                "operation","P95",
                "from",iso(base),
                "to",iso(base.plusSeconds(STEP))
        ))).get(0).get("value") instanceof Number n ? n.doubleValue() : 0;

        assertTrue(p95 >= 40 && p95 <= 50);
    }
}
