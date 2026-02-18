package com.formpresentationreceiver.application.usecase;

import com.formpresentationreceiver.domain.port.input.ProcessPresentationCommand;
import com.formpresentationreceiver.domain.port.output.InboxRepository;
import jakarta.transaction.Transactional;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.util.UUID;

/**
 * Use case for processing a presentation from the inbox
 */
public class ProcessPresentationUseCase implements ProcessPresentationCommand {

    private static final Logger log = Logger.getLogger(ProcessPresentationUseCase.class.getName());

    private final InboxRepository inboxRepository;

    public ProcessPresentationUseCase(InboxRepository inboxRepository) {
        this.inboxRepository = inboxRepository;
    }

    @Override
    @Transactional
    public void execute(UUID presentationId) {
        log.info(() -> "Processing presentation with ID: " + presentationId);

        // TODO: Add your business logic here
        // For example: call external services, transform data, etc.

        // Simulate processing
        try {
            // Your processing logic goes here
            log.info(() -> "Presentation " + presentationId + " processed successfully");

            // Mark as processed
            inboxRepository.markAsProcessed(presentationId);

        } catch (Exception e) {
            log.log(Level.SEVERE, "Error processing presentation " + presentationId + ": " + e.getMessage(), e);
            throw e;
        }
    }
}
