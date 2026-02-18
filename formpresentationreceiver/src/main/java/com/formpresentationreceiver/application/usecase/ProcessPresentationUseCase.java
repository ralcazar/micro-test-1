package com.formpresentationreceiver.application.usecase;

import com.formpresentationreceiver.domain.port.input.ProcessPresentationCommand;

import java.util.logging.Logger;
import java.util.UUID;

/**
 * Use case for processing a presentation from the inbox
 * This implementation only contains the business logic.
 * State management (tryMark, markAsXXX) is handled by the caller.
 */
public class ProcessPresentationUseCase implements ProcessPresentationCommand {

    private static final Logger log = Logger.getLogger(ProcessPresentationUseCase.class.getName());

    @Override
    public void execute(UUID presentationId) {
        log.info(() -> "Processing presentation with ID: " + presentationId);

        // TODO: Add your business logic here
        // For example: call external services, transform data, etc.

        log.info(() -> "Presentation " + presentationId + " processed successfully");
    }
}
