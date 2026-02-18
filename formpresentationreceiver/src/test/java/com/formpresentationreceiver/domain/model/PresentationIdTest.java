package com.formpresentationreceiver.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PresentationIdTest {

    @Test
    void shouldCreateWithDefaultConstructor() {
        PresentationId presentationId = new PresentationId();

        assertNull(presentationId.getId());
        assertNull(presentationId.getFormId());
        assertNull(presentationId.getReceivedAt());
        assertFalse(presentationId.isProcessed());
    }

    @Test
    void shouldCreateWithFormId() {
        UUID formId = UUID.randomUUID();

        PresentationId presentationId = new PresentationId(formId);

        assertNull(presentationId.getId());
        assertEquals(formId, presentationId.getFormId());
        assertNotNull(presentationId.getReceivedAt());
        assertFalse(presentationId.isProcessed());
    }

    @Test
    void shouldSetReceivedAtCloseToNow() {
        LocalDateTime before = LocalDateTime.now();
        PresentationId presentationId = new PresentationId(UUID.randomUUID());
        LocalDateTime after = LocalDateTime.now();

        assertFalse(presentationId.getReceivedAt().isBefore(before));
        assertFalse(presentationId.getReceivedAt().isAfter(after));
    }

    @Test
    void shouldCreateWithAllFields() {
        UUID id = UUID.randomUUID();
        UUID formId = UUID.randomUUID();
        LocalDateTime receivedAt = LocalDateTime.of(2025, 6, 15, 14, 0);
        boolean processed = true;

        PresentationId presentationId = new PresentationId(id, formId, receivedAt, processed);

        assertEquals(id, presentationId.getId());
        assertEquals(formId, presentationId.getFormId());
        assertEquals(receivedAt, presentationId.getReceivedAt());
        assertTrue(presentationId.isProcessed());
    }

    @Test
    void shouldSetAndGetId() {
        PresentationId presentationId = new PresentationId();
        UUID id = UUID.randomUUID();

        presentationId.setId(id);

        assertEquals(id, presentationId.getId());
    }

    @Test
    void shouldSetAndGetFormId() {
        PresentationId presentationId = new PresentationId();
        UUID formId = UUID.randomUUID();

        presentationId.setFormId(formId);

        assertEquals(formId, presentationId.getFormId());
    }

    @Test
    void shouldSetAndGetReceivedAt() {
        PresentationId presentationId = new PresentationId();
        LocalDateTime receivedAt = LocalDateTime.now();

        presentationId.setReceivedAt(receivedAt);

        assertEquals(receivedAt, presentationId.getReceivedAt());
    }

    @Test
    void shouldSetAndGetProcessed() {
        PresentationId presentationId = new PresentationId();

        assertFalse(presentationId.isProcessed());

        presentationId.setProcessed(true);

        assertTrue(presentationId.isProcessed());
    }

    @Test
    void shouldMarkAsProcessed() {
        PresentationId presentationId = new PresentationId(UUID.randomUUID());

        assertFalse(presentationId.isProcessed());

        presentationId.markAsProcessed();

        assertTrue(presentationId.isProcessed());
    }

    @Test
    void shouldDefaultToNotProcessedWhenCreatedWithFormId() {
        PresentationId presentationId = new PresentationId(UUID.randomUUID());

        assertFalse(presentationId.isProcessed());
    }

    @Test
    void shouldCreateUnprocessedWithAllArgsConstructor() {
        PresentationId presentationId = new PresentationId(
                UUID.randomUUID(), UUID.randomUUID(), LocalDateTime.now(), false
        );

        assertFalse(presentationId.isProcessed());

        presentationId.markAsProcessed();

        assertTrue(presentationId.isProcessed());
    }
}
