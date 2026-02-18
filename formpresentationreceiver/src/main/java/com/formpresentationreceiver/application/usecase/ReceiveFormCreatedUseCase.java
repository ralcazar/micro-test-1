package com.formpresentationreceiver.application.usecase;

import com.formpresentationreceiver.domain.model.PresentationId;
import com.formpresentationreceiver.domain.port.input.ReceiveFormCreatedCommand;
import com.formpresentationreceiver.domain.port.output.InboxRepository;
import jakarta.transaction.Transactional;

import java.util.logging.Logger;

import java.util.UUID;

/**
 * Use case for receiving form created events and storing them in the inbox
 */
public class ReceiveFormCreatedUseCase implements ReceiveFormCreatedCommand {

    private static final Logger log = Logger.getLogger(ReceiveFormCreatedUseCase.class.getName());

    private final InboxRepository inboxRepository;

    public ReceiveFormCreatedUseCase(InboxRepository inboxRepository) {
        this.inboxRepository = inboxRepository;
    }

    @Override
    @Transactional
    public void execute(UUID formId) {
        log.info(() -> "Receiving form created event for formId: " + formId);

        // Check if already exists to avoid duplicates (idempotency)
        if (inboxRepository.existsByFormId(formId)) {
            log.info(() -> "FormId " + formId + " already exists in inbox, skipping");
            return;
        }

        // Create and save presentation ID to inbox
        PresentationId presentationId = new PresentationId(formId);
        inboxRepository.save(presentationId);

        log.info(() -> "FormId " + formId + " saved to inbox successfully");
    }
}
