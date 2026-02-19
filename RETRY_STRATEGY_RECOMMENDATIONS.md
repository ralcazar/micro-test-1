# Retry Strategy Recommendations for FormPresentationReceiver

## Current Implementation Analysis

### ✅ What's Working Well

1. **@Transactional Annotations**: All use cases properly use `@Transactional` annotation:
   - `ReceiveFormCreatedUseCase` - saves to inbox transactionally
   - `ProcessPresentationUseCase` - processes business logic transactionally
   - `ProcessPresentationImmediatelyUseCase` - manages state transitions transactionally

2. **Inbox Pattern with State Machine**: The system uses a robust 3-state machine (PENDING → DOING → DONE) that prevents duplicate processing across multiple instances.

3. **Separation of Concerns**: The architecture properly separates:
   - Inbox insertion (resilient, always committed)
   - Business logic processing (can fail and retry)

4. **Consistent Use of PresentationId**: The domain model correctly uses `PresentationId` value object throughout the application layer.

### ⚠️ Issues Found

#### 1. **UUID Leakage in Infrastructure Layer**

The `InboxEntityRepository` uses raw `UUID` instead of `PresentationId` in its method signatures:

```java
// Current implementation (WRONG)
public boolean existsByPresentationId(UUID presentationId)
public void markAsProcessed(UUID presentationId)
public int tryMarkAsProcessing(UUID presentationId)
public void markAsUnprocessed(UUID presentationId)
```

**Problem**: This breaks the hexagonal architecture principle. The infrastructure layer should not expose primitive types when the domain has a value object.

**Solution**: Change method signatures to use `UUID` parameter names that reflect they are primitive infrastructure concerns, OR better yet, keep the conversion at the H2InboxRepository level (which is already doing this correctly).

**Recommendation**: The current implementation in `H2InboxRepository` is actually correct - it converts `PresentationId` to `UUID` before calling the entity repository. The `InboxEntityRepository` is a Panache repository (infrastructure detail) and using UUID there is acceptable. **No changes needed**.

## Retry Strategy Recommendations

### Current Retry Mechanism

The system implements a **passive retry strategy** with the following characteristics:

1. **Immediate Processing Attempt**: When an event is received, it tries to process immediately
2. **Graceful Degradation**: If immediate processing fails, the inbox entry is already saved
3. **Scheduled Retry**: The `InboxProcessor` runs every 10 seconds to retry failed items
4. **State Reversion**: Failed items are reverted from DOING → PENDING for retry

### Recommended Improvements

#### 1. **Add Exponential Backoff**

**Current Issue**: The scheduler retries every 10 seconds indefinitely, which can:
- Overwhelm external services if they're down
- Waste resources on permanently failing items
- Create noise in logs

**Recommendation**: Implement exponential backoff with a retry counter:

```java
@Entity
@Table(name = "inbox_presentations")
public class InboxEntity {
    // ... existing fields ...
    
    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;
    
    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;
    
    @Column(name = "last_error")
    private String lastError;
}
```

**Modified Repository Query**:
```java
public List<InboxEntity> findUnprocessed(int limit) {
    return find("status = 'PENDING' AND (nextRetryAt IS NULL OR nextRetryAt <= ?1) ORDER BY receivedAt ASC", 
                LocalDateTime.now())
            .page(0, limit)
            .list();
}
```

**Modified Use Case**:
```java
@Override
@Transactional
public void execute(PresentationId presentationId) {
    // ... existing tryMarkAsProcessing logic ...
    
    try {
        processPresentationCommand.execute(presentationId);
        inboxRepository.markAsProcessed(presentationId);
        
    } catch (Exception e) {
        log.warning(() -> "Error processing presentation " + presentationId + ": " + e.getMessage());
        
        // Calculate next retry time with exponential backoff
        int retryCount = inboxRepository.getRetryCount(presentationId);
        long delayMinutes = Math.min(Math.pow(2, retryCount), 60); // Max 60 minutes
        LocalDateTime nextRetry = LocalDateTime.now().plusMinutes(delayMinutes);
        
        inboxRepository.markAsUnprocessedWithRetry(presentationId, retryCount + 1, nextRetry, e.getMessage());
        
        throw e;
    }
}
```

**Backoff Schedule Example**:
- Retry 1: 2 minutes
- Retry 2: 4 minutes
- Retry 3: 8 minutes
- Retry 4: 16 minutes
- Retry 5: 32 minutes
- Retry 6+: 60 minutes (capped)

#### 2. **Add Maximum Retry Limit**

**Recommendation**: Add a maximum retry count to prevent infinite retries:

```java
public class ProcessPresentationImmediatelyUseCase {
    private static final int MAX_RETRIES = 10;
    
    @Override
    @Transactional
    public void execute(PresentationId presentationId) {
        // ... existing logic ...
        
        try {
            processPresentationCommand.execute(presentationId);
            inboxRepository.markAsProcessed(presentationId);
            
        } catch (Exception e) {
            int retryCount = inboxRepository.getRetryCount(presentationId);
            
            if (retryCount >= MAX_RETRIES) {
                log.severe(() -> "Presentation " + presentationId + " exceeded max retries (" + MAX_RETRIES + "), moving to FAILED");
                inboxRepository.markAsFailed(presentationId, e.getMessage());
                // Optionally: send to dead letter queue or alert monitoring
                return; // Don't rethrow - it's permanently failed
            }
            
            // Calculate exponential backoff and retry
            long delayMinutes = Math.min(Math.pow(2, retryCount), 60);
            LocalDateTime nextRetry = LocalDateTime.now().plusMinutes(delayMinutes);
            inboxRepository.markAsUnprocessedWithRetry(presentationId, retryCount + 1, nextRetry, e.getMessage());
            
            throw e;
        }
    }
}
```

**Add FAILED State**:
```java
// States: PENDING → DOING → DONE
//                    ↓
//                  FAILED (after max retries)
```

#### 3. **Add Retry Reason Classification**

**Recommendation**: Classify errors to determine retry strategy:

```java
public enum RetryStrategy {
    IMMEDIATE,      // Transient errors (network timeout)
    EXPONENTIAL,    // Service temporarily down
    NO_RETRY        // Permanent errors (validation, business rule)
}

public class ErrorClassifier {
    public static RetryStrategy classify(Exception e) {
        if (e instanceof ValidationException || e instanceof IllegalArgumentException) {
            return RetryStrategy.NO_RETRY; // Don't retry business logic errors
        }
        if (e instanceof TimeoutException || e instanceof ConnectException) {
            return RetryStrategy.IMMEDIATE; // Quick retry for transient issues
        }
        return RetryStrategy.EXPONENTIAL; // Default: exponential backoff
    }
}
```

#### 4. **Add Monitoring and Alerting**

**Recommendation**: Add metrics for observability:

```java
@ApplicationScoped
public class InboxProcessor {
    
    @Scheduled(every = "10s")
    void processInbox() {
        List<PresentationId> unprocessed = inboxRepository.findUnprocessed(BATCH_SIZE);
        
        if (unprocessed.isEmpty()) {
            return;
        }
        
        log.info("Processing {} unprocessed presentations from inbox", unprocessed.size());
        
        int successCount = 0;
        int failureCount = 0;
        
        for (PresentationId presentationId : unprocessed) {
            try {
                processPresentationImmediatelyCommand.execute(presentationId);
                successCount++;
            } catch (Exception e) {
                failureCount++;
                log.error("Error processing presentation {}: {}", presentationId, e.getMessage(), e);
            }
        }
        
        // Emit metrics for monitoring
        log.info("Inbox processing completed: {} succeeded, {} failed", successCount, failureCount);
        
        // Alert if failure rate is high
        if (failureCount > successCount && unprocessed.size() > 5) {
            log.severe("High failure rate detected in inbox processing!");
            // Send alert to monitoring system
        }
    }
}
```

#### 5. **Add Dead Letter Queue (DLQ)**

**Recommendation**: For items that exceed max retries, move to a DLQ for manual investigation:

```java
@Entity
@Table(name = "inbox_dead_letter_queue")
public class DeadLetterEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "form_id", nullable = false)
    private UUID formId;
    
    @Column(name = "original_received_at", nullable = false)
    private LocalDateTime originalReceivedAt;
    
    @Column(name = "moved_to_dlq_at", nullable = false)
    private LocalDateTime movedToDlqAt;
    
    @Column(name = "retry_count", nullable = false)
    private int retryCount;
    
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;
    
    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;
}
```

### Implementation Priority

1. **High Priority** (Implement Now):
   - ✅ @Transactional annotations (already done)
   - ✅ State machine with PENDING/DOING/DONE (already done)
   - ⚠️ Add retry counter and exponential backoff
   - ⚠️ Add maximum retry limit

2. **Medium Priority** (Implement Soon):
   - Add FAILED state for permanently failed items
   - Add error classification for smart retry strategies
   - Add monitoring metrics and alerting

3. **Low Priority** (Nice to Have):
   - Add Dead Letter Queue for manual investigation
   - Add retry history table for audit trail
   - Add dashboard for monitoring inbox health

## Configuration Recommendations

Add configuration properties for retry behavior:

```properties
# application.properties
inbox.retry.max-attempts=10
inbox.retry.initial-delay-minutes=2
inbox.retry.max-delay-minutes=60
inbox.retry.backoff-multiplier=2
inbox.scheduler.interval=10s
inbox.scheduler.batch-size=10
```

## Testing Recommendations

Add tests for retry scenarios:

```java
@Test
void shouldRetryWithExponentialBackoff() {
    // Given: A presentation that fails processing
    PresentationId id = PresentationId.of(UUID.randomUUID());
    when(processPresentationCommand.execute(id)).thenThrow(new RuntimeException("Service unavailable"));
    
    // When: Processing fails multiple times
    for (int i = 0; i < 3; i++) {
        assertThrows(RuntimeException.class, () -> useCase.execute(id));
    }
    
    // Then: Retry count increases and next retry time uses exponential backoff
    InboxEntity entity = repository.findByFormId(id.value());
    assertEquals(3, entity.getRetryCount());
    assertTrue(entity.getNextRetryAt().isAfter(LocalDateTime.now().plusMinutes(7))); // 2^3 = 8 minutes
}

@Test
void shouldMoveToFailedAfterMaxRetries() {
    // Given: A presentation that always fails
    PresentationId id = PresentationId.of(UUID.randomUUID());
    when(processPresentationCommand.execute(id)).thenThrow(new RuntimeException("Permanent error"));
    
    // When: Processing fails MAX_RETRIES times
    for (int i = 0; i < MAX_RETRIES; i++) {
        assertThrows(RuntimeException.class, () -> useCase.execute(id));
    }
    
    // Then: Status is FAILED and not retried anymore
    InboxEntity entity = repository.findByFormId(id.value());
    assertEquals("FAILED", entity.getStatus());
    assertEquals(MAX_RETRIES, entity.getRetryCount());
}
```

## Summary

### Current State: ✅ Good Foundation
- Proper use of @Transactional
- Robust state machine preventing duplicate processing
- Clean separation of concerns

### Recommended Improvements: ⚠️ Add Resilience
1. **Exponential backoff** to avoid overwhelming failing services
2. **Maximum retry limit** to prevent infinite retries
3. **Error classification** for smart retry strategies
4. **Monitoring and alerting** for operational visibility
5. **Dead Letter Queue** for manual investigation of permanent failures

### Architecture Verdict: ✅ No Changes Needed
The current use of UUID in `InboxEntityRepository` is acceptable as it's an infrastructure detail. The `H2InboxRepository` correctly handles the conversion between `PresentationId` (domain) and `UUID` (infrastructure).

The hexagonal architecture is properly maintained with clear boundaries between domain, application, and infrastructure layers.
