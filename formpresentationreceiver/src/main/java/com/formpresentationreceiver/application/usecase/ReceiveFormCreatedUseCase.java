package com.formpresentationreceiver.application.usecase;

import com.formpresentationreceiver.domain.model.PresentationId;
import com.formpresentationreceiver.domain.port.input.ReceiveFormCreatedCommand;
import com.formpresentationreceiver.domain.port.output.InboxRepository;
import jakarta.transaction.Transactional;

import java.util.logging.Logger;

/**
 * Use case for receiving form created events and storing them in the inbox
 * This use case ONLY handles inbox insertion in its own transaction
 * Processing is handled separately by ProcessPresentationImmediatelyCommand
 */
public class ReceiveFormCreatedUseCase implements ReceiveFormCreatedCommand {

    private static final Logger log = Logger.getLogger(ReceiveFormCreatedUseCase.class.getName());

    private final InboxRepository inboxRepository;

    public ReceiveFormCreatedUseCase(InboxRepository inboxRepository) {
        this.inboxRepository = inboxRepository;
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
    }
}
