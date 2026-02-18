package com.formplatform.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FormTest {

    @Test
    void shouldCreateFormWithDefaultConstructor() {
        Form form = new Form();

        assertNull(form.getId());
        assertNull(form.getData());
        assertNull(form.getCreatedAt());
    }

    @Test
    void shouldCreateFormWithData() {
        Map<String, Object> data = Map.of("name", "John", "age", 30);

        Form form = new Form(data);

        assertNull(form.getId());
        assertEquals(data, form.getData());
        assertNotNull(form.getCreatedAt());
    }

    @Test
    void shouldCreateFormWithCreatedAtCloseToNow() {
        LocalDateTime before = LocalDateTime.now();
        Form form = new Form(Map.of("key", "value"));
        LocalDateTime after = LocalDateTime.now();

        assertFalse(form.getCreatedAt().isBefore(before));
        assertFalse(form.getCreatedAt().isAfter(after));
    }

    @Test
    void shouldCreateFormWithAllFields() {
        UUID id = UUID.randomUUID();
        Map<String, Object> data = Map.of("field", "value");
        LocalDateTime createdAt = LocalDateTime.of(2025, 1, 15, 10, 30);

        Form form = new Form(id, data, createdAt);

        assertEquals(id, form.getId());
        assertEquals(data, form.getData());
        assertEquals(createdAt, form.getCreatedAt());
    }

    @Test
    void shouldSetAndGetId() {
        Form form = new Form();
        UUID id = UUID.randomUUID();

        form.setId(id);

        assertEquals(id, form.getId());
    }

    @Test
    void shouldSetAndGetData() {
        Form form = new Form();
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Test");

        form.setData(data);

        assertEquals(data, form.getData());
        assertEquals("Test", form.getData().get("name"));
    }

    @Test
    void shouldSetAndGetCreatedAt() {
        Form form = new Form();
        LocalDateTime now = LocalDateTime.now();

        form.setCreatedAt(now);

        assertEquals(now, form.getCreatedAt());
    }

    @Test
    void shouldHandleEmptyDataMap() {
        Map<String, Object> emptyData = new HashMap<>();

        Form form = new Form(emptyData);

        assertNotNull(form.getData());
        assertTrue(form.getData().isEmpty());
    }

    @Test
    void shouldHandleComplexDataMap() {
        Map<String, Object> data = new HashMap<>();
        data.put("string", "value");
        data.put("number", 42);
        data.put("boolean", true);
        data.put("null", null);

        Form form = new Form(data);

        assertEquals(4, form.getData().size());
        assertEquals("value", form.getData().get("string"));
        assertEquals(42, form.getData().get("number"));
        assertEquals(true, form.getData().get("boolean"));
        assertNull(form.getData().get("null"));
    }
}
