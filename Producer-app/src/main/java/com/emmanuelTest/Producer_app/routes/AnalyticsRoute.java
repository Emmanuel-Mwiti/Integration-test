package com.emmanuelTest.Producer_app.routes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class AnalyticsRoute extends RouteBuilder {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${analytics.url:http://analytics-mock:8080/analytics/data}")
    private String analyticsUrl;

    public AnalyticsRoute(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void configure() throws Exception {

        // Consume from Kafka customer_data
        from("kafka:customer_data?brokers=kafka:9092&groupId=analytics-group")
                .convertBodyTo(String.class)
                .process(exchange -> mergeAndSend(exchange.getIn().getBody(String.class), "customer"));

        // Consume from Kafka inventory_data
        from("kafka:inventory_data?brokers=kafka:9092&groupId=analytics-group")
                .convertBodyTo(String.class)
                .process(exchange -> mergeAndSend(exchange.getIn().getBody(String.class), "inventory"));
    }

    private void mergeAndSend(String payloadJson, String type) throws Exception {
        JsonNode payload = mapper.readTree(payloadJson);
        String id = payload.get("id").asText();

        // Deduplication key
        String key = sha256(payloadJson);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) return;

        // Store in Redis for dedup
        redisTemplate.opsForValue().set(key, "1");

        // Temporary in-memory cache for merging
        AnalyticsCache.cache.computeIfAbsent(id, k -> new AnalyticsCache.MergedRecord())
                .update(type, payload);

        var merged = AnalyticsCache.cache.get(id).toJson();

        // Send to Analytics only if both customer + inventory present
        if (AnalyticsCache.cache.get(id).isComplete()) {
            // POST to Analytics
            getContext().createProducerTemplate()
                    .sendBodyAndHeader(analyticsUrl, merged, "Content-Type", "application/json");

            // Remove from cache after sending
            AnalyticsCache.cache.remove(id);
        }
    }

    private String sha256(String input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }
}
