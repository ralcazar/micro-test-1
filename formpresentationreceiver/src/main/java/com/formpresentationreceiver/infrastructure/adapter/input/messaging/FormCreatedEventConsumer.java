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
 * RabbitMQ consumer for form-created events
 * Input adapter that receives messages and delegates to use cases
 * Calls two separate use cases in separate transactions for resilience:
 * 1. ReceiveFormCreatedCommand - saves to inbox (committed first)
 * 2. ProcessPresentationImmediatelyCommand - processes immediately (if fails, scheduler will retry)
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
        try {
            log.info("Received form-created event: {}", message);

            // Parse the message to extract formId
            JsonNode jsonNode = objectMapper.readTree(message);
            String formIdStr = jsonNode.get("formId").asText();
            UUID formIdUuid = UUID.fromString(formIdStr);
            PresentationId presentationId = PresentationId.of(formIdUuid);

            // Step 1: Save to inbox in its own transaction (resilient - always committed)
            receiveFormCreatedCommand.execute(presentationId);

            // Step 2: Try immediate processing in separate transaction
            // If this fails, the inbox entry is already saved and scheduler will retry
            try {
                log.info("Triggering immediate processing for presentation ID: {}", presentationId);
                processPresentationImmediatelyCommand.execute(presentationId);
            } catch (Exception e) {
                log.warn("Immediate processing failed for {}, will be retried by scheduler", presentationId, e);
                // Don't rethrow - inbox entry is saved, scheduler will handle retry
            }

        } catch (Exception e) {
            log.error("Error processing form-created event: {}", message, e);
            // In a production system, you might want to send to a dead letter queue
            throw new RuntimeException("Failed to process form-created event", e);
        }
    }
}
