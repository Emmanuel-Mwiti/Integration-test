package com.emmanuelTest.Producer_app.service;

import com.emmanuelTest.Producer_app.model.ProductEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;

@Service
public class InventoryProducer {

    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String topic = "inventory_data";
    private final String baseUrl = "http://inventory-mock:8080";

    public InventoryProducer(KafkaTemplate<String, byte[]> kafkaTemplate, WebClient.Builder webClientBuilder) {
        this.kafkaTemplate = kafkaTemplate;
        this.webClientBuilder = webClientBuilder;
    }

    @Scheduled(fixedRateString = "${inventory.fetch-rate-ms}")
    public void fetchInventory() {
        WebClient client = webClientBuilder.baseUrl(baseUrl).build();

        client.get()
                .uri("/products")
                .retrieve()
                .bodyToFlux(ProductEvent.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)))
                .doOnNext(product -> {
                    try {
                        byte[] data = objectMapper.writeValueAsBytes(product);
                        kafkaTemplate.send(topic, product.getId(), data)
                                .whenComplete((result, ex) -> {
                                    if (ex != null) {
                                        System.err.println("Failed to send to Kafka: " + ex.getMessage());
                                    } else {
                                        System.out.println("Sent product " + product.getId() + " to Kafka");
                                    }
                                });
                    } catch (Exception e) {
                        System.err.println("Serialization failed: " + e.getMessage());
                    }
                })
                .subscribe();
    }
}
