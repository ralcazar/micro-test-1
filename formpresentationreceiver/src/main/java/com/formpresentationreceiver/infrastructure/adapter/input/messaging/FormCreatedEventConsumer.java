package com.formpresentationreceiver.infrastructure.adapter.input.messaging;

import com.formpresentationreceiver.domain.model.PresentationId;
import com.formpresentationreceiver.domain.port.input.ProcessPresentationImmediatelyCommand;
import com.formpresentationreceiver.domain.port.input.ReceiveFormCreatedCommand;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.reactive.messaging.annotations.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * RabbitMQ consumer for form-created events.
 * Input adapter that receives messages and delegates to use cases.
 *
 * Calls two separate use cases in separate transactions for resilience:
 * 1. ReceiveFormCreatedCommand  - saves to inbox (committed first)
 * 2. ProcessPresentationImmediatelyCommand - processes immediately (scheduler retries on failure)
 *
 * Message handling strategy:
 * - Malformed / unparse-able messages: discarded immediately (logged, not re-queued)
 *   to avoid poison-message infinite loops. The DLQ configured in application.properties
 *   will receive NACKed messages for manual inspection.
 * - Inbox save failure (DB outage): re-thrown so the broker re-queues the message.
 * - Processing failure: swallowed; the inbox entry is committed and the scheduler retries.
 */
@ApplicationScoped
public class FormCreatedEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(FormCreatedEventConsumer.class);

    private final ReceiveFormCreatedCommand receiveFormCreatedCommand;
    private final ProcessPresentationImmediatelyCommand processPresentationImmediatelyCommand;
    private final ObjectMapper objectMapper;

    public FormCreatedEventConsumer(
            ReceiveFormCreatedCommand receiveFormCreatedCommand,
            ProcessPresentationImmediatelyCommand processPresentationImmediatelyCommand) {
        this.receiveFormCreatedCommand = receiveFormCreatedCommand;
        this.processPresentationImmediatelyCommand = processPresentationImmediatelyCommand;
        this.objectMapper = new ObjectMapper();
    }

    @Incoming("form-created-in")
    @Blocking
    public void consume(String message) {
        log.info("Received form-created event: {}", message);

        // --- Parse and validate -------------------------------------------------
        // Malformed messages are discarded (not re-thrown) to avoid infinite requeue
        // of poison messages. They are captured by the DLQ if configured.
        PresentationId presentationId;
        try {
            JsonNode jsonNode = objectMapper.readTree(message);
            if (jsonNode == null || !jsonNode.has("formId")) {
                log.error("Malformed form-created event (missing formId), discarding: {}", message);
                return;
            }
            String formIdStr = jsonNode.get("formId").asText();
            UUID formIdUuid = UUID.fromString(formIdStr);
            presentationId = PresentationId.of(formIdUuid);
        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID in form-created event, discarding: {} — {}", message, e.getMessage());
            return;
        } catch (Exception e) {
            log.error("Failed to parse form-created event, discarding: {} — {}", message, e.getMessage());
            return;
        }

        // --- Step 1: Save to inbox (own transaction) ----------------------------
        // Re-throw on failure so the broker keeps the message and retries delivery.
        // The inbox entry was never written, so there is no duplicate risk.
        try {
            receiveFormCreatedCommand.execute(presentationId);
        } catch (Exception e) {
            log.error("Failed to persist {} to inbox, re-queuing for broker retry: {}", presentationId, e.getMessage());
            throw new RuntimeException("Inbox save failed for presentationId=" + presentationId, e);
        }

        // --- Step 2: Immediate processing (separate transaction) ----------------
        // Inbox is already committed. Swallow failure — the scheduler will retry.
        try {
            log.info("Triggering immediate processing for presentation ID: {}", presentationId);
            processPresentationImmediatelyCommand.execute(presentationId);
        } catch (Exception e) {
            log.warn("Immediate processing failed for {}, will be retried by scheduler: {}", presentationId, e.getMessage());
        }
    }
}
