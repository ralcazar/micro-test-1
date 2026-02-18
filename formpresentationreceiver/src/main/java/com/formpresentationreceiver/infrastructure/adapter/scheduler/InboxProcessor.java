package com.formpresentationreceiver.infrastructure.adapter.scheduler;

import com.formpresentationreceiver.domain.model.PresentationId;
import com.formpresentationreceiver.domain.port.input.ProcessPresentationCommand;
import com.formpresentationreceiver.domain.port.output.InboxRepository;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Scheduled processor that processes unprocessed presentations from the inbox
 */
@ApplicationScoped
public class InboxProcessor {

    private static final Logger log = LoggerFactory.getLogger(InboxProcessor.class);
    private static final int BATCH_SIZE = 10;

    private final InboxRepository inboxRepository;
    private final ProcessPresentationCommand processPresentationCommand;

    public InboxProcessor(InboxRepository inboxRepository, ProcessPresentationCommand processPresentationCommand) {
        this.inboxRepository = inboxRepository;
        this.processPresentationCommand = processPresentationCommand;
    }

    /**
     * Process unprocessed presentations every 10 seconds
     */
    @Scheduled(every = "10s")
    void processInbox() {
        List<PresentationId> unprocessed = inboxRepository.findUnprocessed(BATCH_SIZE);

        if (unprocessed.isEmpty()) {
            return;
        }

        log.info("Processing {} unprocessed presentations from inbox", unprocessed.size());

        for (PresentationId presentationId : unprocessed) {
            try {
                processPresentation(presentationId);
            } catch (Exception e) {
                log.error("Error processing presentation {}: {}", presentationId.getId(), e.getMessage(), e);
                // Continue processing other items even if one fails
            }
        }
    }

    /**
     * Process a single presentation with state management
     */
    private void processPresentation(PresentationId presentationId) {
        log.info("Attempting to process presentation with ID: {}", presentationId.getId());

        // Try to atomically mark as DOING (prevents duplicate processing by other instances)
        int updated = inboxRepository.tryMarkAsProcessing(presentationId.getId());
        
        if (updated == 0) {
            // Another instance already processing or processed this presentation
            log.info("Presentation {} already being processed or processed by another instance, skipping", presentationId.getId());
            return;
        }

        log.info("Processing presentation with ID: {} (status: DOING)", presentationId.getId());

        try {
            // Execute the business logic
            processPresentationCommand.execute(presentationId.getId());

            // Mark as DONE only after successful processing
            inboxRepository.markAsProcessed(presentationId.getId());
            log.info("Presentation {} marked as DONE", presentationId.getId());

        } catch (Exception e) {
            log.error("Error processing presentation {}: {}", presentationId.getId(), e.getMessage(), e);
            
            // Revert to PENDING state so it can be retried later
            inboxRepository.markAsUnprocessed(presentationId.getId());
            log.info("Presentation {} reverted to PENDING for retry", presentationId.getId());
            
            throw e;
        }
    }
}
