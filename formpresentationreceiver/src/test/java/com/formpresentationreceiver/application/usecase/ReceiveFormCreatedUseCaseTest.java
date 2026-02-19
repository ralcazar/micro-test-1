package com.formpresentationreceiver.application.usecase;

import com.formpresentationreceiver.domain.model.PresentationId;
import com.formpresentationreceiver.domain.port.input.ProcessPresentationImmediatelyCommand;
import com.formpresentationreceiver.domain.port.output.InboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReceiveFormCreatedUseCaseTest {

    @Mock
    private InboxRepository inboxRepository;

    @Mock
    private ProcessPresentationImmediatelyCommand processPresentationImmediatelyCommand;

    private ReceiveFormCreatedUseCase receiveFormCreatedUseCase;

    @BeforeEach
    void setUp() {
        receiveFormCreatedUseCase = new ReceiveFormCreatedUseCase(inboxRepository, processPresentationImmediatelyCommand);
    }

    @Test
    void shouldSaveNewPresentationIdToInbox() {
        UUID uuid = UUID.randomUUID();
        PresentationId presentationId = PresentationId.of(uuid);
        
        when(inboxRepository.existsByPresentationId(any(PresentationId.class))).thenReturn(false);

        receiveFormCreatedUseCase.execute(presentationId);

        verify(inboxRepository).save(presentationId);
        verify(processPresentationImmediatelyCommand).execute(presentationId);
    }

    @Test
    void shouldSkipDuplicatePresentationId() {
        UUID uuid = UUID.randomUUID();
        PresentationId presentationId = PresentationId.of(uuid);
        when(inboxRepository.existsByPresentationId(any(PresentationId.class))).thenReturn(true);

        receiveFormCreatedUseCase.execute(presentationId);

        verify(inboxRepository, never()).save(any(PresentationId.class));
        verify(processPresentationImmediatelyCommand, never()).execute(any(PresentationId.class));
    }

    @Test
    void shouldCheckForExistingPresentationIdBeforeSaving() {
        UUID uuid = UUID.randomUUID();
        PresentationId presentationId = PresentationId.of(uuid);
        
        when(inboxRepository.existsByPresentationId(any(PresentationId.class))).thenReturn(false);

        receiveFormCreatedUseCase.execute(presentationId);

        var inOrder = inOrder(inboxRepository, processPresentationImmediatelyCommand);
        inOrder.verify(inboxRepository).existsByPresentationId(presentationId);
        inOrder.verify(inboxRepository).save(presentationId);
        inOrder.verify(processPresentationImmediatelyCommand).execute(presentationId);
    }

    @Test
    void shouldHandleMultipleNewPresentationIds() {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        PresentationId presentationId1 = PresentationId.of(uuid1);
        PresentationId presentationId2 = PresentationId.of(uuid2);

        when(inboxRepository.existsByPresentationId(any(PresentationId.class))).thenReturn(false);

        receiveFormCreatedUseCase.execute(presentationId1);
        receiveFormCreatedUseCase.execute(presentationId2);

        verify(inboxRepository, times(2)).save(any(PresentationId.class));
        verify(processPresentationImmediatelyCommand, times(2)).execute(any(PresentationId.class));
    }

    @Test
    void shouldNotSaveWhenPresentationIdAlreadyExists() {
        UUID uuid = UUID.randomUUID();
        PresentationId presentationId = PresentationId.of(uuid);
        when(inboxRepository.existsByPresentationId(any(PresentationId.class))).thenReturn(true);

        receiveFormCreatedUseCase.execute(presentationId);

        verify(inboxRepository).existsByPresentationId(presentationId);
        verify(inboxRepository, never()).save(any());
        verify(processPresentationImmediatelyCommand, never()).execute(any(PresentationId.class));
    }

    @Test
    void shouldHandleMixOfNewAndDuplicatePresentationIds() {
        UUID newUuid = UUID.randomUUID();
        UUID duplicateUuid = UUID.randomUUID();
        PresentationId newPresentationId = PresentationId.of(newUuid);
        PresentationId duplicatePresentationId = PresentationId.of(duplicateUuid);

        when(inboxRepository.existsByPresentationId(newPresentationId)).thenReturn(false);
        when(inboxRepository.existsByPresentationId(duplicatePresentationId)).thenReturn(true);

        receiveFormCreatedUseCase.execute(newPresentationId);
        receiveFormCreatedUseCase.execute(duplicatePresentationId);

        verify(inboxRepository, times(1)).save(any(PresentationId.class));
        verify(processPresentationImmediatelyCommand, times(1)).execute(any(PresentationId.class));
    }
    
    @Test
    void shouldContinueEvenIfImmediateProcessingFails() {
        UUID uuid = UUID.randomUUID();
        PresentationId presentationId = PresentationId.of(uuid);
        
        when(inboxRepository.existsByPresentationId(any(PresentationId.class))).thenReturn(false);
        doThrow(new RuntimeException("Processing error")).when(processPresentationImmediatelyCommand).execute(any(PresentationId.class));

        receiveFormCreatedUseCase.execute(presentationId);

        verify(inboxRepository).save(presentationId);
        verify(processPresentationImmediatelyCommand).execute(presentationId);
        // No exception should be thrown, it should be caught and logged
    }
}
