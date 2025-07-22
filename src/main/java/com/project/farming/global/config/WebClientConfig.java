package com.project.farming.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${python.server.url}")
    private String pythonServerUrl;

    @Bean("pythonWebClient")
    public WebClient pythonWebClient() {
        return WebClient.builder()
                .baseUrl(pythonServerUrl)
                .build();
    }
}

// 환경변수 python.server.url=http://localhost:8000
