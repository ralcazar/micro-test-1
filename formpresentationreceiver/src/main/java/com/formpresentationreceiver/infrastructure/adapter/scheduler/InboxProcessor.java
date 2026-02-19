package com.formpresentationreceiver.infrastructure.adapter.scheduler;

import com.formpresentationreceiver.domain.model.PresentationId;
import com.formpresentationreceiver.domain.port.input.ProcessPresentationImmediatelyCommand;
import com.formpresentationreceiver.domain.port.output.InboxRepository;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Scheduled processor that processes unprocessed presentations from the inbox
 * Delegates to ProcessPresentationImmediatelyCommand to avoid code duplication
 */
@ApplicationScoped
public class InboxProcessor {

    private static final Logger log = LoggerFactory.getLogger(InboxProcessor.class);
    private static final int BATCH_SIZE = 10;

    private final InboxRepository inboxRepository;
    private final ProcessPresentationImmediatelyCommand processPresentationImmediatelyCommand;

    public InboxProcessor(
            InboxRepository inboxRepository,
            ProcessPresentationImmediatelyCommand processPresentationImmediatelyCommand) {
        this.inboxRepository = inboxRepository;
        this.processPresentationImmediatelyCommand = processPresentationImmediatelyCommand;
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
                // Delegate to the same command used for immediate processing
                // This avoids code duplication
                processPresentationImmediatelyCommand.execute(presentationId);
            } catch (Exception e) {
                log.error("Error processing presentation {}: {}", presentationId, e.getMessage(), e);
                // Continue processing other items even if one fails
            }
        }
    }
}
