# FormPlatform Microservice

Microservicio desarrollado con Quarkus que implementa una API REST para recibir y procesar formularios. Utiliza arquitectura hexagonal (puertos y adaptadores) para mantener el dominio desacoplado de la infraestructura.

## 🏗️ Arquitectura

El proyecto sigue los principios de **Arquitectura Hexagonal**:

```
formPlatform/
├── domain/                    # Capa de Dominio (núcleo)
│   ├── model/                # Entidades de dominio
│   │   └── Form.java
│   └── port/                 # Puertos (interfaces)
│       ├── FormRepository.java
│       └── EventPublisher.java
├── application/              # Capa de Aplicación
│   └── usecase/             # Casos de uso
│       └── SubmitFormUseCase.java
└── infrastructure/          # Capa de Infraestructura
    └── adapter/            # Adaptadores
        ├── persistence/    # Adaptador de persistencia (H2)
        │   ├── FormEntity.java
        │   └── H2FormRepository.java
        ├── messaging/      # Adaptador de mensajería (RabbitMQ)
        │   └── RabbitMQEventPublisher.java
        └── rest/          # Adaptador REST
            └── FormResource.java
```

### Capas:

1. **Dominio**: Contiene la lógica de negocio pura y las interfaces (puertos)
2. **Aplicación**: Orquesta los casos de uso utilizando los puertos del dominio
3. **Infraestructura**: Implementa los adaptadores que conectan con tecnologías específicas

## 🚀 Tecnologías

- **Quarkus 3.6.4**: Framework Java nativo en la nube
- **H2 Database**: Base de datos embebida (persistencia en disco)
- **RabbitMQ**: Sistema de mensajería para eventos
- **Hibernate ORM with Panache**: ORM simplificado
- **RESTEasy Reactive**: API REST reactiva
- **Jackson**: Serialización/deserialización JSON
- **Maven**: Gestión de dependencias

## 📋 Requisitos Previos

- Java 17 o superior
- Maven 3.8+
- RabbitMQ instalado y ejecutándose (puerto 5672)

### Instalar RabbitMQ

**macOS:**
```bash
brew install rabbitmq
brew services start rabbitmq
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt-get install rabbitmq-server
sudo systemctl start rabbitmq-server
```

**Docker:**
```bash
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management
```

## 🔧 Configuración

La configuración se encuentra en `src/main/resources/application.properties`:

```properties
# Puerto HTTP
quarkus.http.port=8080

# Base de datos H2 (archivo en disco)
quarkus.datasource.jdbc.url=jdbc:h2:file:./data/formplatform

# RabbitMQ
rabbitmq-host=localhost
rabbitmq-port=5672
```

## 🏃 Ejecución

### Modo Desarrollo (con hot reload)
```bash
cd formPlatform
./mvnw quarkus:dev
```

### Compilar y ejecutar
```bash
./mvnw clean package
java -jar target/quarkus-app/quarkus-run.jar
```

## 📡 API REST

### Endpoint: Enviar Formulario

**POST** `/api/forms`

**Request Body:**
```json
{
  "nombre": "Juan Pérez",
  "email": "juan@example.com",
  "edad": 30,
  "comentarios": "Este es un formulario de prueba"
}
```

**Response (201 Created):**
```json
{
  "id": 1,
  "message": "Form submitted successfully"
}
```

### Endpoint: Health Check

**GET** `/api/forms/health`

**Response:**
```json
{
  "status": "UP"
}
```

## 🧪 Pruebas con cURL

```bash
# Enviar un formulario
curl -X POST http://localhost:8080/api/forms \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "María García",
    "email": "maria@example.com",
    "telefono": "123456789",
    "mensaje": "Solicitud de información"
  }'

# Health check
curl http://localhost:8080/api/forms/health
```

## 📨 Eventos RabbitMQ

Cuando se envía un formulario, se publica un evento en RabbitMQ:

- **Exchange**: `form-events`
- **Routing Key**: `form.created`
- **Mensaje**:
```json
{
  "formId": 1,
  "event": "FORM_CREATED"
}
```

### Consumir eventos (ejemplo)

Puedes verificar los eventos en la consola de administración de RabbitMQ:
- URL: http://localhost:15672
- Usuario: `guest`
- Contraseña: `guest`

## 💾 Base de Datos

Los datos se almacenan en H2 en el directorio `./data/formplatform.mv.db`

Para acceder a la consola H2 en modo desarrollo:
```
http://localhost:8080/q/h2-console
```

**Credenciales:**
- JDBC URL: `jdbc:h2:file:./data/formplatform`
- Usuario: `sa`
- Contraseña: (vacío)

## 🏛️ Principios de Arquitectura Hexagonal

### Puertos (Interfaces)
- `FormRepository`: Puerto para operaciones de persistencia
- `EventPublisher`: Puerto para publicación de eventos

### Adaptadores
- `H2FormRepository`: Implementa `FormRepository` usando H2
- `RabbitMQEventPublisher`: Implementa `EventPublisher` usando RabbitMQ
- `FormResource`: Adaptador REST que expone la API

### Ventajas
✅ **Testabilidad**: Fácil crear mocks de los puertos  
✅ **Flexibilidad**: Cambiar tecnologías sin afectar el dominio  
✅ **Mantenibilidad**: Separación clara de responsabilidades  
✅ **Independencia**: El dominio no depende de frameworks

## 📦 Estructura de Paquetes

```
com.formplatform
├── domain
│   ├── model          # Entidades de dominio
│   └── port           # Interfaces (puertos)
├── application
│   └── usecase        # Lógica de aplicación
└── infrastructure
    └── adapter
        ├── persistence  # Adaptador de BD
        ├── messaging    # Adaptador de eventos
        └── rest         # Adaptador HTTP
```

## 🔍 Logs

Los logs muestran:
- Consultas SQL ejecutadas
- Eventos publicados en RabbitMQ
- Peticiones HTTP recibidas

## 🛠️ Desarrollo

Para añadir nuevas funcionalidades:

1. **Definir el puerto** en `domain/port/`
2. **Crear el caso de uso** en `application/usecase/`
3. **Implementar el adaptador** en `infrastructure/adapter/`

## 📄 Licencia

Este proyecto es un ejemplo educativo de arquitectura hexagonal con Quarkus.
