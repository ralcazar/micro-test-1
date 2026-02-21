package com.formplatform.infrastructure.adapter.input.health;

import com.formplatform.infrastructure.adapter.output.persistence.OutboxEventEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

/**
 * Readiness health check that reports DOWN if too many outbox events have
 * accumulated as FAILED — a signal that RabbitMQ publishing is broken.
 */
@Readiness
@ApplicationScoped
public class OutboxHealthCheck implements HealthCheck {

    private static final int FAILED_THRESHOLD = 50;

    private final EntityManager entityManager;

    public OutboxHealthCheck(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public HealthCheckResponse call() {
        long failedCount = (long) entityManager
                .createQuery("SELECT COUNT(e) FROM OutboxEventEntity e WHERE e.status = :status")
                .setParameter("status", OutboxEventEntity.Status.FAILED)
                .getSingleResult();

        long pendingCount = (long) entityManager
                .createQuery("SELECT COUNT(e) FROM OutboxEventEntity e WHERE e.status = :status")
                .setParameter("status", OutboxEventEntity.Status.PENDING)
                .getSingleResult();

        if (failedCount >= FAILED_THRESHOLD) {
            return HealthCheckResponse.named("outbox")
                    .down()
                    .withData("failed_events", failedCount)
                    .withData("pending_events", pendingCount)
                    .withData("message", "Too many failed outbox events - RabbitMQ publishing may be broken")
                    .build();
        }

        return HealthCheckResponse.named("outbox")
                .up()
                .withData("failed_events", failedCount)
                .withData("pending_events", pendingCount)
                .build();
    }
}
