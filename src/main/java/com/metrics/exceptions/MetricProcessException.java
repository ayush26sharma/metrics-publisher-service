package com.metrics.exceptions;

public class MetricProcessException extends RuntimeException {
    public MetricProcessException(String message) {
        super(String.format("Error occurred while trying to process metrics: %s", message));
    }
}