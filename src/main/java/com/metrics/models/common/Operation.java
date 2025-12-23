package com.metrics.models.common;

import lombok.Getter;

@Getter
public enum Operation {

    SUM("sum"),
    AVG("avg"),
    RATE("rate"),
    P95("p95");

    private final String value;

    Operation(String value) {
        this.value = value;
    }

    public static Operation fromValue(String text) {
        for (Operation op : Operation.values()) {
            if (op.value.equalsIgnoreCase(text)) {
                return op;
            }
        }
        throw new IllegalArgumentException("Unknown operation: " + text);
    }

}
