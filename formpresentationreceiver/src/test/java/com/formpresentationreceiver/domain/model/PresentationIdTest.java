package com.formpresentationreceiver.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PresentationIdTest {

    @Test
    void shouldCreatePresentationIdWithUUID() {
        UUID uuid = UUID.randomUUID();
        
        PresentationId presentationId = new PresentationId(uuid);
        
        assertEquals(uuid, presentationId.value());
    }

    @Test
    void shouldCreatePresentationIdWithOfMethod() {
        UUID uuid = UUID.randomUUID();
        
        PresentationId presentationId = PresentationId.of(uuid);
        
        assertEquals(uuid, presentationId.value());
    }

    @Test
    void shouldCreatePresentationIdFromString() {
        String uuidString = "123e4567-e89b-12d3-a456-426614174000";
        
        PresentationId presentationId = PresentationId.of(uuidString);
        
        assertEquals(UUID.fromString(uuidString), presentationId.value());
    }

    @Test
    void shouldThrowExceptionWhenUUIDIsNull() {
        assertThrows(IllegalArgumentException.class, () -> new PresentationId(null));
    }

    @Test
    void shouldThrowExceptionWhenOfMethodReceivesNull() {
        assertThrows(IllegalArgumentException.class, () -> PresentationId.of((UUID) null));
    }

    @Test
    void shouldBeEqualWhenSameUUID() {
        UUID uuid = UUID.randomUUID();
        PresentationId presentationId1 = PresentationId.of(uuid);
        PresentationId presentationId2 = PresentationId.of(uuid);
        
        assertEquals(presentationId1, presentationId2);
        assertEquals(presentationId1.hashCode(), presentationId2.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenDifferentUUID() {
        PresentationId presentationId1 = PresentationId.of(UUID.randomUUID());
        PresentationId presentationId2 = PresentationId.of(UUID.randomUUID());
        
        assertNotEquals(presentationId1, presentationId2);
    }

    @Test
    void shouldBeEqualToItself() {
        PresentationId presentationId = PresentationId.of(UUID.randomUUID());
        
        assertEquals(presentationId, presentationId);
    }

    @Test
    void shouldNotBeEqualToNull() {
        PresentationId presentationId = PresentationId.of(UUID.randomUUID());
        
        assertNotEquals(presentationId, null);
    }

    @Test
    void shouldNotBeEqualToDifferentType() {
        PresentationId presentationId = PresentationId.of(UUID.randomUUID());
        
        assertNotEquals(presentationId, "some string");
    }

    @Test
    void shouldReturnUUIDStringRepresentation() {
        UUID uuid = UUID.randomUUID();
        PresentationId presentationId = PresentationId.of(uuid);
        
        assertEquals(uuid.toString(), presentationId.toString());
    }
}
