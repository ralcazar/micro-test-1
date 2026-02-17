package com.formplatform.domain.port.output;

import com.formplatform.domain.model.Form;

import java.util.UUID;

/**
 * Output port for Form persistence operations
 */
public interface FormRepository {
    Form save(Form form);
    Form findById(UUID id);
}
