package com.emmanuelTest.Producer_app.model;

import lombok.Data;

import java.time.Instant;

@Data
public class ProductEvent {
    private String id;
    private String name;
    private int stock;
    private Instant updatedAt;
}
