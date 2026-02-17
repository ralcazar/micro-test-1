package com.formplatform.infrastructure.adapter.output.messaging;

import com.formplatform.infrastructure.adapter.output.persistence.OutboxRepository;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Scheduled job that reads pending outbox events and publishes them to RabbitMQ.
 * When the queue is unavailable, events remain in PENDING and are retried on the next run.
 */
@ApplicationScoped
public class OutboxProcessor {

    private static final Logger log = LoggerFactory.getLogger(OutboxProcessor.class);
    private static final int BATCH_SIZE = 50;
    private static final int MAX_RETRIES = 10;

    private final OutboxRepository outboxRepository;
    private final RabbitMQEventPublisher rabbitMQEventPublisher;

    public OutboxProcessor(OutboxRepository outboxRepository, RabbitMQEventPublisher rabbitMQEventPublisher) {
        this.outboxRepository = outboxRepository;
        this.rabbitMQEventPublisher = rabbitMQEventPublisher;
    }

    @Scheduled(every = "5s")
    void processOutbox() {
        List<OutboxRepository.PendingOutboxEvent> pending = outboxRepository.findPending(BATCH_SIZE);
        if (pending.isEmpty()) {
            return;
        }
        for (OutboxRepository.PendingOutboxEvent event : pending) {
            try {
                if ("form-created".equals(event.channel())) {
                    rabbitMQEventPublisher.sendMessage(event.payload());
                    outboxRepository.markSent(event.id());
                }
            } catch (Exception e) {
                log.warn("Failed to publish outbox event id={}, retries will continue: {}", event.id(), e.getMessage());
                outboxRepository.incrementRetry(event.id());
                if (event.retryCount() + 1 >= MAX_RETRIES) {
                    outboxRepository.markFailed(event.id());
                    log.error("Outbox event id={} marked as FAILED after {} retries", event.id(), MAX_RETRIES);
                }
            }
        }
    }
}
