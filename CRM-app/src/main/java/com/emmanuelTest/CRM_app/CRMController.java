package com.emmanuelTest.CRM_app;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class CRMController {
    @GetMapping("/customers")
    public List<Map<String, Object>> customers() {
        return List.of(
                Map.of("id", "1", "name", "Emmanuel Mwiti", "email", "emmanuel@example.com"),
                Map.of("id", "2", "name", "Victor Mugo", "email", "victor@example.com")
        );
    }
}
