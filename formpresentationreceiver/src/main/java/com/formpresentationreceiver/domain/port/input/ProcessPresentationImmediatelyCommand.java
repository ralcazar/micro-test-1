package com.formpresentationreceiver.domain.port.input;

import com.formpresentationreceiver.domain.model.PresentationId;

/**
 * Input port for processing a presentation immediately after receiving it
 * This encapsulates the logic of trying to process, marking states, and handling errors
 */
public interface ProcessPresentationImmediatelyCommand {

    /**
     * Try to process a presentation immediately
     * If successful, marks as processed
     * If fails, reverts to unprocessed for scheduler retry
     * If already being processed, does nothing
     */
    void execute(PresentationId presentationId);
}
