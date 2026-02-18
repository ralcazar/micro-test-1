package com.formplatform.application.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InvalidFormExceptionTest {

    @Test
    void shouldCreateExceptionWithMessage() {
        String message = "Form data cannot be empty";

        InvalidFormException exception = new InvalidFormException(message);

        assertEquals(message, exception.getMessage());
    }

    @Test
    void shouldBeRuntimeException() {
        InvalidFormException exception = new InvalidFormException("test");

        assertInstanceOf(RuntimeException.class, exception);
    }

    @Test
    void shouldPreserveDetailedMessage() {
        String detailedMessage = "Field 'name' is required and cannot be null";

        InvalidFormException exception = new InvalidFormException(detailedMessage);

        assertEquals(detailedMessage, exception.getMessage());
    }

    @Test
    void shouldHandleNullMessage() {
        InvalidFormException exception = new InvalidFormException(null);

        assertNull(exception.getMessage());
    }

    @Test
    void shouldHandleEmptyMessage() {
        InvalidFormException exception = new InvalidFormException("");

        assertEquals("", exception.getMessage());
    }
}
