package com.formpresentationreceiver.application.usecase;

import com.formpresentationreceiver.domain.port.input.ProcessPresentationCommand;
import com.formpresentationreceiver.domain.port.output.InboxRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Use case for processing a presentation from the inbox
 */
public class ProcessPresentationUseCase implements ProcessPresentationCommand {

    private static final Logger log = LoggerFactory.getLogger(ProcessPresentationUseCase.class);

    private final InboxRepository inboxRepository;

    public ProcessPresentationUseCase(InboxRepository inboxRepository) {
        this.inboxRepository = inboxRepository;
    }

    @Override
    @Transactional
    public void execute(UUID presentationId) {
        log.info("Processing presentation with ID: {}", presentationId);
        
        // TODO: Add your business logic here
        // For example: call external services, transform data, etc.
        
        // Simulate processing
        try {
            // Your processing logic goes here
            log.info("Presentation {} processed successfully", presentationId);
            
            // Mark as processed
            inboxRepository.markAsProcessed(presentationId);
            
        } catch (Exception e) {
            log.error("Error processing presentation {}: {}", presentationId, e.getMessage(), e);
            throw e;
        }
    }
}
