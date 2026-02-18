package com.formpresentationreceiver.infrastructure.adapter.output.persistence;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity for inbox pattern - stores presentation IDs to be processed
 */
@Entity
@Table(name = "inbox_presentations", indexes = {
    @Index(name = "idx_inbox_status_received", columnList = "status, received_at"),
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

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    public InboxEntity() {
    }

    public InboxEntity(UUID formId, LocalDateTime receivedAt) {
        this.formId = formId;
        this.receivedAt = receivedAt;
        this.status = "PENDING";
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isProcessed() {
        return "DONE".equals(status);
    }

    public void setProcessed(boolean processed) {
        this.status = processed ? "DONE" : "PENDING";
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
}
