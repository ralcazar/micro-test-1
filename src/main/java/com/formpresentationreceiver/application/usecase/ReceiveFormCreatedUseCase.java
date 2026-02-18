package com.formpresentationreceiver.application.usecase;

import com.formpresentationreceiver.domain.model.PresentationId;
import com.formpresentationreceiver.domain.port.input.ReceiveFormCreatedCommand;
import com.formpresentationreceiver.domain.port.output.InboxRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Use case for receiving form created events and storing them in the inbox
 */
public class ReceiveFormCreatedUseCase implements ReceiveFormCreatedCommand {

    private static final Logger log = LoggerFactory.getLogger(ReceiveFormCreatedUseCase.class);

    private final InboxRepository inboxRepository;

    public ReceiveFormCreatedUseCase(InboxRepository inboxRepository) {
        this.inboxRepository = inboxRepository;
    }

    @Override
    @Transactional
    public void execute(UUID formId) {
        log.info("Receiving form created event for formId: {}", formId);
        
        // Check if already exists to avoid duplicates (idempotency)
        if (inboxRepository.existsByFormId(formId)) {
            log.info("FormId {} already exists in inbox, skipping", formId);
            return;
        }
        
        // Create and save presentation ID to inbox
        PresentationId presentationId = new PresentationId(formId);
        inboxRepository.save(presentationId);
        
        log.info("FormId {} saved to inbox successfully", formId);
    }
}
