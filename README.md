# FormPlatform – Microservicios

Proyecto **multi-módulo Maven** con microservicios independientes desarrollados en Quarkus. Cada microservicio tiene su propio código Java y su propia configuración (`application.properties`), sin compartir clases ni configuración entre ellos.

## Estructura del repositorio

```
formPlatform/
├── pom.xml                          # POM padre (solo módulos y dependencyManagement)
├── formplatform/                    # Microservicio: API de formularios + eventos
│   ├── pom.xml
│   ├── src/main/java/com/formplatform/...
│   └── src/main/resources/application.properties
├── formpresentationreceiver/         # Microservicio: consumidor form-created, patrón Inbox
│   ├── pom.xml
│   ├── src/main/java/com/formpresentationreceiver/...
│   └── src/main/resources/application.properties
├── formcli.sh                       # CLI para el API (usa módulo formplatform)
├── README.md                        # Este archivo
├── FORM_PRESENTATION_RECEIVER_README.md
└── CLI_README.md
```

## Microservicios

| Módulo | Puerto | Descripción |
|--------|--------|-------------|
| **formplatform** | 8080 | API REST de formularios, outbox y publicación a RabbitMQ |
| **formpresentationreceiver** | 8081 | Consumidor de eventos `form.created`, patrón Inbox |

Cada uno tiene:
- **Configuración propia**: `application.properties` dentro de su módulo (nombre, puerto, BD, canales RabbitMQ).
- **Código propio**: solo clases de su paquete (`com.formplatform.*` o `com.formpresentationreceiver.*`).
- **Dependencias propias**: en su `pom.xml` (por ejemplo, formpresentationreceiver no usa REST ni Picocli).

## Requisitos

- Java 17+
- Maven 3.8+
- RabbitMQ (puerto 5672)

## Build

Desde la raíz del proyecto:

```bash
# Compilar todos los módulos
mvn clean compile

# Compilar solo un microservicio
mvn -pl formplatform clean compile
mvn -pl formpresentationreceiver clean compile
```

## Ejecución

Cada microservicio se ejecuta por separado desde su módulo:

### FormPlatform (API de formularios)

```bash
cd formplatform
mvn quarkus:dev
```

- API: http://localhost:8080  
- Configuración: `formplatform/src/main/resources/application.properties`  
- Base de datos H2: `./data/formplatform` (relativa al directorio del módulo)

### FormPresentationReceiver (consumidor de eventos)

```bash
cd formpresentationreceiver
mvn quarkus:dev
```

- Puerto: 8081 (por defecto)  
- Configuración: `formpresentationreceiver/src/main/resources/application.properties`  
- Base de datos H2: `./data/formpresentationreceiver`

## Configuración por microservicio

- **formplatform**: `formplatform/src/main/resources/application.properties`  
  - `quarkus.application.name=formplatform`  
  - `quarkus.http.port=8080`  
  - `quarkus.datasource.jdbc.url=jdbc:h2:file:./data/formplatform;...`  
  - Canales **outgoing** RabbitMQ (`form-created`)

- **formpresentationreceiver**: `formpresentationreceiver/src/main/resources/application.properties`  
  - `quarkus.application.name=formpresentationreceiver`  
  - `quarkus.http.port=8081`  
  - `quarkus.datasource.jdbc.url=jdbc:h2:file:./data/formpresentationreceiver;...`  
  - Canales **incoming** RabbitMQ (`form-created-in`)

No hay `application.properties` compartido en la raíz.

## Añadir un nuevo microservicio

1. Crear carpeta del módulo, por ejemplo `mimicroservicio/`.
2. Añadir `mimicroservicio/pom.xml` con `<parent>` apuntando al POM raíz y solo las dependencias Quarkus que necesite.
3. Añadir en el `pom.xml` raíz:
   ```xml
   <modules>
     <module>formplatform</module>
     <module>formpresentationreceiver</module>
     <module>mimicroservicio</module>
   </modules>
   ```
4. Crear `mimicroservicio/src/main/java/com/mimicroservicio/...` y `mimicroservicio/src/main/resources/application.properties` con nombre, puerto y BD propios.

Así cada microservicio sigue siendo independiente en código y configuración.

## CLI (Form Platform)

El script `formcli.sh` compila y ejecuta el CLI del módulo **formplatform**:

```bash
./formcli.sh submit --field nombre=test --field email=test@example.com
./formcli.sh health -u http://localhost:8080
```

Ver `CLI_README.md` para más opciones.

## Documentación adicional

- **FormPresentationReceiver**: ver `FORM_PRESENTATION_RECEIVER_README.md`.
- **CLI**: ver `CLI_README.md`.

## Arquitectura por microservicio

Ambos siguen **arquitectura hexagonal** (dominio, aplicación, infraestructura). El dominio y los casos de uso están en su propio paquete; la infraestructura (REST, RabbitMQ, H2, schedulers) en adaptadores. No se comparten clases entre formplatform y formpresentationreceiver.
