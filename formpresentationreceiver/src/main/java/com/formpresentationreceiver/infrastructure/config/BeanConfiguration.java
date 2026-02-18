package com.formpresentationreceiver.infrastructure.config;

import com.formpresentationreceiver.application.usecase.ProcessPresentationUseCase;
import com.formpresentationreceiver.application.usecase.ReceiveFormCreatedUseCase;
import com.formpresentationreceiver.domain.port.input.ProcessPresentationCommand;
import com.formpresentationreceiver.domain.port.input.ReceiveFormCreatedCommand;
import com.formpresentationreceiver.domain.port.output.InboxRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

/**
 * CDI Bean configuration for dependency injection
 */
@ApplicationScoped
public class BeanConfiguration {

    @Produces
    @ApplicationScoped
    public ReceiveFormCreatedCommand receiveFormCreatedCommand(InboxRepository inboxRepository) {
        return new ReceiveFormCreatedUseCase(inboxRepository);
    }

    @Produces
    @ApplicationScoped
    public ProcessPresentationCommand processPresentationCommand() {
        return new ProcessPresentationUseCase();
    }
}
