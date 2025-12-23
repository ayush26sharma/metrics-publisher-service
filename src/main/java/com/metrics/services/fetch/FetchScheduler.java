package com.metrics.services.fetch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Slf4j
@RequiredArgsConstructor
public class FetchScheduler {

    private final FetchService fetchService;

//    @Scheduled(fixedRate = 10000)
    public void run() {
        log.info("Fetching new metrics at: {}", Instant.now());
        fetchService.pollAllServices();
    }
}
