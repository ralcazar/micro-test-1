package com.formplatform.application.usecase;

import com.formplatform.application.exception.InvalidFormException;
import com.formplatform.domain.model.Form;
import com.formplatform.domain.port.input.SubmitFormCommand;
import com.formplatform.domain.port.output.EventPublisher;
import com.formplatform.domain.port.output.FormRepository;
import jakarta.transaction.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Use case for submitting a new form
 */
public class SubmitFormUseCase implements SubmitFormCommand {

    private final FormRepository formRepository;
    private final EventPublisher eventPublisher;

    public SubmitFormUseCase(FormRepository formRepository, EventPublisher eventPublisher) {
        this.formRepository = formRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public UUID execute(Map<String, Object> formData) {
        if (formData == null || formData.isEmpty()) {
            throw new InvalidFormException("Form data cannot be empty");
        }

        // Create domain entity
        Form form = new Form(formData);
        
        // Save to repository
        Form savedForm = formRepository.save(form);
        
        // Publish event
        eventPublisher.publishFormCreated(savedForm.getId());
        
        return savedForm.getId();
    }
}
