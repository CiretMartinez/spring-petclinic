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
  - Pendiente de revisión del entregable `docs/01-diagnostico-legacy.md` para proceder al diseño de arquitectura (`docs/02-arquitectura.md`) y plan de migración (`docs/03-plan-migracion.md`).

