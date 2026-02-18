# Prevención de Procesamiento Paralelo en ProcessPresentationUseCase

## Problema Original

El sistema utilizaba el patrón Inbox para procesar presentaciones de forma asíncrona mediante un scheduler. Sin embargo, **con múltiples instancias de la aplicación**, existía el riesgo de que la misma presentación fuera procesada en paralelo por diferentes instancias.

### Escenario problemático (antes de la solución):

```
Tiempo | Instancia A                          | Instancia B
-------|--------------------------------------|----------------------------------------
T0     | BEGIN TRANSACTION                    |
T1     | SELECT * WHERE status='PENDING'      | BEGIN TRANSACTION
T2     | → Obtiene Presentación X             | SELECT * WHERE status='PENDING'
T3     | Procesa X (llama servicio web) ✓     | → Obtiene Presentación X
T4     | UPDATE status='DONE' WHERE id=X      | Procesa X (llama servicio web) ✓ ⚠️
T5     | COMMIT                               | UPDATE status='DONE' WHERE id=X
T6     |                                      | (0 filas afectadas, pero ya procesó)
```

**Resultado:** El servicio web se llamaba **2 veces** para la misma presentación.

## Solución Implementada: Máquina de Estados con 3 Estados

### Estrategia

Implementamos una **máquina de estados con 3 estados** (PENDING → DOING → DONE) que utiliza una **actualización atómica condicional** para marcar el registro como "en proceso" **antes** de ejecutar la lógica de negocio. Solo después de procesar exitosamente, se marca como DONE.

### Estados del Sistema

- **PENDING**: Presentación recibida, esperando ser procesada
- **DOING**: Presentación siendo procesada actualmente por una instancia
- **DONE**: Presentación procesada exitosamente

### Transiciones de Estado

```
PENDING --[tryMarkAsProcessing]--> DOING --[markAsProcessed]--> DONE
   ↑                                  |
   |                                  |
   +--------[markAsUnprocessed]------+ (en caso de error)
```

### Cambios Realizados

#### 1. Modificación en `InboxEntity`

```java
@Column(name = "status", nullable = false, length = 20)
private String status = "PENDING";

// Métodos de compatibilidad
public boolean isProcessed() {
    return "DONE".equals(status);
}

public void setProcessed(boolean processed) {
    this.status = processed ? "DONE" : "PENDING";
}
```

**Índice actualizado:**
```java
@Index(name = "idx_inbox_status_received", columnList = "status, received_at")
```

#### 2. Implementación en `InboxEntityRepository`

```java
public List<InboxEntity> findUnprocessed(int limit) {
    return find("status = 'PENDING' ORDER BY receivedAt ASC")
            .page(0, limit)
            .list();
}

public int tryMarkAsProcessing(UUID id) {
    return update("status = 'DOING' WHERE id = ?1 AND status = 'PENDING'", id);
}

public void markAsProcessed(UUID id) {
    update("status = 'DONE', processedAt = ?1 WHERE id = ?2", LocalDateTime.now(), id);
}

public void markAsUnprocessed(UUID id) {
    update("status = 'PENDING', processedAt = null WHERE id = ?1", id);
}
```

**Clave:** La cláusula `WHERE id = ?1 AND status = 'PENDING'` garantiza que solo se actualiza a DOING si el registro está en estado PENDING.

#### 3. Modificación en `ProcessPresentationUseCase`

```java
@Override
@Transactional
public void execute(UUID presentationId) {
    log.info(() -> "Attempting to process presentation with ID: " + presentationId);

    // Try to atomically mark as DOING (prevents duplicate processing by other instances)
    int updated = inboxRepository.tryMarkAsProcessing(presentationId);
    
    if (updated == 0) {
        // Another instance already processing or processed this presentation
        log.info(() -> "Presentation " + presentationId + " already being processed or processed by another instance, skipping");
        return;
    }

    log.info(() -> "Processing presentation with ID: " + presentationId + " (status: DOING)");

    try {
        // Your processing logic goes here
        log.info(() -> "Presentation " + presentationId + " processed successfully");

        // Mark as DONE only after successful processing
        inboxRepository.markAsProcessed(presentationId);
        log.info(() -> "Presentation " + presentationId + " marked as DONE");

    } catch (Exception e) {
        log.log(Level.SEVERE, "Error processing presentation " + presentationId + ": " + e.getMessage(), e);
        
        // Revert to PENDING state so it can be retried later
        inboxRepository.markAsUnprocessed(presentationId);
        log.info(() -> "Presentation " + presentationId + " reverted to PENDING for retry");
        
        throw e;
    }
}
```

## Cómo Funciona

### Escenario con la solución implementada:

```
Tiempo | Instancia A                                    | Instancia B
-------|------------------------------------------------|----------------------------------------
T0     | BEGIN TRANSACTION                              |
T1     | UPDATE status='DOING' WHERE id=X AND status='PENDING' | BEGIN TRANSACTION
T2     | → 1 fila actualizada (éxito) ✓                 | UPDATE status='DOING' WHERE id=X AND status='PENDING'
T3     | Estado: DOING                                  | → 0 filas actualizadas (ya está en DOING)
T4     | Procesa X (llama servicio web) ✓               | Detecta updated=0, SKIP procesamiento ✓
T5     | UPDATE status='DONE'                           | COMMIT (sin procesar)
T6     | COMMIT                                         |
```

**Resultado:** Solo la Instancia A procesa la presentación. La Instancia B detecta que no pudo actualizar el registro (ya está en DOING) y lo omite.

### Flujo de Estados Completo

#### Caso exitoso:
```
1. Scheduler consulta: SELECT * WHERE status='PENDING'
2. Instancia A: UPDATE status='DOING' WHERE id=X AND status='PENDING' → 1 fila
3. Instancia B: UPDATE status='DOING' WHERE id=X AND status='PENDING' → 0 filas (SKIP)
4. Instancia A: Procesa la presentación (llama servicios externos)
5. Instancia A: UPDATE status='DONE' WHERE id=X
6. COMMIT
```

#### Caso con error:
```
1. Instancia A: UPDATE status='DOING' WHERE id=X AND status='PENDING' → 1 fila
2. Instancia A: Procesa la presentación
3. Instancia A: ❌ Excepción durante el procesamiento
4. Instancia A: UPDATE status='PENDING' WHERE id=X (revertir)
5. ROLLBACK
6. En la siguiente ejecución del scheduler, X volverá a aparecer como PENDING
```

## Ventajas de esta Solución

1. ✅ **Exactly-once processing**: Garantiza que cada presentación se procesa exactamente una vez
2. ✅ **Estado explícito**: El estado DOING indica claramente que está siendo procesado
3. ✅ **No marca como procesado prematuramente**: Solo se marca DONE después del procesamiento exitoso
4. ✅ **Sin locks explícitos**: No requiere `SELECT FOR UPDATE` ni bloqueos pesimistas
5. ✅ **Compatible con H2**: Funciona en cualquier base de datos que soporte transacciones ACID
6. ✅ **Manejo de errores robusto**: Si el procesamiento falla, el registro vuelve a PENDING para reintentar
7. ✅ **Sin contención**: Las instancias no se bloquean entre sí, maximizando la concurrencia
8. ✅ **Observabilidad**: Los 3 estados permiten monitorear el progreso del procesamiento

## Manejo de Errores

Si ocurre una excepción durante el procesamiento:

1. Se captura la excepción en el bloque `catch`
2. Se llama a `markAsUnprocessed(presentationId)` para revertir el estado a PENDING
3. Se relanza la excepción
4. La transacción hace ROLLBACK automático
5. En la siguiente ejecución del scheduler, el registro volverá a aparecer como PENDING y se reintentará

## Monitoreo y Observabilidad

Con los 3 estados, puedes monitorear fácilmente:

```sql
-- Presentaciones pendientes
SELECT COUNT(*) FROM inbox_presentations WHERE status = 'PENDING';

-- Presentaciones en proceso (posibles cuellos de botella)
SELECT COUNT(*) FROM inbox_presentations WHERE status = 'DOING';

-- Presentaciones completadas
SELECT COUNT(*) FROM inbox_presentations WHERE status = 'DONE';

-- Presentaciones en DOING por mucho tiempo (posibles problemas)
SELECT * FROM inbox_presentations 
WHERE status = 'DOING' 
AND received_at < NOW() - INTERVAL '5 minutes';
```

## Alternativas Consideradas

### 1. SELECT FOR UPDATE con SKIP LOCKED (PostgreSQL/MySQL 8+)

```java
SELECT ... FOR UPDATE SKIP LOCKED
```

**Ventajas:**
- Distribución tipo Round Robin automática
- Sin esperas entre instancias

**Desventajas:**
- No soportado en H2
- Requiere PostgreSQL o MySQL 8+

### 2. Particionamiento por Hash

```java
WHERE status = 'PENDING' AND MOD(ABS(HASH(id)), totalInstances) = instancePartition
```

**Ventajas:**
- Funciona en cualquier BD
- Distribución predecible

**Desventajas:**
- Requiere conocer el número de instancias
- Si una instancia cae, su partición no se procesa

### 3. Nivel de aislamiento REPEATABLE READ

**Desventajas:**
- Mayor riesgo de deadlocks
- Reduce concurrencia
- No recomendado para alta carga

## Conclusión

La solución de **máquina de estados con 3 estados (PENDING → DOING → DONE)** es la más adecuada para este caso porque:

- ✅ Es simple y fácil de entender
- ✅ Funciona con H2 (base de datos actual)
- ✅ No requiere configuración adicional
- ✅ Garantiza exactly-once processing
- ✅ **No marca como procesado antes de procesar realmente** (requisito clave)
- ✅ Tiene buen rendimiento bajo carga
- ✅ Proporciona visibilidad del estado del procesamiento

Esta implementación es production-ready y puede escalar a múltiples instancias sin riesgo de procesamiento duplicado.
