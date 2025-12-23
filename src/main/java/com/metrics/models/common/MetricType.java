package com.metrics.models.common;

import lombok.Getter;

@Getter
public enum MetricType {
    GAUGE("gauge"),
    COUNTER("counter");

    private final String value;

    MetricType(String value) {
        this.value = value;
    }

    public static MetricType fromValue(String text) {
        for (MetricType type : MetricType.values()) {
            if (type.value.equalsIgnoreCase(text)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown metric type: " + text);
    }
}
