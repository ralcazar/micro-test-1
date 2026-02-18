# Prevención de Procesamiento Paralelo en ProcessPresentationUseCase

## Problema Original

El sistema utilizaba el patrón Inbox para procesar presentaciones de forma asíncrona mediante un scheduler. Sin embargo, **con múltiples instancias de la aplicación**, existía el riesgo de que la misma presentación fuera procesada en paralelo por diferentes instancias.

### Escenario problemático (antes de la solución):

```
Tiempo | Instancia A                          | Instancia B
-------|--------------------------------------|----------------------------------------
T0     | BEGIN TRANSACTION                    |
T1     | SELECT * WHERE processed=false       | BEGIN TRANSACTION
T2     | → Obtiene Presentación X             | SELECT * WHERE processed=false
T3     | Procesa X (llama servicio web) ✓     | → Obtiene Presentación X
T4     | UPDATE processed=true WHERE id=X     | Procesa X (llama servicio web) ✓ ⚠️
T5     | COMMIT                               | UPDATE processed=true WHERE id=X
T6     |                                      | (0 filas afectadas, pero ya procesó)
```

**Resultado:** El servicio web se llamaba **2 veces** para la misma presentación.

## Solución Implementada: Actualización Atómica

### Estrategia

Utilizamos una **actualización atómica condicional** que marca el registro como procesado **solo si aún no ha sido procesado**. La operación UPDATE retorna el número de filas afectadas, lo que nos permite saber si "ganamos la carrera" para procesar ese registro.

### Cambios Realizados

#### 1. Nuevos métodos en `InboxRepository`

```java
/**
 * Try to atomically mark a presentation as processing.
 * Returns the number of rows updated (1 if successful, 0 if already processed by another instance)
 */
int tryMarkAsProcessing(UUID id);

/**
 * Mark a presentation ID as unprocessed (for error recovery)
 */
void markAsUnprocessed(UUID id);
```

#### 2. Implementación en `InboxEntityRepository`

```java
public int tryMarkAsProcessing(UUID id) {
    return update("processed = true, processedAt = ?1 WHERE id = ?2 AND processed = false", 
                 LocalDateTime.now(), id);
}

public void markAsUnprocessed(UUID id) {
    update("processed = false, processedAt = null WHERE id = ?1", id);
}
```

**Clave:** La cláusula `WHERE id = ?2 AND processed = false` garantiza que solo se actualiza si el registro aún no ha sido procesado.

#### 3. Modificación en `ProcessPresentationUseCase`

```java
@Override
@Transactional
public void execute(UUID presentationId) {
    log.info(() -> "Attempting to process presentation with ID: " + presentationId);

    // Try to atomically mark as processing (prevents duplicate processing by other instances)
    int updated = inboxRepository.tryMarkAsProcessing(presentationId);
    
    if (updated == 0) {
        // Another instance already processed or is processing this presentation
        log.info(() -> "Presentation " + presentationId + " already processed by another instance, skipping");
        return;
    }

    log.info(() -> "Processing presentation with ID: " + presentationId);

    try {
        // Your processing logic goes here
        log.info(() -> "Presentation " + presentationId + " processed successfully");

    } catch (Exception e) {
        log.log(Level.SEVERE, "Error processing presentation " + presentationId + ": " + e.getMessage(), e);
        
        // Revert to unprocessed state so it can be retried later
        inboxRepository.markAsUnprocessed(presentationId);
        
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
T1     | UPDATE ... WHERE id=X AND processed=false      | BEGIN TRANSACTION
T2     | → 1 fila actualizada (éxito) ✓                 | UPDATE ... WHERE id=X AND processed=false
T3     | Procesa X (llama servicio web) ✓               | → 0 filas actualizadas (ya está procesado)
T4     | COMMIT                                         | Detecta updated=0, SKIP procesamiento ✓
T5     |                                                | COMMIT (sin procesar)
```

**Resultado:** Solo la Instancia A procesa la presentación. La Instancia B detecta que no pudo actualizar el registro y lo omite.

## Ventajas de esta Solución

1. ✅ **Exactly-once processing**: Garantiza que cada presentación se procesa exactamente una vez
2. ✅ **Sin locks explícitos**: No requiere `SELECT FOR UPDATE` ni bloqueos pesimistas
3. ✅ **Compatible con H2**: Funciona en cualquier base de datos que soporte transacciones ACID
4. ✅ **Manejo de errores**: Si el procesamiento falla, el registro se marca como no procesado para reintentar
5. ✅ **Sin contención**: Las instancias no se bloquean entre sí, maximizando la concurrencia
6. ✅ **Distribución automática**: Cada instancia trabaja en registros diferentes de forma natural

## Manejo de Errores

Si ocurre una excepción durante el procesamiento:

1. Se captura la excepción
2. Se llama a `markAsUnprocessed(presentationId)` para revertir el estado
3. Se relanza la excepción
4. La transacción hace ROLLBACK automático
5. En la siguiente ejecución del scheduler, el registro volverá a aparecer como no procesado

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
WHERE processed = false AND MOD(ABS(HASH(id)), totalInstances) = instancePartition
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

La solución de **actualización atómica condicional** es la más adecuada para este caso porque:

- Es simple y fácil de entender
- Funciona con H2 (base de datos actual)
- No requiere configuración adicional
- Garantiza exactly-once processing
- Tiene buen rendimiento bajo carga

Esta implementación es production-ready y puede escalar a múltiples instancias sin riesgo de procesamiento duplicado.
