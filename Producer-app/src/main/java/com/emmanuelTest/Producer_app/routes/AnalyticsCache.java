package com.emmanuelTest.Producer_app.routes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.concurrent.ConcurrentHashMap;

public class AnalyticsCache {
    public static ConcurrentHashMap<String, MergedRecord> cache = new ConcurrentHashMap<>();
    private static final ObjectMapper mapper = new ObjectMapper();

    public static class MergedRecord {
        private JsonNode customer;
        private JsonNode inventory;

        public synchronized void update(String type, JsonNode data) {
            if ("customer".equals(type)) customer = data;
            else if ("inventory".equals(type)) inventory = data;
        }

        public synchronized boolean isComplete() {
            return customer != null && inventory != null;
        }

        public synchronized String toJson() {
            var node = mapper.createObjectNode();
            if (customer != null) node.set("customer", customer);
            if (inventory != null) node.set("inventory", inventory);
            return node.toString();
        }
    }
}
