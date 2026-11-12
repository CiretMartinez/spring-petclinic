# 03. Plan de Migración — De Monolito MVC/Thymeleaf a API REST Modular (Java 21)

**Fecha de planificación:** 2026-11-11  
**Rol:** Arquitecto de Software Senior / Tech Lead  
**Estrategia:** Reemplazo completo de capa de vistas SSR (Thymeleaf) por API RESTful JSON bajo `/api/v1` en arquitectura de Monolito Modular.

---

## 1. Matriz de Correspondencia de Endpoints (Legacy MVC vs. Nueva API REST)

### 1.1. Módulo 1: Propietarios y Mascotas (`owner`)

| Operación Funcional | Endpoint Legacy (Thymeleaf / MVC) | Nuevo Endpoint REST (`/api/v1`) | Verbo HTTP | Request Payload | Response Exitosa | Códigos de Error | Notas de Transformación |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Formulario creación dueño** | `GET /owners/new` | *Eliminado (Estado en cliente)* | — | — | — | — | En REST no existen endpoints para inicializar formularios HTML vacíos. |
| **Crear dueño** | `POST /owners/new` (Form URL-Encoded) | `/api/v1/owners` | `POST` | `OwnerRequest` (JSON) | `201 Created` (`OwnerResponse` + Header `Location`) | `400 Bad Request` | Reemplaza redirección `302` por estado `201` y cuerpo JSON con ID generado. |
| **Formulario búsqueda dueño** | `GET /owners/find` | *Eliminado (Estado en cliente)* | — | — | — | — | El cliente invoca directamente la búsqueda paginada en `GET /api/v1/owners`. |
| **Buscar / Listar dueños (Paginado)** | `GET /owners?lastName=&page=1` | `/api/v1/owners?lastName={lastName}&page={page}&size={size}` | `GET` | Query Params: `lastName` (default `""`), `page` (default `0`), `size` (default `5`) | `200 OK` (`PageResponse<OwnerResponse>`) | `400 Bad Request` | **Cambio crítico:** Siempre retorna `200 OK` con estructura paginada JSON (0, 1 o N resultados), eliminando el `302 Redirect` cuando hay 1 resultado o cuando `page` excede el límite. |
| **Consultar detalle de dueño** | `GET /owners/{ownerId}` | `/api/v1/owners/{ownerId}` | `GET` | Path Variable: `ownerId` | `200 OK` (`OwnerResponse` con mascotas y visitas) | `404 Not Found` | Traduce `IllegalArgumentException` legacy en respuesta estándar `404 ProblemDetail`. |
| **Formulario edición dueño** | `GET /owners/{ownerId}/edit` | *Eliminado (Se usa `GET /api/v1/owners/{ownerId}`)* | — | — | — | — | El cliente obtiene los datos actuales mediante `GET /api/v1/owners/{ownerId}`. |
| **Actualizar dueño** | `POST /owners/{ownerId}/edit` | `/api/v1/owners/{ownerId}` | `PUT` | `OwnerRequest` (JSON) | `200 OK` (`OwnerResponse`) | `400 Bad Request`, `404 Not Found` | Adopta semántica idempotente `PUT` y valida existencia previa del recurso. |
| **Catálogo tipos de mascota** | `@ModelAttribute("types")` en `PetController` | `/api/v1/pet-types` | `GET` | — | `200 OK` (`List<PetTypeResponse>`) | — | Expone explícitamente el catálogo que antes se inyectaba de forma oculta en el modelo HTML. |
| **Formulario creación mascota** | `GET /owners/{ownerId}/pets/new` | *Eliminado (Estado en cliente)* | — | — | — | — | — |
| **Registrar mascota a dueño** | `POST /owners/{ownerId}/pets/new` | `/api/v1/owners/{ownerId}/pets` | `POST` | `PetRequest` (JSON) | `201 Created` (`PetResponse` + Header `Location`) | `400 Bad Request`, `404 Not Found`, `409 Conflict` | Valida `@PastOrPresent` en `birthDate`, existencia de `PetType` y unicidad *case-insensitive* de nombre por dueño (`409 Conflict`). |
| **Consultar mascotas de dueño** | *(Incluido en vista de dueño)* | `/api/v1/owners/{ownerId}/pets` y `/api/v1/owners/{ownerId}/pets/{petId}` | `GET` | Path Variables: `ownerId`, `petId` | `200 OK` (`List<PetResponse>` / `PetResponse`) | `404 Not Found` | Permite consultar las mascotas de un propietario o una mascota específica de forma granular. |
| **Formulario edición mascota** | `GET /owners/{ownerId}/pets/{petId}/edit` | *Eliminado* | — | — | — | — | — |
| **Actualizar mascota** | `POST /owners/{ownerId}/pets/{petId}/edit` | `/api/v1/owners/{ownerId}/pets/{petId}` | `PUT` | `PetRequest` (JSON) | `200 OK` (`PetResponse`) | `400 Bad Request`, `404 Not Found`, `409 Conflict` | Valida que la mascota pertenezca al dueño y que el nuevo nombre no colisione con otra mascota del mismo dueño. |

---

### 1.2. Módulo 2: Visitas Clínicas (`visit`)

| Operación Funcional | Endpoint Legacy (Thymeleaf / MVC) | Nuevo Endpoint REST (`/api/v1`) | Verbo HTTP | Request Payload | Response Exitosa | Códigos de Error | Notas de Transformación |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Formulario nueva visita** | `GET /owners/{ownerId}/pets/{petId}/visits/new` | *Eliminado (Estado en cliente)* | — | — | — | — | Se elimina la mutación previa en `@ModelAttribute("visit")`. |
| **Agendar nueva visita** | `POST /owners/{ownerId}/pets/{petId}/visits/new` | `/api/v1/owners/{ownerId}/pets/{petId}/visits` | `POST` | `VisitRequest` (JSON: `date`, `description`) | `201 Created` (`VisitResponse` + Header `Location`) | `400 Bad Request`, `404 Not Found` | Si `date` es `null`, asigna `LocalDate.now().plusDays(1)`; si se envía fecha, valida que sea estrictamente posterior a hoy (`@Future`). |
| **Listar visitas de una mascota** | *(Embebido en detalle de dueño)* | `/api/v1/owners/{ownerId}/pets/{petId}/visits` | `GET` | Path Variables: `ownerId`, `petId` | `200 OK` (`List<VisitResponse>`) | `404 Not Found` | Retorna las visitas de la mascota ordenadas cronológicamente (`date ASC`). |

---

### 1.3. Módulo 3: Veterinarios (`vet`) y Sistema (`system`)

| Operación Funcional | Endpoint Legacy (Thymeleaf / MVC) | Nuevo Endpoint REST (`/api/v1`) | Verbo HTTP | Request Payload | Response Exitosa | Códigos de Error | Notas de Transformación |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Vista HTML paginada de veterinarios** | `GET /vets.html?page=1` | `/api/v1/vets?page={page}&size={size}` | `GET` | Query Params: `page` (default `0`), `size` (default `5`) | `200 OK` (`PageResponse<VetResponse>`) | `400 Bad Request` | Mantiene el uso de `@Cacheable("vets")` y devuelve estructura paginada estándar. |
| **Recurso JSON/XML de veterinarios** | `GET /vets` | `/api/v1/vets/all` (o `/api/v1/vets` sin paginar / paginado consistente) | `GET` | — | `200 OK` (`List<VetResponse>`) | — | Reemplaza el wrapper JAXB `Vets` con `@XmlElement` por `VetResponse` inmutable en JSON. |
| **Página de inicio** | `GET /` (`WelcomeController`) | `/v3/api-docs` y `/swagger-ui.html` | `GET` | — | `200 OK` | — | Documentación interactiva OpenAPI 3 como punto de entrada para desarrolladores. |
| **Prueba de fallo controlado** | `GET /oups` (`CrashController`) | *Eliminado o reemplazado por test unitario de `GlobalExceptionHandler`* | — | — | — | `500 Internal Server Error` (`ProblemDetail`) | Las excepciones no controladas se transforman automáticamente en `ProblemDetail` RFC 7807. |

---

## 2. Análisis de Riesgos y Estrategia de Mitigación

| ID | Riesgo Identificado | Probabilidad | Impacto | Estrategia de Mitigación |
| :--- | :--- | :---: | :---: | :--- |
| **R-01** | **Condición de carrera en nombres duplicados de mascotas (`PetClinicConcurrencyTests`)** | Media | Alto | Mantener la restricción `UNIQUE (owner_id, name)` en base de datos, usar `saveAndFlush` en `OwnerPetService` y capturar `DataIntegrityViolationException` en el servicio/manejador global para responder `409 Conflict` determinísticamente. |
| **R-02** | **Fallo de compilación por reglas de formato (`spring-javaformat`) y `nohttp-checkstyle`** | Alta | Medio | Ejecutar `./mvnw spring-javaformat:apply` antes de cada validación y evitar cualquier enlace sin TLS (usar siempre `https://` en Javadocs, licencias y configuraciones OpenAPI). |
| **R-03** | **Incompatibilidad de versión de librería `springdoc-openapi` con Spring Boot 4.1.0** | Media | Medio | Verificar en el sandbox la resolución de dependencias con Spring Boot `4.1.0`; si `springdoc-openapi` presenta conflicto con Jakarta/Spring 7 en Boot 4.1, configurar un descriptor OpenAPI 3.1 + Swagger UI WebJar ligero o versión compatible verificada en el build. |
| **R-04** | **Ruptura de la suite de pruebas legacy que valida vistas Thymeleaf (`view().name(...)`, `model().attribute(...)`)** | Alta | Alto | Migrar las pruebas de controladores (`OwnerControllerTests`, `PetControllerTests`, `VisitControllerTests`, `VetControllerTests`, `PetClinicIntegrationTests`, `PetClinicConcurrencyTests`) para validar contratos JSON (`jsonPath`), códigos HTTP REST (`200`, `201`, `400`, `404`, `409`) y cabeceras `Content-Type: application/json`. |
| **R-05** | **Acoplamiento en `Visit` ubicada originalmente en el paquete `owner`** | Baja | Medio | Mantener la integridad del mapeo JPA (`pets` -> `visits`) mientras se expone la lógica de negocio, DTOs, Mapper y Controlador REST en el módulo `visit`, o crear `VisitRepository` para consultas directas sin romper `ClinicServiceTests`. |

---

## 3. Secuencia de Pasos de Implementación (Roadmap Incremental)

### Fase 3.1: Modernización de Base Tecnológica y Infraestructura Transversal (`system` & `model`)
1. Actualizar `pom.xml` y `build.gradle` a **Java 21** (`<java.version>21</java.version>`), remover dependencias exclusivas de vistas (`spring-boot-starter-thymeleaf`, `webjars` de Bootstrap/FontAwesome) y configurar soporte OpenAPI/Swagger.
2. Crear el DTO genérico `PageResponse<T>` (`record`) en `model.dto`.
3. Implementar las excepciones de dominio (`ResourceNotFoundException`, `DuplicateResourceException`, `BusinessRuleException`) y el `GlobalExceptionHandler` (`@RestControllerAdvice`) basado en `ProblemDetail` (RFC 7807) en `system.exception`.
4. Configurar `OpenApiConfiguration` en `system`.

### Fase 3.2: Refactorización del Módulo 1 — `Owners & Pets` (`owner`)
1. Crear los `record` DTOs con validaciones Jakarta (`OwnerRequest`, `OwnerResponse`, `PetRequest`, `PetResponse`, `PetTypeResponse`).
2. Implementar `OwnerPetMapper` para transformar entidades JPA a DTOs sin exponer entidades.
3. Implementar `OwnerPetService` encapsulando las reglas de negocio (búsqueda paginada con `strip()` del apellido, validación de fechas de nacimiento `@PastOrPresent`, validación y manejo transaccional de unicidad de nombre de mascota por dueño).
4. Implementar `OwnerRestController` (`/api/v1/owners`), `PetRestController` (`/api/v1/owners/{ownerId}/pets`) y `PetTypeRestController` (`/api/v1/pet-types`) documentados con OpenAPI.
5. Eliminar `OwnerController`, `PetController`, `PetValidator` y `PetTypeFormatter` legacy.

### Fase 3.3: Refactorización del Módulo 2 — `Visits` (`visit`) y Módulo 3 — `Vets` (`vet`)
1. Crear el paquete modular `org.springframework.samples.petclinic.visit` con `VisitRequest`, `VisitResponse`, `VisitMapper`, `VisitService` y `VisitRestController` (`/api/v1/owners/{ownerId}/pets/{petId}/visits`).
2. Eliminar `VisitController` legacy.
3. En el módulo `vet`, crear `VetResponse`, `SpecialtyResponse`, `VetMapper`, `VetService` y `VetRestController` (`/api/v1/vets`), preservando la caché `"vets"`.
4. Eliminar `VetController`, `Vets` (wrapper XML) y los controladores de vistas `WelcomeController` y `CrashController`, así como las plantillas HTML de `src/main/resources/templates`.

### Fase 3.4: Actualización de Suite de Pruebas, Verificación de Concurrencia y Documentación Final
1. Actualizar y ampliar las pruebas unitarias y de integración (`@WebMvcTest`, `@DataJpaTest`, `@SpringBootTest`) hacia los nuevos endpoints `/api/v1/...`, incluyendo la prueba crítica `PetClinicConcurrencyTests`.
2. Ejecutar `./mvnw spring-javaformat:apply` y `./mvnw test` verificando que el build completo (Format + Checkstyle + JaCoCo + JUnit 5) pase con **0 errores**.
3. Actualizar `README.md` y `docs/04-uso-ia.md` con las instrucciones de ejecución, arquitectura final y catálogo de endpoints REST.

