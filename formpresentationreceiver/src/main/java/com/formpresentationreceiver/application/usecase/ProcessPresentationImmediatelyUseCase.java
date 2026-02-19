package com.formpresentationreceiver.application.usecase;

import com.formpresentationreceiver.domain.model.PresentationId;
import com.formpresentationreceiver.domain.port.input.ProcessPresentationCommand;
import com.formpresentationreceiver.domain.port.input.ProcessPresentationImmediatelyCommand;
import com.formpresentationreceiver.domain.port.output.InboxRepository;
import jakarta.transaction.Transactional;

import java.util.logging.Logger;

/**
 * Use case for processing a presentation immediately with state management
 * Encapsulates the logic of tryMark, process, and markAsXXX to avoid duplication
 */
public class ProcessPresentationImmediatelyUseCase implements ProcessPresentationImmediatelyCommand {

    private static final Logger log = Logger.getLogger(ProcessPresentationImmediatelyUseCase.class.getName());

    private final InboxRepository inboxRepository;
    private final ProcessPresentationCommand processPresentationCommand;

    public ProcessPresentationImmediatelyUseCase(
            InboxRepository inboxRepository,
            ProcessPresentationCommand processPresentationCommand) {
        this.inboxRepository = inboxRepository;
        this.processPresentationCommand = processPresentationCommand;
    }

    @Override
    @Transactional
    public void execute(PresentationId presentationId) {
        log.info(() -> "Attempting to process presentation with ID: " + presentationId);

        // Try to atomically mark as DOING (prevents duplicate processing by other instances)
        int updated = inboxRepository.tryMarkAsProcessing(presentationId);

        if (updated == 0) {
            // Another instance already processing or processed this presentation
            log.info(() -> "Presentation " + presentationId + " already being processed or processed by another instance, skipping");
            return;
        }

        log.info(() -> "Processing presentation with ID: " + presentationId + " (status: DOING)");

        try {
            // Execute the business logic
            processPresentationCommand.execute(presentationId);

            // Mark as DONE only after successful processing
            inboxRepository.markAsProcessed(presentationId);
            log.info(() -> "Presentation " + presentationId + " marked as DONE");

        } catch (Exception e) {
            log.warning(() -> "Error processing presentation " + presentationId + ": " + e.getMessage() + ". Will be retried by scheduler.");

            // Revert to PENDING state so it can be retried later
            inboxRepository.markAsUnprocessed(presentationId);
            log.info(() -> "Presentation " + presentationId + " reverted to PENDING for retry");

            throw e;
        }
    }
}
