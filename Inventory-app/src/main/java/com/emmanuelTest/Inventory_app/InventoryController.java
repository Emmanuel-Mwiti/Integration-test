package com.emmanuelTest.Inventory_app;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class InventoryController {
    @GetMapping("/products")
    public List<Map<String, Object>> products() {
        return List.of(
                Map.of("id", "1", "name", "Laptop", "stock", 15),
                Map.of("id", "2", "name", "Mouse", "stock", 50)
        );
    }
}
