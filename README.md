# Microservicio de productos con MongoDB

HU-01 · Práctica individual de Edwin Ledezma. La API usa Spring Boot 3.5, Java 17 y MongoDB. El SKU es único y MongoDB almacena el precio como `Decimal128`.

## Arranque local

Requiere Java 17 o superior, Maven 3.6.3 o superior y MongoDB. Con Docker Compose, inicia la base y luego la aplicación:

```bash
docker compose up -d mongodb
# Define las credenciales como se indica abajo antes de ejecutar Maven.
mvn spring-boot:run "-Dspring-boot.run.arguments=--server.address=127.0.0.1"
```

También puedes usar una instancia local de MongoDB 8.0. Define `MONGODB_URI` para cambiar la cadena de conexión. El valor predeterminado apunta a `mongodb://127.0.0.1:27017/productos`. Para el laboratorio, Compose publica los puertos solo en la interfaz local.

Swagger UI está en [localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html).

| Método y ruta | Resultado |
| --- | --- |
| `POST /api/productos` | `201 Created` con los datos, el ID y el encabezado `Location`. |
| `GET /api/productos` | `200 OK` con el listado. |
| `GET /api/productos/{id}` | `200 OK` con el producto. |
| `PUT /api/productos/{id}` | `200 OK` con los campos reemplazados y el mismo ID. |
| `DELETE /api/productos/{id}` | `204 No Content`. |

POST y PUT reciben el mismo cuerpo. El SKU admite de 1 a 32 caracteres en mayúsculas, dígitos y guiones, y debe empezar con una letra o un dígito. El nombre es obligatorio y admite hasta 120 caracteres. El precio debe ser positivo y admite ocho dígitos enteros y dos decimales.

```json
{"sku":"MON-01","nombre":"Monitor","precio":149.99}
```

Los errores usan `application/problem+json`. El código `400` corresponde a JSON o campos inválidos, `404` a un producto inexistente, `409` a un SKU duplicado y `503` a un fallo de acceso a la base de datos.

## Autenticación y permisos

Antes de iniciar la aplicación, configura contraseñas distintas para `lector` y `editor`. Cada una debe tener al menos 16 caracteres y como máximo 72 bytes en UTF-8. En PowerShell 7 puedes introducirlas sin mostrarlas en consola:

```powershell
$env:APP_AUTH_LECTOR_PASSWORD = Read-Host "Contraseña de lector" -MaskInput
$env:APP_AUTH_EDITOR_PASSWORD = Read-Host "Contraseña de editor" -MaskInput
```

El servidor verifica las contraseñas con BCrypt. En Swagger ejecuta `POST /api/auth/login` con los campos `usuario` y `password`. Copia el `access_token` a **Authorize**. Para solicitudes fuera de Swagger, usa el encabezado `Authorization: Bearer <token>`.

| Cuenta | Permisos |
| --- | --- |
| `lector` | GET en rutas de productos. |
| `editor` | GET, POST, PUT y DELETE en rutas de productos. |

La API devuelve `401` si faltan credenciales o el token es inválido o venció. Devuelve `403` si el JWT es válido y el rol carece del permiso requerido. El token vence en 15 minutos. Para obtener uno nuevo, repite el login. La clave de firma se genera al iniciar el proceso, por lo que el reinicio invalida los tokens anteriores. Usa HTTPS fuera del laboratorio local y evita almacenar contraseñas o JWT en el repositorio.

## Pruebas y cobertura

```bash
mvn test
mvn clean verify
```

`mvn test` ejecuta las pruebas unitarias con Mockito y las pruebas del filtro HTTP de autenticación con MockMvc. `mvn clean verify` ejecuta además las pruebas HTTP contra una instancia real de MongoDB y exige al menos 80 % de líneas y ramas con JaCoCo. El reporte HTML se genera en `target/site/jacoco/index.html`.

Las pruebas de integración usan una base temporal con un nombre aleatorio y la eliminan al finalizar. Usa `MONGODB_TEST_URI` para indicar la instancia de pruebas. El URI predeterminado apunta a localhost y la cuenta necesita permisos para crear y eliminar la base temporal.

## Análisis estático

```bash
docker compose --profile quality up -d sonarqube
```

En [localhost:9000](http://localhost:9000), completa la configuración inicial de SonarQube y genera un token con permiso de análisis. Configura `SONAR_TOKEN` en tu entorno. Con MongoDB y SonarQube activos, ejecuta:

```bash
mvn clean verify sonar:sonar "-Dsonar.host.url=http://localhost:9000" "-Dsonar.qualitygate.wait=true"
```

El escáner importa la cobertura de JaCoCo y Maven espera el resultado del Quality Gate. Consulta el proyecto `pruebas-unitarias` en SonarQube para revisar los hallazgos.

Para ejecutar el control local con SpotBugs, Find Security Bugs y PMD:

```bash
mvn -Pquality-local clean verify
```

El perfil falla si los analizadores reportan hallazgos. El filtro de SpotBugs se limita al inventario de rutas del controlador y a la inyección por constructor de Spring. Maven genera `target/spotbugsXml.xml` y `target/reports/pmd.html`.

## Historial y evidencias

Los cambios entran por PR desde `feature/*` hacia `dev` y se promueven mediante un segundo PR a `master`. Los mensajes siguen el formato `Add<backend>:`, `Change<backend>:`, `Fix<backend>:` o `Update<backend>:` para cambios del backend..

El [reporte técnico de la HU-01](evidencia/HU-01-reporte-tecnico.pdf) incluye capturas de Swagger, Maven, JaCoCo y el análisis estático local.

La [captura de la práctica anterior](evidencia/mvn-test.jpg) corresponde a la versión inicial con H2.

## Despliegue en Render y MongoDB Atlas

El Dockerfile compila el proyecto con Maven y ejecuta las pruebas unitarias y de filtros HTTP. La imagen final contiene Java 17 y el JAR, y ejecuta la API con un usuario sin privilegios de root.

Render construye la imagen desde la rama master y proporciona un subdominio onrender.com con HTTPS administrado. El archivo render.yaml describe el servicio Free en Oregon, el puerto 10000 y la comprobación de salud en /v3/api-docs. El despliegue automático está desactivado para publicar versiones verificadas mediante el panel.

Configura estas variables en el servicio de Render:

| Variable | Configuración |
| --- | --- |
| SERVER_PORT | 10000 |
| SERVER_FORWARD_HEADERS_STRATEGY | framework |
| JAVA_TOOL_OPTIONS | -XX:MaxRAMPercentage=50.0 |
| MONGODB_URI | URI de Atlas con la base productos y conexión TLS mongodb+srv. |
| APP_AUTH_LECTOR_PASSWORD | Contraseña exclusiva de al menos 16 caracteres y hasta 72 bytes UTF-8. |
| APP_AUTH_EDITOR_PASSWORD | Otra contraseña exclusiva con los mismos límites. |

En Atlas, crea un usuario con readWrite solo sobre productos y autoriza los rangos de salida que Render muestra en Connect → Outbound. Las credenciales se guardan en la configuración de Render.

La base persiste en Atlas. El sistema de archivos del contenedor de Render es efímero. El plan gratuito suspende la API tras un período de inactividad y puede tardar en reactivarla. Cada reinicio cambia la clave de firma del laboratorio, por lo que los clientes deben volver a iniciar sesión.

Para comprobar el despliegue, abre https://TU-SERVICIO.onrender.com/swagger-ui/index.html, ejecuta el login y comprueba el CRUD con editor. Una consulta protegida sin token debe devolver 401. Revisa también el historial de despliegues de Render y el certificado HTTPS del dominio.
