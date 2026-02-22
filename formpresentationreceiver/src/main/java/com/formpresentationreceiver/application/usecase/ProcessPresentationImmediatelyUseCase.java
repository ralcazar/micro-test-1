package com.formpresentationreceiver.application.usecase;

import com.formpresentationreceiver.domain.model.PresentationId;
import com.formpresentationreceiver.domain.port.input.ProcessPresentationCommand;
import com.formpresentationreceiver.domain.port.input.ProcessPresentationImmediatelyCommand;
import com.formpresentationreceiver.domain.port.output.InboxRepository;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.logging.Logger;

/**
 * Use case for processing a presentation immediately with state management.
 * Encapsulates the logic of tryMark, process, and markAsXXX to avoid duplication.
 * After MAX_RETRIES failures the item is permanently marked as FAILED.
 */
public class ProcessPresentationImmediatelyUseCase implements ProcessPresentationImmediatelyCommand {

    private static final Logger log = Logger.getLogger(ProcessPresentationImmediatelyUseCase.class.getName());

    static final int MAX_RETRIES = 5;

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
            // markAsUnprocessed also increments retry_count atomically
            inboxRepository.markAsUnprocessed(presentationId);

            // Fetch the updated retry count by re-querying is not straightforward here,
            // but markAsUnprocessed returns the count after increment. We use a secondary
            // check: if the entity retryCount (before increment) was already at MAX_RETRIES - 1,
            // the next value is MAX_RETRIES and we should fail it permanently.
            // We achieve this by checking after the increment via a dedicated method.
            int newRetryCount = inboxRepository.getRetryCount(presentationId);
            if (newRetryCount >= MAX_RETRIES) {
                inboxRepository.markAsFailed(presentationId);
                log.severe(() -> "Presentation " + presentationId + " permanently marked as FAILED after "
                        + newRetryCount + " retries. Error: " + e.getMessage());
            } else {
                log.warning(() -> "Error processing presentation " + presentationId
                        + " (retry " + newRetryCount + "/" + MAX_RETRIES + "): "
                        + e.getMessage() + ". Will be retried by scheduler.");
            }

            throw e;
        }
    }
}
