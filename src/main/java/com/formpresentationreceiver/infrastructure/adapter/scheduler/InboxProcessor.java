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
                processPresentationCommand.execute(presentationId.getId());
            } catch (Exception e) {
                log.error("Error processing presentation {}: {}", presentationId.getId(), e.getMessage(), e);
                // Continue processing other items even if one fails
            }
        }
    }
}
