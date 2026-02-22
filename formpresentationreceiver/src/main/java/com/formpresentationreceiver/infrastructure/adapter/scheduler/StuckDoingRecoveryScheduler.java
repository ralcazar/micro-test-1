package com.formpresentationreceiver.infrastructure.adapter.scheduler;

import com.formpresentationreceiver.domain.port.output.InboxRepository;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

/**
 * Scheduled job that recovers presentations stuck in DOING state.
 *
 * A presentation can become stuck in DOING if the service crashes after marking it
 * as DOING but before completing processing. Without recovery, those items would
 * remain DOING forever and never be retried.
 *
 * This scheduler runs every 5 minutes and resets any DOING item whose attemptedAt
 * timestamp is older than STUCK_THRESHOLD_MINUTES back to PENDING so the
 * InboxProcessor can pick it up again.
 */
@ApplicationScoped
public class StuckDoingRecoveryScheduler {

    private static final Logger log = LoggerFactory.getLogger(StuckDoingRecoveryScheduler.class);
    private static final int STUCK_THRESHOLD_MINUTES = 5;

    private final InboxRepository inboxRepository;

    public StuckDoingRecoveryScheduler(InboxRepository inboxRepository) {
        this.inboxRepository = inboxRepository;
    }

    @Scheduled(every = "5m")
    @Transactional
    void recoverStuckDoingItems() {
        LocalDateTime stuckSince = LocalDateTime.now().minusMinutes(STUCK_THRESHOLD_MINUTES);
        int recovered = inboxRepository.resetStuckDoingItems(stuckSince);
        if (recovered > 0) {
            log.warn("Recovered {} presentation(s) stuck in DOING state for more than {} minutes. "
                    + "This may indicate service crashes or very slow processing.",
                    recovered, STUCK_THRESHOLD_MINUTES);
        }
    }
}
