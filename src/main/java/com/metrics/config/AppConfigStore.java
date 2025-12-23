package com.metrics.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Getter
@Setter
@Service
public class AppConfigStore {

    @Value("${store.flushBatchSize}")
    public int flushBatchSize;

    @Value("${store.flushInterval}")
    public Duration flushInterval;

    @Value("${check.labelSizeLimit}")
    public int labelSizeLimit;

    @Value("${check.metricSeriesLimit}")
    public int metricSeriesLimit;

}
