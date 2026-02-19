package com.formpresentationreceiver.domain.port.input;

import com.formpresentationreceiver.domain.model.PresentationId;

/**
 * Functional interface for processing a presentation
 * The caller is responsible for managing the presentation state (tryMark, markAsXXX)
 */
@FunctionalInterface
public interface ProcessPresentationCommand {

    /**
     * Process a presentation by its ID
     * This method should only contain the business logic for processing.
     * State management (marking as processing, processed, etc.) should be handled externally.
     * 
     * @param presentationId the ID of the presentation to process
     */
    void execute(PresentationId presentationId);
}
