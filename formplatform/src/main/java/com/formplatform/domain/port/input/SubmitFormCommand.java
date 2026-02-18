package com.formplatform.domain.port.input;

import java.util.Map;
import java.util.UUID;

/**
 * Input port for submitting forms
 */
public interface SubmitFormCommand {
    UUID execute(Map<String, Object> formData);
}
