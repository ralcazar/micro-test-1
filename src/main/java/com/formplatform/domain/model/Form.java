package com.formplatform.domain.model;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Domain entity representing a Form
 */
public class Form {
    private UUID id;
    private Map<String, Object> data;
    private LocalDateTime createdAt;

    public Form() {
    }

    public Form(Map<String, Object> data) {
        this.data = data;
        this.createdAt = LocalDateTime.now();
    }

    public Form(UUID id, Map<String, Object> data, LocalDateTime createdAt) {
        this.id = id;
        this.data = data;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
