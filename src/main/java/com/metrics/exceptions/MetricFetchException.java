package com.metrics.exceptions;

public class MetricFetchException extends RuntimeException {
    public MetricFetchException(String serviceId) {
        super(String.format("Error occurred while trying to fetch metric data from serviceId: %s", serviceId));
    }
}