package com.formpresentationreceiver.infrastructure.adapter.scheduler;

import com.formpresentationreceiver.domain.model.PresentationId;
import com.formpresentationreceiver.domain.port.output.InboxRepository;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled job that runs daily at 7:00 AM to fetch unprocessed presentations from the last 7 days
 * and ensures they are in the inbox for processing
 */
@ApplicationScoped
public class UnprocessedPresentationsFetcher {

    private static final Logger log = LoggerFactory.getLogger(UnprocessedPresentationsFetcher.class);
    private static final int DAYS_TO_LOOK_BACK = 7;

    private final InboxRepository inboxRepository;

    public UnprocessedPresentationsFetcher(InboxRepository inboxRepository) {
        this.inboxRepository = inboxRepository;
    }

    /**
     * Runs daily at 7:00 AM to check for unprocessed presentations from the last 7 days
     */
    @Scheduled(cron = "0 0 7 * * ?")
    void fetchUnprocessedPresentations() {
        log.info("Starting scheduled fetch of unprocessed presentations from last {} days", DAYS_TO_LOOK_BACK);

        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(DAYS_TO_LOOK_BACK);

        List<PresentationId> unprocessed = inboxRepository.findUnprocessedSince(sevenDaysAgo);

        log.info("Found {} unprocessed presentations from the last {} days", unprocessed.size(), DAYS_TO_LOOK_BACK);

        if (!unprocessed.isEmpty()) {
            log.warn("There are {} presentations that have not been processed in the last {} days. " +
                    "The InboxProcessor will continue attempting to process them.",
                    unprocessed.size(), DAYS_TO_LOOK_BACK);

            // Log details for monitoring/alerting purposes
            for (PresentationId presentationId : unprocessed) {
                log.warn("Unprocessed presentation - ID: {}, FormID: {}, ReceivedAt: {}",
                        presentationId.getId(),
                        presentationId.getFormId(),
                        presentationId.getReceivedAt());
            }
        } else {
            log.info("All presentations from the last {} days have been processed successfully", DAYS_TO_LOOK_BACK);
        }
    }
}
