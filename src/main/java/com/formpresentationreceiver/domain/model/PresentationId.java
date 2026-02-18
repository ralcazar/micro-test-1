package com.formpresentationreceiver.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain entity representing a Presentation ID
 */
public class PresentationId {
    private UUID id;
    private UUID formId;
    private LocalDateTime receivedAt;
    private boolean processed;

    public PresentationId() {
    }

    public PresentationId(UUID formId) {
        this.formId = formId;
        this.receivedAt = LocalDateTime.now();
        this.processed = false;
    }

    public PresentationId(UUID id, UUID formId, LocalDateTime receivedAt, boolean processed) {
        this.id = id;
        this.formId = formId;
        this.receivedAt = receivedAt;
        this.processed = processed;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getFormId() {
        return formId;
    }

    public void setFormId(UUID formId) {
        this.formId = formId;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }

    public void markAsProcessed() {
        this.processed = true;
    }
}
