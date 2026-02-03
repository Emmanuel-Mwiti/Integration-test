package com.emmanuelTest.Analytics_app;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AnalyticsController {
    @PostMapping("/analytics/data")
    public ResponseEntity<String> receive(@RequestBody Map<String, Object> payload) {
        System.out.println("Analytics received: " + payload);
        return ResponseEntity.ok("OK");
    }
}
