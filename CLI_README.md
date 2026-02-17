# Form Platform CLI Client

Cliente de línea de comandos para interactuar con el API REST de Form Platform.

## Características

- ✅ Envío de formularios mediante línea de comandos
- ✅ Verificación del estado de salud del API
- ✅ Soporte para datos JSON o campos individuales
- ✅ Modo verbose para debugging
- ✅ Configuración flexible de URL del API

## Requisitos

- Java 17 o superior
- Maven 3.6 o superior
- API de Form Platform ejecutándose (por defecto en http://localhost:8080)

## Instalación

1. Asegúrate de que el proyecto esté compilado:
```bash
mvn clean compile
```

2. El script `formcli.sh` está listo para usar y tiene permisos de ejecución.

## Uso

### Ayuda General

```bash
./formcli.sh --help
```

### Verificar Estado del API

Verifica que el API esté funcionando correctamente:

```bash
./formcli.sh health
```

Con modo verbose:
```bash
./formcli.sh health -v
```

Con URL personalizada:
```bash
./formcli.sh health -u http://localhost:9090
```

### Enviar Formulario

#### Opción 1: Usando JSON directo

```bash
./formcli.sh submit -d '{"name":"Juan Pérez","email":"juan@example.com","message":"Hola mundo"}'
```

#### Opción 2: Usando campos individuales

```bash
./formcli.sh submit -f name="Juan Pérez" -f email="juan@example.com" -f message="Hola mundo"
```

#### Con modo verbose

```bash
./formcli.sh submit -d '{"name":"Juan","email":"juan@example.com"}' -v
```

#### Con URL personalizada

```bash
./formcli.sh submit -u http://localhost:9090 -d '{"name":"Juan","email":"juan@example.com"}'
```

## Ejemplos Completos

### Ejemplo 1: Envío simple de formulario

```bash
./formcli.sh submit -d '{"firstName":"María","lastName":"García","email":"maria@example.com","phone":"123456789"}'
```

Salida esperada:
```
✓ Form submitted successfully!
Form ID: 550e8400-e29b-41d4-a716-446655440000
Message: Form submitted successfully
```

### Ejemplo 2: Verificar salud del API

```bash
./formcli.sh health
```

Salida esperada:
```
✓ API is healthy
Response: {"status":"UP"}
```

### Ejemplo 3: Envío con campos individuales

```bash
./formcli.sh submit \
  -f name="Pedro López" \
  -f email="pedro@example.com" \
  -f company="Tech Corp" \
  -f message="Solicitud de información"
```

### Ejemplo 4: Debugging con modo verbose

```bash
./formcli.sh submit -v -d '{"test":"data"}'
```

Salida esperada:
```
Submitting form to: http://localhost:8080/api/forms
Data: {"test":"data"}
Response status: 201
✓ Form submitted successfully!
Form ID: 550e8400-e29b-41d4-a716-446655440000
Message: Form submitted successfully
```

## Opciones Disponibles

### Comando `submit`

| Opción | Descripción | Requerido |
|--------|-------------|-----------|
| `-d, --data` | Datos del formulario en formato JSON | Sí* |
| `-f, --field` | Agregar un campo (formato: key=value). Puede usarse múltiples veces | Sí* |
| `-u, --url` | URL base del API (default: http://localhost:8080) | No |
| `-v, --verbose` | Habilitar salida detallada | No |
| `-h, --help` | Mostrar ayuda | No |

*Nota: Se debe proporcionar `-d` o al menos un `-f`

### Comando `health`

| Opción | Descripción | Requerido |
|--------|-------------|-----------|
| `-u, --url` | URL base del API (default: http://localhost:8080) | No |
| `-v, --verbose` | Habilitar salida detallada | No |
| `-h, --help` | Mostrar ayuda | No |

## Códigos de Salida

- `0`: Operación exitosa
- `1`: Error (conexión fallida, datos inválidos, etc.)

## Solución de Problemas

### Error: "Maven is not installed or not in PATH"

Asegúrate de tener Maven instalado y en tu PATH:
```bash
mvn --version
```

### Error: "Error connecting to API"

1. Verifica que el API esté ejecutándose:
```bash
curl http://localhost:8080/api/forms/health
```

2. Si el API está en otro puerto, usa la opción `-u`:
```bash
./formcli.sh health -u http://localhost:PUERTO
```

### Error: "Compilation failed"

Compila manualmente el proyecto:
```bash
mvn clean compile
```

### Error de permisos en formcli.sh

Dale permisos de ejecución:
```bash
chmod +x formcli.sh
```

## Integración con Scripts

El cliente CLI puede ser integrado fácilmente en scripts de automatización:

```bash
#!/bin/bash

# Script de ejemplo para enviar múltiples formularios

for i in {1..5}; do
  ./formcli.sh submit -d "{\"name\":\"User $i\",\"email\":\"user$i@example.com\"}"
  if [ $? -eq 0 ]; then
    echo "Formulario $i enviado exitosamente"
  else
    echo "Error enviando formulario $i"
  fi
done
```

## Desarrollo

### Estructura del Código

El cliente CLI está implementado en:
```
src/main/java/com/formplatform/infrastructure/adapter/input/cli/FormCliClient.java
```

### Agregar Nuevos Comandos

Para agregar un nuevo comando, crea una clase interna que implemente `Callable<Integer>` y anótala con `@Command`:

```java
@Command(name = "micomando", description = "Descripción del comando")
static class MiComando implements Callable<Integer> {
    @Override
    public Integer call() {
        // Implementación
        return 0;
    }
}
```

Luego agrégalo en el método `main`:

```java
new CommandLine(new FormCliClient())
    .addSubcommand("submit", new SubmitCommand())
    .addSubcommand("health", new HealthCommand())
    .addSubcommand("micomando", new MiComando())
    .execute(args);
```

## Licencia

Este cliente CLI es parte del proyecto Form Platform.
