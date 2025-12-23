package com.metrics.config;

import com.metrics.models.common.ServiceConfig;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "fetch")
public class FetchProperties {

    private List<ServiceConfig> services;
}
