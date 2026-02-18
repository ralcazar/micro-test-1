package com.formpresentationreceiver.application.usecase;

import com.formpresentationreceiver.domain.port.output.InboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessPresentationUseCaseTest {

    @Mock
    private InboxRepository inboxRepository;

    private ProcessPresentationUseCase processPresentationUseCase;

    @BeforeEach
    void setUp() {
        processPresentationUseCase = new ProcessPresentationUseCase(inboxRepository);
    }

    @Test
    void shouldProcessPresentationSuccessfully() {
        UUID presentationId = UUID.randomUUID();

        processPresentationUseCase.execute(presentationId);

        verify(inboxRepository).markAsProcessed(presentationId);
    }

    @Test
    void shouldCallMarkAsProcessedWithCorrectId() {
        UUID presentationId = UUID.randomUUID();

        processPresentationUseCase.execute(presentationId);

        verify(inboxRepository, times(1)).markAsProcessed(presentationId);
        verifyNoMoreInteractions(inboxRepository);
    }

    @Test
    void shouldRethrowExceptionWhenProcessingFails() {
        UUID presentationId = UUID.randomUUID();
        RuntimeException expectedError = new RuntimeException("Processing failed");

        doThrow(expectedError).when(inboxRepository).markAsProcessed(presentationId);

        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> processPresentationUseCase.execute(presentationId)
        );

        assertEquals("Processing failed", thrown.getMessage());
    }

    @Test
    void shouldProcessMultiplePresentationsIndependently() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        processPresentationUseCase.execute(id1);
        processPresentationUseCase.execute(id2);

        verify(inboxRepository).markAsProcessed(id1);
        verify(inboxRepository).markAsProcessed(id2);
    }
}
