# Diagramas de Secuencia - Form Platform

Este directorio contiene los diagramas de secuencia PlantUML para todos los adaptadores de entrada del sistema Form Platform.

## Índice de Diagramas

### 1. REST API - Submit Form
**Archivo:** `01-rest-submit-form.puml`

**Descripción:** Flujo completo de envío de formulario a través de la API REST.

**Adaptador de entrada:** `FormResource` (REST Controller)

**Flujo:**
- Cliente HTTP → FormResource → SubmitFormUseCase → FormRepository → H2 Database
- Publicación de evento en Outbox para envío asíncrono a RabbitMQ

**Capas:**
- 🔵 **Infraestructura** (azul claro): FormResource, H2FormRepository, OutboxEventPublisher
- 🟡 **Aplicación** (amarillo): SubmitFormUseCase, FormRepository, EventPublisher
- 🔴 **Dominio** (rosa): Form (entidad de dominio)

---

### 2. CLI - Submit Form
**Archivo:** `02-cli-submit-form.puml`

**Descripción:** Flujo de envío de formulario a través del cliente CLI.

**Adaptador de entrada:** `FormCliClient` (CLI)

**Flujo:**
- Usuario → FormCliClient → HttpClient → FormResource → SubmitFormUseCase → ...
- El CLI actúa como cliente HTTP que consume la API REST

**Capas:**
- 🔵 **Infraestructura** (azul claro): FormCliClient, HttpClient, FormResource, H2FormRepository, OutboxEventPublisher
- 🟡 **Aplicación** (amarillo): SubmitFormUseCase, FormRepository, EventPublisher
- 🔴 **Dominio** (rosa): Form (entidad de dominio)

---

### 3. RabbitMQ Event Consumer - Form Created
**Archivo:** `03-rabbitmq-event-consumer.puml`

**Descripción:** Comunicación entre microservicios a través de RabbitMQ. Muestra cómo el evento `form.created` se publica desde FormPlatform y se consume en FormPresentationReceiver.

**Adaptador de entrada:** `FormCreatedEventConsumer` (RabbitMQ Listener)

**Flujo:**
- **FormPlatform:** OutboxProcessor → RabbitMQEventPublisher → RabbitMQ
- **RabbitMQ:** Queue `form-presentation-receiver-queue` con binding `form.created`
- **FormPresentationReceiver:** RabbitMQ → FormCreatedEventConsumer → ReceiveFormCreatedUseCase → InboxRepository → H2 Database

**Características:**
- Patrón Outbox en FormPlatform para garantizar entrega de eventos
- Patrón Inbox en FormPresentationReceiver para prevenir duplicados
- Constraint UNIQUE en `form_id` para idempotencia

**Capas:**
- 🔵 **Infraestructura** (azul claro): OutboxProcessor, RabbitMQEventPublisher, FormCreatedEventConsumer, H2InboxRepository
- 🟡 **Aplicación** (amarillo): ReceiveFormCreatedUseCase, InboxRepository
- 🔴 **Dominio** (rosa): PresentationId (entidad de dominio)
- 🟠 **RabbitMQ** (naranja): Exchange y Queue

---

### 4. Scheduler - Inbox Processor
**Archivo:** `04-scheduler-inbox-processor.puml`

**Descripción:** Procesamiento asíncrono de presentaciones desde el inbox mediante un scheduler. Incluye el mecanismo de prevención de procesamiento paralelo con máquina de estados de 3 estados.

**Adaptador de entrada:** `InboxProcessor` (Scheduler @10s)

**Flujo:**
- Scheduler ejecuta cada 10 segundos
- Consulta registros con `status = 'PENDING'`
- Para cada registro:
  1. Intenta marcar como `DOING` (atómico)
  2. Si tiene éxito (rowsUpdated=1): procesa y marca como `DONE`
  3. Si falla (rowsUpdated=0): otra instancia ya lo está procesando → SKIP
  4. Si hay error: revierte a `PENDING` para reintentar

**Máquina de Estados:**
```
PENDING → DOING → DONE
   ↑         |
   +---------+ (on error)
```

**Prevención de Procesamiento Paralelo:**
- `tryMarkAsProcessing()` es una operación atómica
- Solo una instancia puede transicionar de PENDING a DOING
- Otras instancias reciben `rowsUpdated=0` y omiten el procesamiento

**Capas:**
- 🔵 **Infraestructura** (azul claro): InboxProcessor, H2InboxRepository
- 🟡 **Aplicación** (amarillo): ProcessPresentationCommand, ProcessPresentationUseCase, InboxRepository

---

## Convenciones de Color

Los diagramas utilizan colores consistentes para identificar las capas de la arquitectura hexagonal:

- 🔵 **#E3F2FD (Azul claro)** - Capa de Infraestructura
  - Adaptadores de entrada (REST, CLI, RabbitMQ, Scheduler)
  - Adaptadores de salida (Repositorios H2, Publishers)
  - Tecnologías específicas (HTTP, JPA, RabbitMQ)

- 🟡 **#FFF9C4 (Amarillo)** - Capa de Aplicación
  - Use Cases
  - Puertos (interfaces)
  - Comandos

- 🔴 **#F8BBD0 (Rosa)** - Capa de Dominio
  - Entidades de dominio
  - Value Objects
  - Lógica de negocio

- 🟠 **#FFE0B2 (Naranja)** - Componentes Externos
  - RabbitMQ
  - Sistemas externos

- 💾 **Cilindro** - Bases de datos
  - H2 Database (forms, outbox_events, inbox_presentations)

---

## Cómo Visualizar los Diagramas

### Opción 1: PlantUML Online
1. Visita [PlantUML Online Server](http://www.plantuml.com/plantuml/uml/)
2. Copia y pega el contenido del archivo `.puml`
3. Visualiza el diagrama generado

### Opción 2: VS Code con extensión PlantUML
1. Instala la extensión "PlantUML" en VS Code
2. Abre el archivo `.puml`
3. Presiona `Alt+D` para previsualizar

### Opción 3: IntelliJ IDEA con plugin PlantUML
1. Instala el plugin "PlantUML integration"
2. Abre el archivo `.puml`
3. El diagrama se renderiza automáticamente

### Opción 4: Generar imágenes PNG
```bash
# Instalar PlantUML (requiere Java)
brew install plantuml  # macOS
apt-get install plantuml  # Linux

# Generar PNG
plantuml diagrams/*.puml

# Generar SVG
plantuml -tsvg diagrams/*.puml
```

---

## Patrones Arquitectónicos Implementados

### 1. Arquitectura Hexagonal (Ports & Adapters)
- Separación clara entre capas de infraestructura, aplicación y dominio
- Puertos (interfaces) definen contratos
- Adaptadores implementan los puertos

### 2. Patrón Outbox (FormPlatform)
- Eventos se guardan en tabla `outbox_events` en la misma transacción que el formulario
- OutboxProcessor envía eventos a RabbitMQ de forma asíncrona
- Garantiza entrega de eventos incluso si RabbitMQ está caído

### 3. Patrón Inbox (FormPresentationReceiver)
- Eventos recibidos se guardan en tabla `inbox_presentations`
- Constraint UNIQUE en `form_id` previene duplicados
- InboxProcessor procesa eventos de forma asíncrona

### 4. Máquina de Estados (Inbox Processing)
- Estados: PENDING → DOING → DONE
- Prevención de procesamiento paralelo mediante actualización atómica
- Manejo de errores con reversión a PENDING

### 5. CQRS (Command Query Responsibility Segregation)
- Comandos: SubmitFormCommand, ReceiveFormCreatedCommand, ProcessPresentationCommand
- Separación clara entre escritura y lectura

---

## Documentación Relacionada

- **PARALLEL_PROCESSING_PREVENTION.md** - Explicación detallada del mecanismo de prevención de procesamiento paralelo
- **README.md** (raíz) - Documentación general del proyecto
- **FORM_PRESENTATION_RECEIVER_README.md** - Documentación específica del microservicio FormPresentationReceiver

---

## Notas Técnicas

### Transaccionalidad
Todos los Use Cases están anotados con `@Transactional` para garantizar consistencia de datos.

### Idempotencia
- FormPresentationReceiver verifica duplicados con `existsByFormId()` antes de insertar
- Constraint UNIQUE en `form_id` como segunda línea de defensa

### Escalabilidad
- Ambos microservicios pueden ejecutarse en múltiples instancias
- El mecanismo de estados PENDING/DOING/DONE previene procesamiento duplicado
- RabbitMQ actúa como buffer entre microservicios

---

**Última actualización:** 18/02/2026
