package com.metrics.utils;

import com.metrics.models.query.QuantileSketch;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class SketchSerde {

    public static QuantileSketch deserialize(byte[] raw) {
        if (raw == null || raw.length == 0) {
            return TDigestSketch.create();
        }
        return TDigestSketch.deserialize(raw);
    }
}
