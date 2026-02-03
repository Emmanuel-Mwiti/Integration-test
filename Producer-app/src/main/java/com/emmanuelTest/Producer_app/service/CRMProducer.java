package com.emmanuelTest.Producer_app.service;

import com.emmanuelTest.Producer_app.model.CustomerEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;

@Service
public class CRMProducer {

    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String topic = "customer_data";
    private final String baseUrl = "http://crm-mock:8080";

    public CRMProducer(KafkaTemplate<String, byte[]> kafkaTemplate, WebClient.Builder webClientBuilder) {
        this.kafkaTemplate = kafkaTemplate;
        this.webClientBuilder = webClientBuilder;
    }

    @Scheduled(fixedRateString = "${crm.fetch-rate-ms}")
    public void fetchCustomers() {
        WebClient client = webClientBuilder.baseUrl(baseUrl).build();

        client.get()
                .uri("/customers")
                .retrieve()
                .bodyToFlux(CustomerEvent.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)))
                .doOnNext(customer -> {
                    try {
                        byte[] data = objectMapper.writeValueAsBytes(customer);
                        kafkaTemplate.send(topic, customer.getId(), data)
                                .whenComplete((result, ex) -> {
                                    if (ex != null) {
                                        System.err.println("Failed to send to Kafka: " + ex.getMessage());
                                    }
                                });
                    } catch (Exception e) {
                        System.err.println("Serialization failed: " + e.getMessage());
                    }
                })
                .subscribe();
    }
}
