package com.formpresentationreceiver.domain.port.input;

import java.util.UUID;

/**
 * Input port for processing a presentation
 */
public interface ProcessPresentationCommand {
    
    /**
     * Process a presentation by its ID
     */
    void execute(UUID presentationId);
}
