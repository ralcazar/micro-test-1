package com.formpresentationreceiver.application.usecase;

import com.formpresentationreceiver.domain.model.PresentationId;
import com.formpresentationreceiver.domain.port.input.ProcessPresentationImmediatelyCommand;
import com.formpresentationreceiver.domain.port.input.ReceiveFormCreatedCommand;
import com.formpresentationreceiver.domain.port.output.InboxRepository;
import jakarta.transaction.Transactional;

import java.util.logging.Logger;

/**
 * Use case for receiving form created events and storing them in the inbox
 */
public class ReceiveFormCreatedUseCase implements ReceiveFormCreatedCommand {

    private static final Logger log = Logger.getLogger(ReceiveFormCreatedUseCase.class.getName());

    private final InboxRepository inboxRepository;
    private final ProcessPresentationImmediatelyCommand processPresentationImmediatelyCommand;

    public ReceiveFormCreatedUseCase(
            InboxRepository inboxRepository,
            ProcessPresentationImmediatelyCommand processPresentationImmediatelyCommand) {
        this.inboxRepository = inboxRepository;
        this.processPresentationImmediatelyCommand = processPresentationImmediatelyCommand;
    }

    @Override
    @Transactional
    public void execute(PresentationId presentationId) {
        log.info(() -> "Receiving form created event for presentationId: " + presentationId);

        // Check if already exists to avoid duplicates (idempotency)
        if (inboxRepository.existsByPresentationId(presentationId)) {
            log.info(() -> "PresentationId " + presentationId + " already exists in inbox, skipping");
            return;
        }

        // Save presentation ID to inbox
        inboxRepository.save(presentationId);

        log.info(() -> "PresentationId " + presentationId + " saved to inbox successfully");

        // Trigger immediate processing after saving to inbox
        // The processing logic is delegated to avoid duplication
        try {
            log.info(() -> "Triggering immediate processing for presentation ID: " + presentationId);
            processPresentationImmediatelyCommand.execute(presentationId);
        } catch (Exception e) {
            // Exception already logged and handled by ProcessPresentationImmediatelyUseCase
            // Just log that we're continuing
            log.info(() -> "Immediate processing failed for " + presentationId + ", will be retried by scheduler");
        }
    }
}
