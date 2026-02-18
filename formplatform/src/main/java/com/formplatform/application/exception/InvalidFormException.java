package com.formplatform.application.exception;

/**
 * Thrown when form data fails business validation (e.g. empty or invalid).
 */
public class InvalidFormException extends RuntimeException {

    public InvalidFormException(String message) {
        super(message);
    }
}
