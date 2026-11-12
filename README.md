# Spring PetClinic — Modular REST API (Java 21)

Modernización de **Spring PetClinic** hacia una arquitectura de **Monolito Modular** con **Java 21 LTS** y **API RESTful JSON (`/api/v1/...`)**. La solución reemplaza la capa legacy acoplada a vistas SSR (Thymeleaf) por contratos externos inmutables basados en Java 21 `record`, validaciones declarativas (Jakarta Bean Validation), manejo centralizado de errores bajo el estándar **RFC 7807 (`ProblemDetail`)** y documentación interactiva **OpenAPI 3 / Swagger UI**, preservando íntegramente las reglas de negocio, validaciones y casos límite del sistema original.

---

## 1. Documentación Obligatoria (`docs/`)

Toda la documentación de ingeniería exigida en la prueba técnica se encuentra en la carpeta [`docs/`](docs/):

| Documento / Recurso | Contenido |
| :--- | :--- |
| **[`docs/01-diagnostico-legacy.md`](docs/01-diagnostico-legacy.md)** | Hallazgos técnicos y funcionales del proyecto original, inventario de dependencias, deuda técnica e invariantes de negocio. |
| **[`docs/02-arquitectura.md`](docs/02-arquitectura.md)** | Diagramas Mermaid (Componentes, Secuencia y Entidad-Relación), límites de módulos, justificación de Monolito Modular vs. Microservicios y Decisiones Arquitectónicas (ADRs). |
| **[`docs/03-plan-migracion.md`](docs/03-plan-migracion.md)** | Alcance migrado, secuencia incremental, análisis de riesgos y matriz de correspondencia original (MVC/Thymeleaf) vs. nuevo (`/api/v1`). |
| **[`docs/04-uso-ia.md`](docs/04-uso-ia.md)** | Herramientas y modelos utilizados, objetivos por fase, prompts representativos, sugerencias aceptadas/rechazadas, validaciones humanas y errores/limitaciones de IA detectados y corregidos. |
| **[`.env.example`](.env.example)** | Variables de entorno de ejemplo no sensibles para configuración local y perfiles de base de datos. |

---

## 2. Arquitectura Modular y Responsabilidades

El sistema está dividido en tres módulos funcionales de negocio y un núcleo transversal bajo `org.springframework.samples.petclinic`:

- **Módulo 1 — `Owners & Pets` (`org.springframework.samples.petclinic.owner`):**
  - **Responsabilidad:** Gestión de propietarios, registro/actualización de mascotas y consulta del catálogo de tipos de mascota.
  - **Componentes:** `OwnerRestController`, `PetRestController`, `PetTypeRestController`, `OwnerPetService`, `OwnerPetMapper`, `OwnerRepository`, `PetTypeRepository`, DTOs `record` (`OwnerRequest`, `OwnerResponse`, `PetRequest`, `PetResponse`, `PetTypeResponse`).
- **Módulo 2 — `Visits` (`org.springframework.samples.petclinic.visit`):**
  - **Responsabilidad:** Registro y consulta cronológica de visitas clínicas asociadas a mascotas, validando pertenencia `Owner -> Pet` y restricción de fecha futura (`@Future`).
  - **Componentes:** `VisitRestController`, `VisitService`, `VisitMapper`, `VisitRepository`, DTOs `record` (`VisitRequest`, `VisitResponse`).
- **Módulo 3 — `Vets` (`org.springframework.samples.petclinic.vet`):**
  - **Responsabilidad:** Consulta paginada y completa del directorio de veterinarios y sus especialidades respaldada por caché JCache (`"vets"`).
  - **Componentes:** `VetRestController`, `VetService`, `VetMapper`, `VetRepository`, DTOs `record` (`VetResponse`, `SpecialtyResponse`).
- **Módulo Transversal — `System & Shared Kernel` (`model`, `system`):**
  - **Responsabilidad:** Superclases de persistencia (`BaseEntity`, `NamedEntity`, `Person`), contenedor inmutable de paginación `PageResponse<T>`, manejo global de excepciones RFC 7807 (`GlobalExceptionHandler`), configuración de caché (`CacheConfiguration`) y especificación OpenAPI 3 (`OpenApiConfiguration`).

---

## 3. Guía de Puesta en Marcha (Paso a Paso)

### 3.1. Prerrequisitos
- **JDK:** **Java 21** o superior (verificado con OpenJDK 21 / 25).
- **Gestor de Construcción:** **Maven 3.9+** a través del wrapper incluido (`./mvnw` en Linux/macOS o `.\mvnw.cmd` en Windows) o **Gradle** (`./gradlew`). No requiere instalación manual previa de Maven.
- **Motor de Base de Datos:** **H2 In-Memory** incluido por defecto (cero configuración externa necesaria).
- **Docker y Docker Compose (Opcional):** Solo requeridos si se desea levantar motores externos persistentes (**MySQL 9.7** o **PostgreSQL 18.4**) mediante `docker-compose.yml`.

### 3.2. Clonar la solución
```bash
git clone <URL_DEL_REPOSITORIO_PUBLICO_DEL_CANDIDATO>
cd spring-petclinic
```

### 3.3. Configurar el ambiente
1. **Archivos de configuración y ejemplo:**
   - [`.env.example`](.env.example): Plantilla de variables de entorno no sensibles.
   - [`src/main/resources/application.properties`](src/main/resources/application.properties): Configuración principal (perfil por defecto con H2, JPA, Actuator y rutas de OpenAPI).
   - [`src/main/resources/application-mysql.properties`](src/main/resources/application-mysql.properties) y [`src/main/resources/application-postgres.properties`](src/main/resources/application-postgres.properties): Perfiles opcionales para motores relacionales externos.
2. **Variables de entorno y valores permitidos (no sensibles):**
   - `SPRING_PROFILES_ACTIVE`: `default` (H2 en memoria), `mysql`, `postgres`.
   - `SERVER_PORT`: Puerto HTTP de escucha (por defecto `8080`).
   - `MYSQL_URL` / `MYSQL_USER` / `MYSQL_PASS`: Credenciales locales por defecto (`jdbc:mysql://localhost:3306/petclinic`, `petclinic`, `petclinic`).
   - `POSTGRES_URL` / `POSTGRES_USER` / `POSTGRES_PASS`: Credenciales locales por defecto (`jdbc:postgresql://localhost:5432/petclinic`, `petclinic`, `petclinic`).
3. **Motor de Base de Datos, creación del esquema y datos de prueba:**
   - En el perfil por defecto (`database=h2`), Spring Boot ejecuta automáticamente al arranque:
     - Esquema DDL: [`src/main/resources/db/h2/schema.sql`](src/main/resources/db/h2/schema.sql) (incluyendo índices y la restricción `CONSTRAINT unique_owner_pet_name UNIQUE (owner_id, name)`).
     - Datos semilla: [`src/main/resources/db/h2/data.sql`](src/main/resources/db/h2/data.sql) (10 propietarios con IDs `1..10`, 13 mascotas con IDs `1..13`, 6 tipos de mascota con IDs `1..6`, 4 visitas y 6 veterinarios con 3 especialidades).
4. **Puertos de escucha:**
   - API REST / Aplicación principal: **`8080`** (sin dependencias previas en perfil por defecto).
   - MySQL (opcional vía `docker compose up mysql`): **`3306`**.
   - PostgreSQL (opcional vía `docker compose up postgres`): **`5432`**.
5. **Perfiles de Spring disponibles:**
   - `default`: Base de datos H2 en memoria auto-inicializada; ideal para evaluación rápida y pruebas reproducibles.
   - `mysql`: Conexión a MySQL.
   - `postgres`: Conexión a PostgreSQL.

### 3.4. Compilar, ejecutar y verificar

- **Comando real para compilar y aplicar formato de código:**
  ```bash
  ./mvnw clean spring-javaformat:apply compile
  ```
  *(En Windows PowerShell: `.\mvnw.cmd clean spring-javaformat:apply compile`)*

- **Comando real para ejecutar todas las pruebas automatizadas (Unitarias, MVC, JPA, Integración y Concurrencia):**
  ```bash
  ./mvnw test
  ```
  *(En Windows PowerShell: `.\mvnw.cmd test`)*

- **Comando real para iniciar la aplicación:**
  ```bash
  ./mvnw spring-boot:run
  ```
  *(En Windows PowerShell: `.\mvnw.cmd spring-boot:run`)*

- **URL real de Health Check / Verificación de arranque:**
  - `http://localhost:8080/actuator/health` -> Respuesta esperada: `{"status":"UP",...}`

- **URL real de Swagger UI y especificación OpenAPI 3 JSON:**
  - **Swagger UI Interactivo:** `http://localhost:8080/swagger-ui.html`
  - **OpenAPI 3 JSON:** `http://localhost:8080/v3/api-docs`

---

## 4. Pruebas Funcionales Paso a Paso (Casos de Uso y Errores)

Con la aplicación iniciada en `http://localhost:8080` y los datos semilla de `data.sql` cargados automáticamente, puedes verificar cada capacidad migrada mediante `curl`, Postman o Swagger UI:

### 4.1. Consultar propietarios (Paginado y Filtro por Apellido)
- **Petición:**
  ```bash
  curl -i -X GET "http://localhost:8080/api/v1/owners?lastName=Davis&page=0&size=5" -H "Accept: application/json"
  ```
- **Respuesta esperada (`200 OK`):** Objeto `PageResponse<OwnerResponse>` con los 2 propietarios de apellido `"Davis"` (`Betty Davis` y `Harold Davis`), `totalElements: 2`, `totalPages: 1`.

### 4.2. Consultar detalle de un propietario por ID (Flujo exitoso y Error 404)
- **Flujo exitoso (`200 OK`):**
  ```bash
  curl -i -X GET "http://localhost:8080/api/v1/owners/1" -H "Accept: application/json"
  ```
  *Retorna a `George Franklin` (`id: 1`) junto con su mascota `Leo` (`type: cat`).*
- **Caso de error — Propietario inexistente (`404 Not Found` RFC 7807 `ProblemDetail`):**
  ```bash
  curl -i -X GET "http://localhost:8080/api/v1/owners/999" -H "Accept: application/json"
  ```
  *Respuesta esperada (`404 Not Found`):*
  ```json
  {
    "type": "https://petclinic.spring.io/errors/not-found",
    "title": "Resource Not Found",
    "status": 404,
    "detail": "Owner not found with id: 999",
    "instance": "/api/v1/owners/999"
  }
  ```

### 4.3. Crear un nuevo propietario (Flujo exitoso y Error 400 de Validación)
- **Flujo exitoso (`201 Created` + Header `Location`):**
  ```bash
  curl -i -X POST "http://localhost:8080/api/v1/owners" \
    -H "Content-Type: application/json" \
    -d '{"firstName":"Carlos","lastName":"Mendoza","address":"Av. Libertador 123","city":"Caracas","telephone":"0414123456"}'
  ```
- **Caso de error — Teléfono inválido y dirección vacía (`400 Bad Request`):**
  ```bash
  curl -i -X POST "http://localhost:8080/api/v1/owners" \
    -H "Content-Type: application/json" \
    -d '{"firstName":"Carlos","lastName":"Mendoza","address":"","city":"Caracas","telephone":"123"}'
  ```
  *Respuesta esperada (`400 Bad Request`):* `ProblemDetail` con el mapa `"errors": {"address": "...", "telephone": "..."}`.

### 4.4. Actualizar un propietario existente (`PUT /api/v1/owners/{ownerId}`)
```bash
curl -i -X PUT "http://localhost:8080/api/v1/owners/1" \
  -H "Content-Type: application/json" \
  -d '{"firstName":"George","lastName":"Franklin","address":"200 E. Main St.","city":"Madison","telephone":"6085551023"}'
```
- **Respuesta esperada:** `200 OK` con la dirección actualizada.

### 4.5. Consultar catálogo de tipos de mascota y registrar una mascota (Éxito, Error 400 y Conflicto 409)
- **Consultar catálogo (`200 OK`):**
  ```bash
  curl -i -X GET "http://localhost:8080/api/v1/pet-types" -H "Accept: application/json"
  ```
  *(Retorna IDs `1=cat`, `2=dog`, `3=lizard`, `4=snake`, `5=bird`, `6=hamster`).*
- **Registrar mascota nueva para el propietario 1 (`201 Created`):**
  ```bash
  curl -i -X POST "http://localhost:8080/api/v1/owners/1/pets" \
    -H "Content-Type: application/json" \
    -d '{"name":"Rocky","birthDate":"2022-05-10","typeId":2}'
  ```
- **Caso límite — Nombre de mascota duplicado para el mismo dueño (`409 Conflict`):**
  *(El propietario 1 ya posee la mascota `"Leo"` en `data.sql`):*
  ```bash
  curl -i -X POST "http://localhost:8080/api/v1/owners/1/pets" \
    -H "Content-Type: application/json" \
    -d '{"name":"leo","birthDate":"2022-05-10","typeId":1}'
  ```
  *Respuesta esperada (`409 Conflict`):* `ProblemDetail` indicando `"Duplicate Resource Conflict"`.
- **Caso de error — Fecha de nacimiento en el futuro (`400 Bad Request`):**
  ```bash
  curl -i -X POST "http://localhost:8080/api/v1/owners/1/pets" \
    -H "Content-Type: application/json" \
    -d '{"name":"FuturePet","birthDate":"2099-01-01","typeId":1}'
  ```

### 4.6. Actualizar una mascota existente (`PUT /api/v1/owners/{ownerId}/pets/{petId}`)
```bash
curl -i -X PUT "http://localhost:8080/api/v1/owners/1/pets/1" \
  -H "Content-Type: application/json" \
  -d '{"name":"Leo Jr","birthDate":"2010-09-07","typeId":1}'
```
- **Respuesta esperada:** `200 OK` con los datos actualizados de la mascota `1`.

### 4.7. Registrar y consultar visitas clínicas (`GET` y `POST /api/v1/owners/{ownerId}/pets/{petId}/visits`)
- **Consultar visitas de la mascota 7 (`Samantha`, dueña `Jean Coleman` `ownerId=6`):**
  ```bash
  curl -i -X GET "http://localhost:8080/api/v1/owners/6/pets/7/visits" -H "Accept: application/json"
  ```
  *Respuesta esperada (`200 OK`):* Lista cronológica con las 2 visitas precargadas en `data.sql`.
- **Agendar nueva visita futura (`201 Created`):**
  ```bash
  curl -i -X POST "http://localhost:8080/api/v1/owners/6/pets/7/visits" \
    -H "Content-Type: application/json" \
    -d '{"date":"2027-06-15","description":"Control dental anual"}'
  ```
- **Caso de error — Fecha de visita pasada o actual (`400 Bad Request`):**
  ```bash
  curl -i -X POST "http://localhost:8080/api/v1/owners/6/pets/7/visits" \
    -H "Content-Type: application/json" \
    -d '{"date":"2020-01-01","description":"Fecha invalida"}'
  ```

### 4.8. Consultar veterinarios y especialidades (`GET /api/v1/vets` y `/api/v1/vets/all`)
- **Consulta paginada (`200 OK`):**
  ```bash
  curl -i -X GET "http://localhost:8080/api/v1/vets?page=0&size=5" -H "Accept: application/json"
  ```
- **Consulta completa sin paginar (`200 OK`):**
  ```bash
  curl -i -X GET "http://localhost:8080/api/v1/vets/all" -H "Accept: application/json"
  ```

### 4.9. Cómo restablecer los datos para pruebas 100% reproducibles
Dado que el perfil por defecto utiliza **H2 en memoria** inicializado desde `schema.sql` y `data.sql`, basta con reiniciar el proceso `./mvnw spring-boot:run` para restaurar exactamente el estado inicial de la base de datos. Asimismo, toda la suite de pruebas (`./mvnw test`) se ejecuta con rollback transaccional y contextos aislados.

---

## Licencia

Spring PetClinic se distribuye bajo la versión 2.0 de la [Apache License](https://www.apache.org/licenses/LICENSE-2.0). Se conservan todos los avisos de copyright y atribución de los autores originales del proyecto.
