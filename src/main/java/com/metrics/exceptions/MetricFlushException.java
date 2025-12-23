package com.metrics.exceptions;

public class MetricFlushException extends RuntimeException {
    public MetricFlushException() {
        super("Error while flushing metrics in database");
    }
}