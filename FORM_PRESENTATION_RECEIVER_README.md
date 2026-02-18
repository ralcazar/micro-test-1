# FormPresentationReceiver Microservice

## Descripción

El microservicio **FormPresentationReceiver** es un consumidor de eventos que implementa el patrón Inbox para procesar eventos de formularios creados de manera confiable y resiliente.

## Arquitectura Hexagonal

El microservicio sigue los principios de arquitectura hexagonal (puertos y adaptadores):

### Capas

```
com.formpresentationreceiver/
├── domain/                          # Capa de Dominio (núcleo del negocio)
│   ├── model/
│   │   └── PresentationId.java     # Entidad de dominio
│   └── port/
│       ├── input/                   # Puertos de entrada (casos de uso)
│       │   ├── ReceiveFormCreatedCommand.java
│       │   └── ProcessPresentationCommand.java
│       └── output/                  # Puertos de salida (repositorios)
│           └── InboxRepository.java
│
├── application/                     # Capa de Aplicación (casos de uso)
│   └── usecase/
│       ├── ReceiveFormCreatedUseCase.java
│       └── ProcessPresentationUseCase.java
│
└── infrastructure/                  # Capa de Infraestructura (adaptadores)
    ├── adapter/
    │   ├── input/
    │   │   └── messaging/
    │   │       └── FormCreatedEventConsumer.java  # Adaptador RabbitMQ
    │   ├── output/
    │   │   └── persistence/
    │   │       ├── InboxEntity.java
    │   │       ├── InboxEntityRepository.java
    │   │       └── H2InboxRepository.java
    │   └── scheduler/
    │       ├── InboxProcessor.java                # Procesador de inbox
    │       └── UnprocessedPresentationsFetcher.java  # Tarea programada diaria
    └── config/
        └── BeanConfiguration.java
```

## Características Principales

### 1. Patrón Inbox
- **Almacenamiento idempotente**: Los eventos recibidos se almacenan en una tabla `inbox_presentations` con un índice único en `form_id` para evitar duplicados.
- **Procesamiento asíncrono**: Los eventos se procesan de forma independiente a su recepción.
- **Resiliencia**: Si el procesamiento falla, el evento permanece en el inbox para reintentos.

### 2. Consumidor de Eventos RabbitMQ
- **Canal**: `form-created-in`
- **Exchange**: `form-events` (tipo topic)
- **Routing Key**: `form.created`
- **Queue**: `form-presentation-receiver-queue` (durable)
- **Idempotencia**: Verifica si el `formId` ya existe antes de insertarlo.

### 3. Procesador de Inbox
- **Frecuencia**: Cada 10 segundos
- **Batch Size**: 10 presentaciones por ejecución
- **Función**: Procesa las presentaciones no procesadas del inbox
- **Manejo de errores**: Continúa procesando otros elementos aunque uno falle

### 4. Tarea Programada Diaria
- **Horario**: Todos los días a las 7:00 AM
- **Función**: Busca presentaciones no procesadas de los últimos 7 días
- **Propósito**: Monitoreo y alertas sobre presentaciones pendientes
- **Logging**: Registra detalles de presentaciones no procesadas para análisis

## Base de Datos

### Tabla: inbox_presentations

```sql
CREATE TABLE inbox_presentations (
    id UUID PRIMARY KEY,
    form_id UUID NOT NULL UNIQUE,
    received_at TIMESTAMP NOT NULL,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    processed_at TIMESTAMP,
    INDEX idx_inbox_processed_received (processed, received_at),
    INDEX idx_inbox_form_id (form_id)
);
```

## Configuración

Las configuraciones se encuentran en `application.properties`:

```properties
# Incoming channel configuration for form-created events
mp.messaging.incoming.form-created-in.connector=smallrye-rabbitmq
mp.messaging.incoming.form-created-in.exchange.name=form-events
mp.messaging.incoming.form-created-in.exchange.type=topic
mp.messaging.incoming.form-created-in.exchange.durable=true
mp.messaging.incoming.form-created-in.routing-keys=form.created
mp.messaging.incoming.form-created-in.queue.name=form-presentation-receiver-queue
mp.messaging.incoming.form-created-in.queue.durable=true
```

## Flujo de Procesamiento

1. **Recepción del Evento**:
   - `FormCreatedEventConsumer` recibe el mensaje de RabbitMQ
   - Extrae el `formId` del JSON
   - Invoca `ReceiveFormCreatedCommand.execute(formId)`

2. **Almacenamiento en Inbox**:
   - `ReceiveFormCreatedUseCase` verifica si el `formId` ya existe
   - Si no existe, crea un `PresentationId` y lo guarda en el inbox
   - Marca como `processed = false`

3. **Procesamiento Asíncrono**:
   - `InboxProcessor` se ejecuta cada 10 segundos
   - Obtiene hasta 10 presentaciones no procesadas
   - Para cada una, invoca `ProcessPresentationCommand.execute(presentationId)`
   - Marca como procesada al completar exitosamente

4. **Monitoreo Diario**:
   - `UnprocessedPresentationsFetcher` se ejecuta a las 7:00 AM
   - Busca presentaciones no procesadas de los últimos 7 días
   - Registra advertencias para monitoreo y alertas

## Extensibilidad

Para agregar lógica de negocio personalizada, modifica el método `execute` en `ProcessPresentationUseCase.java`:

```java
@Override
@Transactional
public void execute(UUID presentationId) {
    log.info("Processing presentation with ID: {}", presentationId);
    
    // TODO: Agregar tu lógica de negocio aquí
    // Ejemplos:
    // - Llamar a servicios externos
    // - Transformar datos
    // - Enviar notificaciones
    // - Generar reportes
    
    // Marcar como procesado
    inboxRepository.markAsProcessed(presentationId);
}
```

## Ventajas del Diseño

1. **Desacoplamiento**: El consumidor y el procesador están desacoplados
2. **Resiliencia**: Los eventos no se pierden si el procesamiento falla
3. **Idempotencia**: Los eventos duplicados se ignoran automáticamente
4. **Escalabilidad**: El procesamiento puede ajustarse independientemente
5. **Monitoreo**: Tarea diaria para detectar problemas de procesamiento
6. **Testabilidad**: Arquitectura hexagonal facilita las pruebas unitarias
7. **Mantenibilidad**: Separación clara de responsabilidades

## Dependencias

- Quarkus 3.6.4
- Hibernate ORM with Panache
- SmallRye Reactive Messaging RabbitMQ
- Quarkus Scheduler
- H2 Database
- Jackson (para parsing JSON)

## Ejecución

El microservicio se ejecuta automáticamente junto con la aplicación principal:

```bash
./mvnw quarkus:dev
```

## Logs

Los logs proporcionan visibilidad completa del flujo:

```
[FormCreatedEventConsumer] Received form-created event: {"formId": "..."}
[ReceiveFormCreatedUseCase] Receiving form created event for formId: ...
[ReceiveFormCreatedUseCase] FormId ... saved to inbox successfully
[InboxProcessor] Processing 3 unprocessed presentations from inbox
[ProcessPresentationUseCase] Processing presentation with ID: ...
[ProcessPresentationUseCase] Presentation ... processed successfully
[UnprocessedPresentationsFetcher] Found 0 unprocessed presentations from the last 7 days
```
