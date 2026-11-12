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
  - Pendiente de confirmación del usuario para iniciar la **Fase 3 (Implementación en código Java 21 + API REST + Tests)**.

