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
        log.info(() -> "Attempting to process presentation with ID: " + presentationId);

        // Try to atomically mark as DOING (prevents duplicate processing by other instances)
        int updated = inboxRepository.tryMarkAsProcessing(presentationId);
        
        if (updated == 0) {
            // Another instance already processing or processed this presentation
            log.info(() -> "Presentation " + presentationId + " already being processed or processed by another instance, skipping");
            return;
        }

        log.info(() -> "Processing presentation with ID: " + presentationId + " (status: DOING)");

        // TODO: Add your business logic here
        // For example: call external services, transform data, etc.

        try {
            // Your processing logic goes here
            log.info(() -> "Presentation " + presentationId + " processed successfully");

            // Mark as DONE only after successful processing
            inboxRepository.markAsProcessed(presentationId);
            log.info(() -> "Presentation " + presentationId + " marked as DONE");

        } catch (Exception e) {
            log.log(Level.SEVERE, "Error processing presentation " + presentationId + ": " + e.getMessage(), e);
            
            // Revert to PENDING state so it can be retried later
            inboxRepository.markAsUnprocessed(presentationId);
            log.info(() -> "Presentation " + presentationId + " reverted to PENDING for retry");
            
            throw e;
        }
    }
}
