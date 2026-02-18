package com.formpresentationreceiver.infrastructure.adapter.output.persistence;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity for inbox pattern - stores presentation IDs to be processed
 */
@Entity
@Table(name = "inbox_presentations", indexes = {
    @Index(name = "idx_inbox_processed_received", columnList = "processed, received_at"),
    @Index(name = "idx_inbox_form_id", columnList = "form_id", unique = true)
})
public class InboxEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "form_id", nullable = false, unique = true)
    private UUID formId;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "processed", nullable = false)
    private boolean processed = false;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    public InboxEntity() {
    }

    public InboxEntity(UUID formId, LocalDateTime receivedAt) {
        this.formId = formId;
        this.receivedAt = receivedAt;
        this.processed = false;
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

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
}
