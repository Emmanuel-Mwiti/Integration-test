package com.emmanuelTest.Producer_app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .defaultHeader("Content-Type", "application/json")
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024));
        // 16MB buffer for large responses
    }
}
