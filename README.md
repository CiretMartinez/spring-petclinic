# Spring PetClinic — Modular REST API (Java 21)

Modernización de **Spring PetClinic** hacia una arquitectura de **Monolito Modular** con **Java 21 LTS** y **API RESTful JSON (`/api/v1/...`)**, reemplazando por completo la capa de vistas SSR (Thymeleaf) por contratos inmutables basados en Java 21 `record`, validaciones declarativas (Jakarta Bean Validation), manejo centralizado de errores RFC 7807 (`ProblemDetail`) y documentación interactiva OpenAPI 3 / Swagger UI.

---

## Documentación de Ingeniería y Arquitectura (`docs/`)

Toda la documentación técnica del proceso de modernización se mantiene actualizada en la carpeta [`docs/`](docs/):

1. **[`docs/01-diagnostico-legacy.md`](docs/01-diagnostico-legacy.md)**: Diagnóstico técnico del sistema original, inventario de dependencias, hallazgos de deuda técnica e invariantes de negocio.
2. **[`docs/02-arquitectura.md`](docs/02-arquitectura.md)**: Diseño de arquitectura del Monolito Modular, diagramas en formato Mermaid (Componentes, Secuencia y Entidad-Relación), límites de módulos y Decisiones Arquitectónicas (ADRs).
3. **[`docs/03-plan-migracion.md`](docs/03-plan-migracion.md)**: Matriz de correspondencia entre endpoints MVC/Thymeleaf originales y la nueva API REST (`/api/v1`), análisis de riesgos y roadmap ejecutado.
4. **[`docs/04-uso-ia.md`](docs/04-uso-ia.md)**: Bitácora transparente de prompts clave, sugerencias aceptadas/descartadas y validaciones humanas realizadas en cada iteración.

---

## Arquitectura Modular

El proyecto está organizado en módulos funcionales desacoplados bajo `org.springframework.samples.petclinic`:

| Módulo | Paquete Base | Responsabilidad |
| :--- | :--- | :--- |
| **Shared Kernel (`model`)** | `org.springframework.samples.petclinic.model` | Superclases JPA (`BaseEntity`, `NamedEntity`, `Person`) y el contenedor genérico inmutable `PageResponse<T>`. |
| **Infraestructura (`system`)** | `org.springframework.samples.petclinic.system` | Manejo global de excepciones RFC 7807 (`GlobalExceptionHandler`), configuración de caché JCache (`CacheConfiguration`) y especificación OpenAPI 3 (`OpenApiConfiguration`). |
| **Módulo 1: Owners & Pets (`owner`)** | `org.springframework.samples.petclinic.owner` | Gestión de propietarios, mascotas y catálogo de tipos de mascota (`OwnerPetService`, `OwnerPetMapper`, DTOs `record`, `OwnerRestController`, `PetRestController`, `PetTypeRestController`). |
| **Módulo 2: Visits (`visit`)** | `org.springframework.samples.petclinic.visit` | Agendamiento y consulta de visitas clínicas (`VisitService`, `VisitMapper`, `VisitRepository`, DTOs `record`, `VisitRestController`). |
| **Módulo 3: Vets (`vet`)** | `org.springframework.samples.petclinic.vet` | Catálogo cacheado de veterinarios y especialidades (`VetService`, `VetMapper`, DTOs `record`, `VetRestController`). |

---

## Catálogo de Endpoints REST (`/api/v1`)

| Módulo | Método | Ruta | Descripción | Códigos HTTP |
| :--- | :---: | :--- | :--- | :--- |
| **Owners** | `GET` | `/api/v1/owners?lastName={lastName}&page={page}&size={size}` | Búsqueda paginada de propietarios por prefijo de apellido | `200`, `400` |
| **Owners** | `GET` | `/api/v1/owners/{ownerId}` | Consulta de propietario por ID (incluye mascotas y visitas) | `200`, `404` |
| **Owners** | `POST` | `/api/v1/owners` | Creación de un nuevo propietario | `201`, `400` |
| **Owners** | `PUT` | `/api/v1/owners/{ownerId}` | Actualización de datos de un propietario existente | `200`, `400`, `404` |
| **Pets** | `GET` | `/api/v1/owners/{ownerId}/pets` | Listado de mascotas de un propietario | `200`, `404` |
| **Pets** | `GET` | `/api/v1/owners/{ownerId}/pets/{petId}` | Detalle de una mascota específica de un propietario | `200`, `404` |
| **Pets** | `POST` | `/api/v1/owners/{ownerId}/pets` | Registro de mascota validando unicidad de nombre por dueño | `201`, `400`, `404`, `409` |
| **Pets** | `PUT` | `/api/v1/owners/{ownerId}/pets/{petId}` | Actualización de mascota existente | `200`, `400`, `404`, `409` |
| **Pet Types** | `GET` | `/api/v1/pet-types` | Catálogo de tipos de mascota (`cat`, `dog`, `lizard`, etc.) | `200` |
| **Visits** | `GET` | `/api/v1/owners/{ownerId}/pets/{petId}/visits` | Historial cronológico de visitas de una mascota | `200`, `404` |
| **Visits** | `POST` | `/api/v1/owners/{ownerId}/pets/{petId}/visits` | Agendamiento de nueva visita futura para una mascota | `201`, `400`, `404` |
| **Vets** | `GET` | `/api/v1/vets?page={page}&size={size}` | Listado paginado de veterinarios y especialidades (con caché) | `200`, `400` |
| **Vets** | `GET` | `/api/v1/vets/all` | Listado completo de veterinarios y especialidades (con caché) | `200` |

### Documentación OpenAPI 3 / Swagger UI
Con la aplicación en ejecución:
- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **Especificación JSON OpenAPI 3:** `http://localhost:8080/v3/api-docs`

---

## Requisitos Previos

- **Java 21** o superior (JDK completo).
- **Maven** (incluido vía wrapper `./mvnw` / `.\mvnw.cmd`) o **Gradle** (`./gradlew`).
- *(Opcional)* **Docker** si se desea levantar bases de datos MySQL o PostgreSQL mediante `docker compose` o ejecutar pruebas con Testcontainers.

---

## Instrucciones de Compilación, Ejecución y Pruebas

### 1. Compilar el proyecto
```bash
./mvnw clean compile
```

### 2. Ejecutar la aplicación localmente (Base de datos H2 en memoria por defecto)
```bash
./mvnw spring-boot:run
```
La API estará disponible en `http://localhost:8080/api/v1/...`.

También puede ejecutarse con perfiles de base de datos persistentes:
- **MySQL:** `./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql`
- **PostgreSQL:** `./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres`

### 3. Ejecutar la suite de pruebas (Unitarias, Integración y Concurrencia)
```bash
./mvnw test
```
> **Nota para entornos sin acceso a red (Offline):**
> Si se desea compilar y ejecutar las pruebas utilizando únicamente la caché local de Maven sin invocar plugins de validación externos:
> ```bash
> ./mvnw compiler:compile compiler:testCompile surefire:test -o
> ```

---

## Licencia

Spring PetClinic se distribuye bajo la versión 2.0 de la [Apache License](https://www.apache.org/licenses/LICENSE-2.0).
