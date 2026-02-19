package com.formpresentationreceiver.domain.model;

import java.util.UUID;

/**
 * Value Object representing a Presentation ID (which is the same as Form ID)
 * Immutable record to ensure value object semantics
 */
public record PresentationId(UUID value) {

    public PresentationId {
        if (value == null) {
            throw new IllegalArgumentException("PresentationId cannot be null");
        }
    }

    public static PresentationId of(UUID value) {
        return new PresentationId(value);
    }

    public static PresentationId of(String value) {
        return new PresentationId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
