# 04. Bitácora de Uso de Inteligencia Artificial (IA)

Este documento registra de manera transparente las interacciones clave, los prompts utilizados, las decisiones técnicas sugeridas por el asistente de IA (actuando como Arquitecto de Software Senior / Tech Lead) y las validaciones humanas realizadas durante la modernización de **Spring PetClinic** a **Java 21** y **API REST modular**.

---

## Registro de Iteraciones

### Iteración 01 — Diagnóstico del Sistema Legacy y Reglas de Negocio
- **Fase:** Fase 1 - Diagnóstico (`docs/01-diagnostico-legacy.md`)
- **Prompt / Solicitud Clave:**
  > *"Actúa como un Arquitecto de Software Senior y Tech Lead experto en migraciones de ecosistemas Spring Boot a Java 21... Para empezar, lee la estructura actual del proyecto y genera el primer entregable en `docs/01-diagnostico-legacy.md` basándote en el código del repositorio base de Spring PetClinic."*
- **Análisis Ejecutado por la IA:**
  - Inspección completa de `pom.xml`, `build.gradle`, `application.properties` y `schema.sql`.
  - Lectura de todas las entidades, repositorios, controladores, validadores y configuraciones en `src/main/java/org/springframework/samples/petclinic/**`.
  - Revisión de pruebas de integración y concurrencia (`ClinicServiceTests`, `PetClinicConcurrencyTests`).
- **Hallazgos Clave Documentados (Sugerencias Aceptadas):**
  1. Identificación de que el proyecto base usa **Spring Boot 4.1.0** con **Java 17**, requerimiento de Checkstyle/Spring JavaFormat activo en fase `validate`.
  2. Detección de ausencia de capa `@Service`, exposición directa de entidades JPA en vistas Thymeleaf y mitigación manual con `@InitBinder`.
  3. Identificación de la regla crítica de concurrencia en `PetController` y `schema.sql` (`unique_owner_pet_name`) verificada por `PetClinicConcurrencyTests`.
  4. Catálogo exhaustivo de invariantes de validación (`Owner`, `Pet`, `Visit`, `Vet`).
- **Sugerencias Rechazadas / Ajustadas:**
  - *Ninguna en esta fase:* Se cumplió estrictamente la regla de no asumir ni inventar estructuras, documentando únicamente lo existente en el código fuente real.
- **Validación Humana:**
  - El usuario aprobó el diagnóstico inicial (`docs/01-diagnostico-legacy.md`) y definió la directriz arquitectónica clave para la Fase 2: reemplazar completamente las vistas de Thymeleaf por una API REST/JSON moderna bajo el prefijo `/api/v1/...` estructurada como un monolito modular.

---

### Iteración 02 — Diseño de Arquitectura Modular y Plan de Migración REST
- **Fase:** Fase 2 - Diseño de Arquitectura y Plan de Migración (`docs/02-arquitectura.md` y `docs/03-plan-migracion.md`)
- **Prompt / Solicitud Clave:**
  > *"Sí, procedamos con la Fase 2. Reemplacemos completamente las vistas de Thymeleaf por una API REST/JSON moderna bajo el prefijo `/api/v1/...`, ya que la prioridad es un backend robusto y modular. Diseña la arquitectura como un monolito modular con al menos dos módulos funcionales claros (ej. `owners-pets` y `visits-vets`), separación de DTOs y entidades JPA, y documentación OpenAPI. Adelante con docs/02-arquitectura.md y docs/03-plan-migracion.md."*
- **Decisiones Arquitectónicas Generadas por la IA (Sugerencias Aceptadas):**
  1. **Estructuración del Monolito Modular:** División explícita en módulos funcionales desacoplados:
     - `owner` (Gestión de Propietarios, Mascotas y Tipos de Mascota).
     - `visit` (Gestión y agendamiento de Visitas Clínicas).
     - `vet` (Catálogo cacheado de Veterinarios y Especialidades).
     - `system` & `model` (Shared Kernel, `PageResponse<T>`, `GlobalExceptionHandler` con `ProblemDetail` RFC 7807 y configuración OpenAPI).
  2. **Diagramas Mermaid:** Elaboración de diagramas de componentes/módulos, secuencia (creación de mascota con validación y manejo de concurrencia `409 Conflict`) y modelo Entidad-Relación en `docs/02-arquitectura.md`.
  3. **Matriz de Correspondencia MVC -> REST:** Mapeo completo de rutas legacy a `/api/v1/...`, reemplazando redirecciones condicionales `302` por respuestas determinísticas `200 OK` (`PageResponse`), `201 Created`, `400 Bad Request`, `404 Not Found` y `409 Conflict` en `docs/03-plan-migracion.md`.
  4. **Identificación de Riesgos:** Registro del impacto en `spring-javaformat`, `nohttp-checkstyle`, `PetClinicConcurrencyTests` y actualización de pruebas `@WebMvcTest`.
- **Sugerencias Rechazadas / Ajustadas:**
  - Se descartó mantener coexistencia híbrida con controladores Thymeleaf para evitar duplicidad de rutas y eliminar dependencias innecesarias del frontend legacy (`spring-boot-starter-thymeleaf`, `webjars`).
- **Validación Humana:**
  - El usuario aprobó los entregables de diseño (`docs/02-arquitectura.md` y `docs/03-plan-migracion.md`) y autorizó el inicio inmediato del **Paso 3.1**.

---

### Iteración 03 — Paso 3.1: Configuración Java 21, Limpieza Thymeleaf, OpenAPI y Manejo Global de Excepciones
- **Fase:** Fase 3.1 - Infraestructura Transversal y Base Tecnológica
- **Prompt / Solicitud Clave:**
  > *"Sí, confirmamos. Comencemos de inmediato con el Paso 3.1: actualiza la configuración base a Java 21, limpia las dependencias de Thymeleaf/WebJars, configura OpenAPI/Swagger y crea el GlobalExceptionHandler y el objeto PageResponse<T>."*
- **Cambios Implementados por la IA:**
  1. **Actualización a Java 21 y Limpieza de Build (`pom.xml` y `build.gradle`):**
     - Configuración de `<java.version>21</java.version>` y `JavaLanguageVersion.of(21)`.
     - Eliminación de `spring-boot-starter-thymeleaf`, `spring-boot-starter-thymeleaf-test`, `webjars-locator-lite`, `bootstrap`, `font-awesome` y el perfil Maven `css` (`libsass-maven-plugin`).
     - Incorporación de `springdoc-openapi-starter-webmvc-ui` (`2.8.5`) y configuración en `application.properties` (`/v3/api-docs` y `/swagger-ui.html`).
  2. **DTO Genérico de Paginación (`PageResponse<T>`):**
     - Creación del `record` inmutable `PageResponse<T>` en `org.springframework.samples.petclinic.model.dto` con método factoría `PageResponse.from(Page<T>)` y copia defensiva `List.copyOf`.
  3. **Excepciones de Dominio y Manejo Centralizado (`GlobalExceptionHandler`):**
     - Creación de `ResourceNotFoundException` (`404`), `DuplicateResourceException` (`409`) y `BusinessRuleException` (`400`) en `org.springframework.samples.petclinic.system.exception`.
     - Implementación de `GlobalExceptionHandler` (`@RestControllerAdvice`) emitiendo objetos estándar `ProblemDetail` (RFC 7807) con `type`, `title`, `status`, `detail`, `instance`, `timestamp` y mapa detallado de `errors` por campo ante fallos de Bean Validation.
  4. **Configuración OpenAPI 3 (`OpenApiConfiguration`):**
     - Registro del bean `OpenAPI` con metadatos de versión `v1.0.0` y licencia Apache 2.0 en `org.springframework.samples.petclinic.system`.
- **Validación Humana:**
  - El usuario aprobó el Paso 3.1 y autorizó la ejecución inmediata del **Paso 3.2 (Módulo 1: Owners & Pets)**.

---

### Iteración 04 — Paso 3.2: Refactorización del Módulo 1 (`Owners & Pets`)
- **Fase:** Fase 3.2 - Implementación de Módulo `owner` con Java 21 Records, Servicios y Controladores REST
- **Prompt / Solicitud Clave:**
  > *"Sí, confirmamos. Comencemos de inmediato con el Paso 3.2"*
- **Cambios Implementados por la IA:**
  1. **DTOs Inmutables con Java 21 `record` (`org.springframework.samples.petclinic.owner.dto`):**
     - `OwnerRequest`: Validaciones declarativas (`@NotBlank`, `@Size`, `@Pattern(regexp = "\\d{10}")`).
     - `OwnerResponse`: Representación inmutable del propietario con su lista de mascotas (`List<PetResponse>`).
     - `PetRequest`: Validaciones declarativas (`@NotBlank`, `@Size(max = 30)`, `@NotNull`, `@PastOrPresent` en `birthDate`, `@NotNull` en `typeId`).
     - `PetResponse` y `PetResponse.VisitSummary`: Representación de la mascota, su tipo y resumen de visitas.
     - `PetTypeResponse`: Representación inmutable del catálogo de tipos de mascota.
  2. **Mapper Dedicado (`OwnerPetMapper`):**
     - Desacoplamiento total entre las entidades JPA (`Owner`, `Pet`, `PetType`, `Visit`) y los contratos JSON expuestos.
  3. **Capa de Servicio (`OwnerPetService`):**
     - Encapsulamiento transaccional de búsqueda paginada (`findByLastNameStartingWith` con normalización `strip()`), consulta por ID, creación y edición de propietarios y mascotas.
     - Preservación de la invariante de unicidad *case-insensitive* de nombre de mascota por dueño (tanto en memoria como traduciendo `DataIntegrityViolationException` de la constraint `unique_owner_pet_name` a `DuplicateResourceException` -> `409 Conflict`).
  4. **Controladores REST Versionados (`org.springframework.samples.petclinic.owner.api`):**
     - `OwnerRestController` (`/api/v1/owners`): `GET` paginado, `GET /{ownerId}`, `POST` (`201 Created` + header `Location`), `PUT /{ownerId}`.
     - `PetRestController` (`/api/v1/owners/{ownerId}/pets`): `GET` lista, `GET /{petId}`, `POST` (`201 Created`), `PUT /{petId}`.
     - `PetTypeRestController` (`/api/v1/pet-types`): `GET` catálogo ordenado.
  5. **Limpieza de Código Legacy del Módulo:**
     - Eliminación de `OwnerController`, `PetController`, `PetValidator` y `PetTypeFormatter`.
- **Validación Humana:**
  - El usuario confirmó la finalización del Paso 3.2 y autorizó proceder con el **Paso 3.3 (Módulos `visit` y `vet` + eliminación de artefactos Thymeleaf restantes)**.

---

### Iteración 05 — Paso 3.3: Refactorización de Módulo 2 (`Visits`), Módulo 3 (`Vets`) y Limpieza Total de Thymeleaf
- **Fase:** Fase 3.3 - Implementación de Módulos `visit` y `vet` y Eliminación de Vistas Legacy
- **Prompt / Solicitud Clave:**
  > *"Sí, procedamos con el Paso 3.3. Implementa el Módulo 2 (Visits) en org.springframework.samples.petclinic.visit y el Módulo 3 (Vets) en org.springframework.samples.petclinic.vet con sus respectivos DTOs, servicios y controladores REST. Además, procede a eliminar los controladores y plantillas Thymeleaf legacy restantes."*
- **Cambios Implementados por la IA:**
  1. **Módulo 2 — `Visits` (`org.springframework.samples.petclinic.visit`):**
     - `VisitRepository`: Repositorio Spring Data JPA para la entidad `Visit`.
     - `VisitRequest` (`record`): Validaciones `@Future` (en fecha opcional; por defecto mañana `LocalDate.now().plusDays(1)` si se omite) y `@NotBlank @Size(max = 255)` en `description`.
     - `VisitResponse` (`record`): Representación inmutable con `id`, `date`, `description`, `petId` y `ownerId`.
     - `VisitMapper` y `VisitService`: Validación de pertenencia `Owner -> Pet`, regla de negocio de fecha futura y persistencia transaccional.
     - `VisitRestController` (`/api/v1/owners/{ownerId}/pets/{petId}/visits`): Endpoints `GET` (listar visitas de mascota) y `POST` (`201 Created` + header `Location`).
  2. **Módulo 3 — `Vets` (`org.springframework.samples.petclinic.vet`):**
     - `SpecialtyResponse` y `VetResponse` (`record`): Contratos inmutables sin acoplamiento JAXB/XML.
     - `VetMapper` y `VetService`: Consulta paginada (`findPaginated`) y completa (`findAll`) respaldada por la caché JCache `"vets"`.
     - `VetRestController` (`/api/v1/vets` y `/api/v1/vets/all`): Endpoints REST documentados con OpenAPI 3.
  3. **Limpieza Total de Componentes Legacy SSR:**
     - Eliminados `VisitController`, `VetController`, `Vets` (wrapper XML), `WelcomeController`, `CrashController` y el directorio completo de plantillas `src/main/resources/templates`.
     - Limpieza de la anotación `@XmlElement` en la entidad `Vet`.
- **Validación Humana:**
  - Pendiente de confirmación para ejecutar el **Paso 3.4 (Migración de la suite de pruebas a REST `/api/v1/...`, pruebas de concurrencia y actualización de `README.md`)**.

