package com.formplatform.infrastructure.config;

import com.formplatform.application.usecase.SubmitFormUseCase;
import com.formplatform.domain.port.input.SubmitFormCommand;
import com.formplatform.domain.port.output.EventPublisher;
import com.formplatform.domain.port.output.FormRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

/**
 * Configuration class for dependency injection
 */
@ApplicationScoped
public class BeanConfiguration {

    @Produces
    @ApplicationScoped
    public SubmitFormCommand submitFormCommand(FormRepository formRepository, @Named("outbox") EventPublisher eventPublisher) {
        return new SubmitFormUseCase(formRepository, eventPublisher);
    }
}
