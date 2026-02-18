package com.formplatform.infrastructure.adapter.output.persistence;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity for Form persistence
 */
@Entity
@Table(name = "forms")
public class FormEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "form_data", columnDefinition = "TEXT")
    private String formData;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public FormEntity() {
    }

    public FormEntity(String formData, LocalDateTime createdAt) {
        this.formData = formData;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFormData() {
        return formData;
    }

    public void setFormData(String formData) {
        this.formData = formData;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
