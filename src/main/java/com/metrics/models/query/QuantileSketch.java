package com.metrics.models.query;

public interface QuantileSketch {
    void merge(QuantileSketch other);
    double getQuantile(double q);
    byte[] serialize();
}
