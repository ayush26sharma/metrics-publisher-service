package com.metrics.utils;

import com.metrics.models.query.QuantileSketch;
import com.tdunning.math.stats.MergingDigest;
import com.tdunning.math.stats.TDigest;
import java.nio.ByteBuffer;

public class TDigestSketch implements QuantileSketch {

    public final TDigest digest;

    private TDigestSketch(TDigest digest) {
        this.digest = digest;
    }

    public static TDigestSketch create() {
        return new TDigestSketch(new MergingDigest(100));
    }

    @Override
    public void merge(QuantileSketch other) {
        TDigestSketch o = (TDigestSketch) other;
        this.digest.add(o.digest);
    }

    @Override
    public double getQuantile(double q) {
        return digest.quantile(q);
    }

    @Override
    public byte[] serialize() {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        digest.asSmallBytes(buffer);

        buffer.flip();

        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }

    public static TDigestSketch deserialize(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return TDigestSketch.create();
        }

        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        TDigest digest = MergingDigest.fromBytes(buffer);
        return new TDigestSketch(digest);
    }


}
