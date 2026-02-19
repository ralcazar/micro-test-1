package com.formpresentationreceiver.infrastructure.config;

import com.formpresentationreceiver.application.usecase.ProcessPresentationImmediatelyUseCase;
import com.formpresentationreceiver.application.usecase.ProcessPresentationUseCase;
import com.formpresentationreceiver.application.usecase.ReceiveFormCreatedUseCase;
import com.formpresentationreceiver.domain.port.input.ProcessPresentationCommand;
import com.formpresentationreceiver.domain.port.input.ProcessPresentationImmediatelyCommand;
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
    public ProcessPresentationCommand processPresentationCommand() {
        return new ProcessPresentationUseCase();
    }

    @Produces
    @ApplicationScoped
    public ProcessPresentationImmediatelyCommand processPresentationImmediatelyCommand(
            InboxRepository inboxRepository,
            ProcessPresentationCommand processPresentationCommand) {
        return new ProcessPresentationImmediatelyUseCase(inboxRepository, processPresentationCommand);
    }

    @Produces
    @ApplicationScoped
    public ReceiveFormCreatedCommand receiveFormCreatedCommand(
            InboxRepository inboxRepository,
            ProcessPresentationImmediatelyCommand processPresentationImmediatelyCommand) {
        return new ReceiveFormCreatedUseCase(inboxRepository, processPresentationImmediatelyCommand);
    }
}
