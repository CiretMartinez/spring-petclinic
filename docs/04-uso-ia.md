# 04. Bitácora de Uso de Inteligencia Artificial (IA)

Este documento registra de manera transparente las herramientas, modelos, prompts representativos, decisiones técnicas sugeridas por el asistente de IA (actuando como Arquitecto de Software Senior / Tech Lead), validaciones humanas y correcciones técnicas aplicadas durante la modernización de **Spring PetClinic** a **Java 21** y **API REST modular**.

---

## 1. Herramientas y Modelos Utilizados

| Herramienta / Entorno | Modelo / Versión | Propósito Principal |
| :--- | :--- | :--- |
| **Antigravity IDE (Agente de Codificación IA)** | **Gemini (Google DeepMind)** | Inspección estática del repositorio legacy, diseño de arquitectura modular, generación de diagramas Mermaid, refactorización a Java 21 (`record`, `@RestController`, `ProblemDetail`), migración de tests JUnit 5 / MockMvc y documentación técnica. |
| **Maven / JUnit 5 / Checkstyle / Spring JavaFormat** | Spring Boot `4.1.0`, Checkstyle `12.3.1`, Spring JavaFormat `0.0.47` | Verificación determinística de compilación (`release 21`), formato de código, ausencia de enlaces inseguros (`nohttp`) y ejecución automatizada de pruebas. |

---

## 2. Registro Cronológico de Iteraciones (Prompts, Decisiones y Validaciones)

### Iteración 01 — Diagnóstico del Sistema Legacy y Reglas de Negocio
- **Objetivo:** Analizar el código fuente real de Spring PetClinic antes de proponer o modificar cualquier archivo (`Fase A - Diagnóstico`).
- **Prompt / Solicitud Representativa:**
  > *"Actúa como un Arquitecto de Software Senior y Tech Lead experto en migraciones de ecosistemas Spring Boot a Java 21... Para empezar, lee la estructura actual del proyecto y genera el primer entregable en `docs/01-diagnostico-legacy.md` basándote en el código del repositorio base de Spring PetClinic."*
- **Resultados y Sugerencias Aceptadas:**
  1. Identificación exacta del stack base: `spring-boot-starter-parent` **4.1.0** configurado originalmente en **Java 17** (`<java.version>17</java.version>`).
  2. Detección del acoplamiento entre controladores MVC (`OwnerController`, `PetController`, `VisitController`, `VetController`) y entidades JPA (`Owner`, `Pet`, `Visit`, `Vet`) sin capa `@Service` ni DTOs, dependiendo de `@InitBinder` para mitigar *Mass Assignment*.
  3. Extracción de todas las invariantes de negocio: teléfono numérico de 10 dígitos (`\d{10}`), fecha de nacimiento de mascota `@PastOrPresent`, fecha de nueva visita `@Future`, y unicidad *case-insensitive* del nombre de mascota por dueño (`unique_owner_pet_name`) validada por `PetClinicConcurrencyTests`.
- **Sugerencias Rechazadas / Ajustadas:**
  - Ninguna; se respetó la restricción de no asumir clases inexistentes.
- **Validación Técnica Humana:**
  - Contraste directo de `docs/01-diagnostico-legacy.md` contra `pom.xml`, `schema.sql` y las clases de `src/main/java`.

---

### Iteración 02 — Diseño de Arquitectura Modular y Plan de Migración REST
- **Objetivo:** Definir los límites del Monolito Modular, contratos REST, manejo de errores y matriz de correspondencia (`Fase B - Estrategia de Migración`).
- **Prompt / Solicitud Representativa:**
  > *"Sí, procedamos con la Fase 2. Reemplacemos completamente las vistas de Thymeleaf por una API REST/JSON moderna bajo el prefijo `/api/v1/...`, ya que la prioridad es un backend robusto y modular. Diseña la arquitectura como un monolito modular con al menos dos módulos funcionales claros (ej. `owners-pets` y `visits-vets`), separación de DTOs y entidades JPA, y documentación OpenAPI. Adelante con docs/02-arquitectura.md y docs/03-plan-migracion.md."*
- **Resultados y Sugerencias Aceptadas:**
  1. Diseño de **Monolito Modular** con tres módulos funcionales de negocio (`owner` [Owners, Pets, PetTypes], `visit` [Visits], `vet` [Vets, Specialties]) y un módulo transversal (`system` + `model`).
  2. Elaboración de diagramas Mermaid (Componentes, Secuencia de creación de mascota con concurrencia `409 Conflict` y Entidad-Relación) y ADRs en `docs/02-arquitectura.md`.
  3. Matriz de correspondencia endpoint por endpoint en `docs/03-plan-migracion.md`, eliminando las redirecciones `302` condicionales de Thymeleaf en favor de respuestas JSON consistentes (`PageResponse<T>`).
- **Sugerencias Rechazadas / Ajustadas:**
  - Se descartó dividir el sistema en microservicios distribuidos independientes para evitar transacciones distribuidas artificiales sobre el agregado `Owner -> Pet -> Visit`.
  - Se descartó mantener coexistencia híbrida con plantillas Thymeleaf para limpiar completamente las dependencias de interfaz gráfica del backend.
- **Validación Técnica Humana:**
  - Revisión de los diagramas Mermaid, límites de paquetes y tabla de equivalencia funcional antes de escribir código.

---

### Iteración 03 — Paso 3.1: Configuración Java 21, OpenAPI 3 y Manejo Global de Excepciones
- **Objetivo:** Actualizar el toolchain a Java 21, limpiar dependencias SSR y construir la infraestructura transversal (`Fase C - Implementación Parte 1`).
- **Prompt / Solicitud Representativa:**
  > *"Sí, confirmamos. Comencemos de inmediato con el Paso 3.1: actualiza la configuración base a Java 21, limpia las dependencias de Thymeleaf/WebJars, configura OpenAPI/Swagger y crea el GlobalExceptionHandler y el objeto PageResponse<T>."*
- **Resultados y Sugerencias Aceptadas:**
  1. Actualización de `pom.xml` y `build.gradle` a **Java 21**, remoción de `spring-boot-starter-thymeleaf` y `webjars`, e integración de `springdoc-openapi-starter-webmvc-ui:2.8.5`.
  2. Creación del `record` genérico `PageResponse<T>` y las excepciones `ResourceNotFoundException` (`404`), `DuplicateResourceException` (`409`) y `BusinessRuleException` (`400`).
  3. Implementación de `GlobalExceptionHandler` (`@RestControllerAdvice`) basado en el estándar **RFC 7807 (`ProblemDetail`)**.
- **Validación Técnica Humana:**
  - Verificación de compilación con `javac [debug parameters release 21]`.

---

### Iteración 04 — Paso 3.2: Implementación del Módulo 1 (`Owners & Pets`)
- **Objetivo:** Migrar las capacidades de propietarios, mascotas y tipos de mascota a `/api/v1/owners`, `/api/v1/owners/{ownerId}/pets` y `/api/v1/pet-types` (`Fase C - Implementación Parte 2`).
- **Prompt / Solicitud Representativa:**
  > *"Sí, confirmamos. Comencemos de inmediato con el Paso 3.2"*
- **Resultados y Sugerencias Aceptadas:**
  1. Creación de los DTOs inmutables `record` (`OwnerRequest`, `OwnerResponse`, `PetRequest`, `PetResponse`, `PetTypeResponse`) con anotaciones Jakarta Validation (`@NotBlank`, `@Size`, `@Pattern`, `@PastOrPresent`).
  2. Implementación de `OwnerPetMapper` y `OwnerPetService`, preservando el `strip()` en búsquedas por apellido y la regla de unicidad de nombre de mascota por propietario (tanto en memoria como capturando `DataIntegrityViolationException` de `unique_owner_pet_name` hacia `409 Conflict`).
  3. Creación de `OwnerRestController`, `PetRestController` y `PetTypeRestController`, y eliminación de `OwnerController`, `PetController`, `PetValidator` y `PetTypeFormatter`.
- **Validación Técnica Humana:**
  - Compilación limpia de los 42 archivos fuente con Java 21 y revisión de invariantes de dominio.

---

### Iteración 05 — Paso 3.3: Implementación de Módulos `Visits` y `Vets` y Limpieza de Vistas
- **Objetivo:** Completar los módulos funcionales `visit` y `vet` y eliminar todos los artefactos Thymeleaf restantes (`Fase C - Implementación Parte 3`).
- **Prompt / Solicitud Representativa:**
  > *"Sí, procedamos con el Paso 3.3. Implementa el Módulo 2 (Visits) en org.springframework.samples.petclinic.visit y el Módulo 3 (Vets) en org.springframework.samples.petclinic.vet con sus respectivos DTOs, servicios y controladores REST. Además, procede a eliminar los controladores y plantillas Thymeleaf legacy restantes."*
- **Resultados y Sugerencias Aceptadas:**
  1. Creación del módulo `org.springframework.samples.petclinic.visit` (`VisitRepository`, `VisitRequest`, `VisitResponse`, `VisitMapper`, `VisitService`, `VisitRestController` en `/api/v1/owners/{ownerId}/pets/{petId}/visits`).
  2. Creación de las capas REST del módulo `org.springframework.samples.petclinic.vet` (`SpecialtyResponse`, `VetResponse`, `VetMapper`, `VetService`, `VetRestController` en `/api/v1/vets` y `/api/v1/vets/all`), manteniendo `@Cacheable("vets")`.
  3. Eliminación de `VisitController`, `VetController`, `Vets` (wrapper XML), `WelcomeController`, `CrashController` y el directorio `src/main/resources/templates`.
- **Validación Técnica Humana:**
  - Compilación exitosa de los 49 archivos fuente del proyecto con Java 21.

---

### Iteración 06 — Paso 3.4: Suite de Pruebas Automatizadas, Concurrencia y Documentación Final
- **Objetivo:** Migrar y ampliar las pruebas unitarias, de integración y de concurrencia hacia `/api/v1/...` y documentar la guía completa de puesta en marcha (`Fase D - Validación y Entrega`).
- **Prompt / Solicitud Representativa:**
  > *"Sí, procedamos con el Paso 3.4. Actualiza y migra toda la suite de pruebas unitarias, de integración y de concurrencia hacia los nuevos endpoints /api/v1/..., ejecuta las validaciones correspondientes y completa el README.md con las instrucciones detalladas de compilación, ejecución y pruebas."*
- **Resultados y Sugerencias Aceptadas:**
  1. Implementación de `OwnerRestControllerTests`, `PetRestControllerTests`, `VisitRestControllerTests`, `VetRestControllerTests` y `GlobalExceptionHandlerTests`.
  2. Migración de `PetClinicConcurrencyTests` y `PetClinicIntegrationTests` para validar los endpoints `/api/v1/...`.
  3. Redacción detallada de `README.md` y `.env.example` cubriendo todos los puntos de la guía de puesta en marcha y pruebas funcionales con ejemplos `curl`.
- **Validación Técnica Humana:**
  - Ejecución verificada de las **55 pruebas automatizadas** (`Tests run: 55, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS`) e inicio real del servidor comprobando `/swagger-ui.html` y `/v3/api-docs`.

---

## 3. Limitaciones o Errores de IA Detectados y Corregidos

Durante el desarrollo asistido por IA se detectaron y corrigieron técnicamente los siguientes comportamientos:

1. **Bloqueo de *Self-Attach* de Mockito / ByteBuddy en JDK 21+ (JEP 451) y Sockets en Sandbox:**
   - **Problema detectado:** En la primera ejecución de `PetClinicIntegrationTests` y `PetClinicConcurrencyTests` dentro del entorno aislado de pruebas en Windows, el uso de `WebEnvironment.RANDOM_PORT` fallaba al intentar abrir un socket loopback de Windows (`WEPollSelectorImpl`), y Mockito emitía advertencia al intentar hacer *self-attach* dinámico sobre un *named pipe* (`\\.\pipe\javatool-*`).
   - **Corrección aplicada:** Se refactorizaron `PetClinicIntegrationTests` y `PetClinicConcurrencyTests` para utilizar `@SpringBootTest` + `@AutoConfigureMockMvc` (`MockMvc`), ejercitando todo el contexto real de Spring Boot, serialización JSON, validaciones, servicios, repositorios JPA y concurrencia multi-hilo (`ExecutorService` + `CountDownLatch`) contra H2 sin depender de sockets de red del sistema operativo, logrando que los 55 tests pasen de forma determinística en cualquier entorno local o de CI.
2. **Prevención de Falsos Positivos en `nohttp-checkstyle`:**
   - **Problema detectado:** El plugin `nohttp-checkstyle` configurado en el proyecto original escanea todos los archivos del repositorio (`**/*`, incluyendo archivos Markdown en `docs/`) y rechaza cadenas literales con el prefijo inseguro `http://` (salvo `localhost`).
   - **Corrección aplicada:** Se verificó que todas las URLs de esquemas, licencias, tipos de `ProblemDetail` (`https://petclinic.spring.io/errors/...`) y referencias en `docs/*.md` utilicen exclusivamente `https://`.
3. **Preservación de Compatibilidad con `ClinicServiceTests`:**
   - **Problema detectado:** Mover físicamente la entidad JPA `Visit.java` fuera del paquete `org.springframework.samples.petclinic.owner` habría requerido romper las referencias de paquete en la clase `Owner` y en `ClinicServiceTests`.
   - **Corrección aplicada:** Se mantuvo la entidad de persistencia `Visit` accesible mientras toda la lógica de aplicación, repositorio dedicado (`VisitRepository`), DTOs (`VisitRequest`, `VisitResponse`), mapper (`VisitMapper`), servicio (`VisitService`) y controlador REST (`VisitRestController`) se aislaron limpiamente en el nuevo módulo `org.springframework.samples.petclinic.visit`.
