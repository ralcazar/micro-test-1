package com.formpresentationreceiver.infrastructure.adapter.input.health;

import com.formpresentationreceiver.infrastructure.adapter.output.persistence.InboxEntityRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

import java.time.LocalDateTime;

/**
 * Readiness health check that reports DOWN if the inbox has too many
 * permanently FAILED presentations or presentations stuck as PENDING
 * for an unexpectedly long time.
 */
@Readiness
@ApplicationScoped
public class InboxHealthCheck implements HealthCheck {

    private static final int FAILED_THRESHOLD = 10;
    private static final int STALE_PENDING_THRESHOLD = 50;
    private static final int STALE_HOURS = 1;

    private final InboxEntityRepository inboxEntityRepository;

    public InboxHealthCheck(InboxEntityRepository inboxEntityRepository) {
        this.inboxEntityRepository = inboxEntityRepository;
    }

    @Override
    public HealthCheckResponse call() {
        long failedCount = inboxEntityRepository.count("status", "FAILED");
        long stalePendingCount = inboxEntityRepository
                .count("status = 'PENDING' AND receivedAt < ?1",
                        LocalDateTime.now().minusHours(STALE_HOURS));

        if (failedCount >= FAILED_THRESHOLD) {
            return HealthCheckResponse.named("inbox")
                    .down()
                    .withData("failed_presentations", failedCount)
                    .withData("stale_pending_presentations", stalePendingCount)
                    .withData("message", "Too many permanently failed inbox presentations")
                    .build();
        }

        if (stalePendingCount >= STALE_PENDING_THRESHOLD) {
            return HealthCheckResponse.named("inbox")
                    .down()
                    .withData("failed_presentations", failedCount)
                    .withData("stale_pending_presentations", stalePendingCount)
                    .withData("message", "Too many stale PENDING presentations (> " + STALE_HOURS + "h old)")
                    .build();
        }

        return HealthCheckResponse.named("inbox")
                .up()
                .withData("failed_presentations", failedCount)
                .withData("stale_pending_presentations", stalePendingCount)
                .build();
    }
}
