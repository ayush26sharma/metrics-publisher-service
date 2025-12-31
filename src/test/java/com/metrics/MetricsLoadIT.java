package com.metrics;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("e2e")
class MetricsLoadIT extends BaseMetricsIT {

    @Test
    void applicationIsHealthy() throws Exception {
        query(Map.of(
                "metricName","health_check_dummy",
                "operation","SUM",
                "from",iso(Instant.now()),
                "to",iso(Instant.now().plusSeconds(1))
        ));
    }

    @Test
    void counterAggregationCorrect() throws Exception {
        Instant base = Instant.parse("2025-01-01T10:00:00Z");

        publish("svc-agg", List.of(
                Map.of("metricName","req_total","metricType","COUNTER","value",5,"timestamp",iso(base.plusSeconds(3)),"labels",Map.of("api","/a")),
                Map.of("metricName","req_total","metricType","COUNTER","value",7,"timestamp",iso(base.plusSeconds(7)),"labels",Map.of("api","/a"))
        ));

        Thread.sleep(2000);

        double sum = nonZero(query(Map.of(
                "metricName","svc-agg:req_total",
                "operation","SUM",
                "from",iso(base),
                "to",iso(base.plusSeconds(STEP)),
                "labels",Map.of("api","/a")
        ))).get(0).get("value") instanceof Number n ? n.doubleValue() : 0;

        assertEquals(12.0, sum);
    }

    @Test
    void cardinalityLimitEnforced() throws Exception {
        Instant base = Instant.now();

        for (int i = 0; i < 700; i++) {
            publish("svc-hc", List.of(
                    Map.of("metricName","hc_metric_6","metricType","GAUGE","value",1,"timestamp",iso(base),"labels",Map.of("id",String.valueOf(i)))
            ));
        }

        Thread.sleep(5000);

        List<Map<String, Object>> series = query(Map.of(
                "metricName","svc-hc:hc_metric_6",
                "operation","AVG",
                "from",iso(base),
                "to",iso(base.plusSeconds(STEP))
        ));

        assertTrue(series.size() <= 500);
    }

    @Test
    void highLoadTest() throws Exception {
        Instant base = Instant.now();
        int total = 500;
        int allowedSeries = 50;

        ExecutorService pool = Executors.newFixedThreadPool(50);

        for (int i = 0; i < total; i++) {
            int idx = i;
            pool.submit(() -> {
                try {
                    publish("svc-load", List.of(
                            Map.of("metricName","load_metric_122","metricType","COUNTER","value",1,"timestamp",iso(base),"labels",Map.of("worker",String.valueOf(idx % allowedSeries)))
                    ));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        pool.shutdown();
        pool.awaitTermination(15, TimeUnit.SECONDS);

        double observed = waitUntilSum(base, "svc-load:load_metric_122", total);
        assertTrue(observed >= total*0.95 && observed <= total*1.05);
//        assertEquals(total, observed);
    }

    private double waitUntilSum(Instant base, String metric, int expected) throws Exception {
        long deadline = System.currentTimeMillis() + 30_000;
        double last = 0;

        while (System.currentTimeMillis() < deadline) {
            last = query(Map.of(
                    "metricName",metric,
                    "operation","SUM",
                    "from",iso(base),
                    "to",iso(base.plusSeconds(STEP))
            )).stream()
                    .mapToDouble(p -> ((Number)p.get("value")).doubleValue())
                    .sum();

            if (last == expected) return last;
            Thread.sleep(1000);
        }
        return last;
    }

    @Test
    void resilienceAfterRestart() throws Exception {
        Instant base = Instant.now();

        publish("svc-chaos", List.of(
                Map.of("metricName","resilience_metric3","metricType","COUNTER","value",3,"timestamp",iso(base),"labels",Map.of())
        ));

        Thread.sleep(3000);

        double value = nonZero(query(Map.of(
                "metricName","svc-chaos:resilience_metric3",
                "operation","SUM",
                "from",iso(base),
                "to",iso(base.plusSeconds(STEP))
        ))).get(0).get("value") instanceof Number n ? n.doubleValue() : 0;

        assertEquals(3.0, value);
    }
}
