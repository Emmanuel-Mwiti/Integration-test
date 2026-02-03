package com.emmanuelTest.Producer_app.model;

import lombok.Data;

import java.time.Instant;

@Data
public class CustomerEvent {
    private String id;
    private String name;
    private String email;
    private Instant updatedAt;
}
