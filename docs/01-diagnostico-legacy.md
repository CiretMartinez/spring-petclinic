# 01. Diagnóstico del Sistema Legacy — Spring PetClinic

**Fecha de análisis:** 2026-11-11  
**Rol:** Arquitecto de Software Senior / Tech Lead  
**Versión Base Analizada:** `org.springframework.samples:spring-petclinic:4.0.0-SNAPSHOT` (Spring Boot Parent `4.1.0`)

---

## 1. Resumen Ejecutivo

El repositorio base de **Spring PetClinic** implementa una aplicación monolítica web tradicional renderizada en el servidor (**SSR - Server-Side Rendering**) utilizando **Spring MVC** y **Thymeleaf**, estructurada parcialmente por paquetes de dominio (`owner`, `vet`, `system`, `model`). Aunque utiliza una versión reciente de Spring Boot (`4.1.0`), mantiene patrones arquitectónicos acoplados al ciclo de vida de vistas HTML (`@Controller`, `@ModelAttribute`, `@InitBinder`, `RedirectAttributes`, `Formatter`) y carece de una capa de servicios explícita, exponiendo directamente las entidades JPA hacia la capa de presentación.

El objetivo de esta modernización es evolucionar la plataforma hacia una **API REST modular con Java 21**, desacoplando contratos de transferencia (DTOs), estandarizando validaciones, centralizando el manejo de errores (RFC 7807 / `ProblemDetail`) y documentando los contratos mediante OpenAPI 3 / Swagger.

---

## 2. Inventario Tecnológico y Dependencias Actuales

### 2.1. Toolchain y Build System
- **Gestores de construcción:** Dual (`pom.xml` con Maven y `build.gradle` con Gradle).
- **Versión de Java configurada:** **Java 17** (`<java.version>17</java.version>` en Maven y `JavaLanguageVersion.of(17)` en Gradle).
  - *Nota técnica relevante en `pom.xml`:* El plugin `checkstyle` está fijado en `12.3.1` con un comentario explícito indicando que Checkstyle v13 requiere mínimo JDK 21.
- **Plugins de Calidad y Formato:**
  - `spring-javaformat-maven-plugin` (`0.0.47`): Valida el formato de código en la fase `validate` (cualquier modificación de código debe cumplir con el formateador de Spring o ejecutarse `./mvnw spring-javaformat:apply`).
  - `maven-checkstyle-plugin` (`3.6.0`) + `nohttp-checkstyle` (`0.0.11`): Prohibición de URLs con protocolo `http://` inseguro en código y configuración.
  - `jacoco-maven-plugin` (`0.8.15`): Cobertura de pruebas unitarias e integración.
  - `native-maven-plugin` (GraalVM) con soporte AOT mediante `PetClinicRuntimeHints`.
  - `cyclonedx-maven-plugin` y `git-commit-id-maven-plugin` para observabilidad en Actuator.

### 2.2. Dependencias Principales (`pom.xml`)
| Categoría | Dependencia | Propósito Actual | Impacto en Migración a API REST |
| :--- | :--- | :--- | :--- |
| **Web / MVC** | `spring-boot-starter-webmvc` | Controladores HTTP y soporte servlet | **Conservar** (base para `@RestController`). |
| **Vistas SSR** | `spring-boot-starter-thymeleaf`, `webjars-*` (`bootstrap`, `font-awesome`, `webjars-locator-lite`) | Renderizado HTML en servidor y estilos CSS/SCSS | **Deprecar / Aislar** al migrar endpoints a REST puro. |
| **Persistencia** | `spring-boot-starter-data-jpa` | Repositorios Spring Data JPA y Hibernate | **Conservar y optimizar** (revisar `FetchType.EAGER`). |
| **Validación** | `spring-boot-starter-validation` | Bean Validation (`jakarta.validation`) | **Conservar** y trasladar hacia Request DTOs. |
| **Caché** | `spring-boot-starter-cache`, `javax.cache:cache-api`, `caffeine` | Caché JCache (`vets`) con estadísticas JMX | **Conservar** en módulo de Veterinarios (`Vet`). |
| **Serialización XML** | `jakarta.xml.bind-api` | Anotaciones `@XmlRootElement` / `@XmlElement` en `Vets` y `Vet` | **Refactorizar** a contratos DTO JSON estándar. |
| **Bases de Datos** | `h2` (default en memoria), `mysql-connector-j`, `postgresql` | Soporte multi-motor por perfiles (`default`, `mysql`, `postgres`) | **Conservar** esquemas e inicialización SQL (`schema.sql`, `data.sql`). |
| **Observabilidad** | `spring-boot-starter-actuator` | Métricas, healthchecks, info, SBOM | **Conservar** (restringir exposición en producción). |
| **Testing** | `spring-boot-starter-*-test`, `testcontainers` (MySQL), `spring-boot-docker-compose` (Postgres) | Pruebas con `@WebMvcTest`, `@DataJpaTest`, `@SpringBootTest` | **Ampliar** para validar contratos JSON REST. |

---

## 3. Radiografía de la Estructura de Paquetes y Componentes

El proyecto sigue una estructura base bajo `org.springframework.samples.petclinic`:

```text
org.springframework.samples.petclinic
├── PetClinicApplication.java          # Clase principal (@SpringBootApplication)
├── PetClinicRuntimeHints.java         # Hints de reflexión/recursos para GraalVM Native Image
├── model/                             # Superclases mapeadas (@MappedSuperclass) compartidas
│   ├── BaseEntity.java                # ID autogenerado (Integer) + método isNew()
│   ├── NamedEntity.java               # Extiende BaseEntity, añade campo name (@NotBlank)
│   └── Person.java                    # Extiende BaseEntity, añade firstName y lastName (@NotBlank, @Size(max=30))
├── owner/                             # Agregado de Propietarios, Mascotas y Visitas
│   ├── Owner.java                     # Entidad raíz (tabla 'owners') con lista EAGER de Pet
│   ├── Pet.java                       # Entidad (tabla 'pets') con PetType y Set EAGER de Visit
│   ├── PetType.java                   # Catálogo (tabla 'types')
│   ├── Visit.java                     # Entidad (tabla 'visits')
│   ├── OwnerRepository.java           # JpaRepository<Owner, Integer>
│   ├── PetTypeRepository.java         # JpaRepository<PetType, Integer>
│   ├── OwnerController.java           # Controlador MVC de propietarios
│   ├── PetController.java             # Controlador MVC de mascotas (/owners/{ownerId}/pets/...)
│   ├── VisitController.java           # Controlador MVC de visitas (/owners/{ownerId}/pets/{petId}/visits/...)
│   ├── PetValidator.java              # Validador imperativo Spring (org.springframework.validation.Validator)
│   └── PetTypeFormatter.java          # Formatter de texto <-> PetType para formularios HTML
├── vet/                               # Módulo de Veterinarios y Especialidades
│   ├── Vet.java                       # Entidad (tabla 'vets') con relación ManyToMany EAGER a Specialty
│   ├── Specialty.java                 # Entidad catálogo (tabla 'specialties')
│   ├── Vets.java                      # Wrapper JAXB/JSON para lista de veterinarios
│   ├── VetRepository.java             # Repository<Vet, Integer> de solo lectura con @Cacheable("vets")
│   └── VetController.java             # Controlador híbrido: HTML (/vets.html) y JSON/XML (@ResponseBody /vets)
└── system/                            # Infraestructura transversal y vistas generales
    ├── CacheConfiguration.java        # JCache customizer para caché "vets"
    ├── WebConfiguration.java          # Internacionalización (SessionLocaleResolver + LocaleChangeInterceptor)
    ├── WelcomeController.java         # Vista raíz GET "/" -> "welcome"
    └── CrashController.java           # Endpoint GET "/oups" que lanza RuntimeException de prueba
```

---

## 4. Hallazgos Técnicos y Deuda Técnica Identificada

### 4.1. Acoplamiento Fuerte entre Presentación (MVC) y Dominio/Persistencia (JPA)
1. **Ausencia de Capa de Servicios (`@Service`) y Casos de Uso:**
   - Los controladores (`OwnerController`, `PetController`, `VisitController`, `VetController`) inyectan directamente los repositorios (`OwnerRepository`, `PetTypeRepository`, `VetRepository`) y gestionan la lógica de negocio, validaciones cruzadas y manejo de transacciones.
2. **Exposición Directa de Entidades JPA (Sin DTOs):**
   - Las entidades `Owner`, `Pet`, `Visit` y `Vet` se enlazan directamente en los formularios mediante `@ModelAttribute`.
   - Para mitigar vulnerabilidades de *Mass Assignment* (asignación masiva del campo `id`), los controladores dependen de `@InitBinder` con `dataBinder.setDisallowedFields("id", "*.id")`.
   - En `VetController#showResourcesVetList()`, la entidad `Vet` (y su wrapper `Vets`) se serializa directamente en la respuesta HTTP con `@ResponseBody`, acoplando el esquema de base de datos al contrato externo.

### 4.2. Problemas de Diseño en el Modelo de Datos y JPA
1. **Relaciones Unidireccionales con `@OneToMany` y `FetchType.EAGER`:**
   - En `Owner`: `@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER) @JoinColumn(name = "owner_id") private final List<Pet> pets`.
   - En `Pet`: `@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER) @JoinColumn(name = "pet_id") private final Set<Visit> visits`.
   - En `Vet`: `@ManyToMany(fetch = FetchType.EAGER) private Set<Specialty> specialties`.
   - **Impacto:** Cada consulta de una lista de `Owner` carga en memoria todas sus mascotas y todas las visitas de cada mascota (compensado parcialmente por `spring.jpa.properties.hibernate.default_batch_fetch_size=16` en `application.properties`). Además, al ser relaciones `@OneToMany` unidireccionales con `@JoinColumn`, Hibernate puede generar sentencias `INSERT` seguidas de `UPDATE` adicionales sobre las claves foráneas (`owner_id`, `pet_id`).
2. **Persistencia exclusiva a través del Aggregate Root (`Owner`):**
   - No existen `PetRepository` ni `VisitRepository`. Toda creación o modificación de una mascota (`Pet`) o visita (`Visit`) requiere cargar el `Owner` completo desde la base de datos, mutar su grafo de objetos en memoria (`owner.addPet(pet)`, `owner.addVisit(petId, visit)`) y ejecutar `owners.save(owner)` o `owners.saveAndFlush(owner)`.
3. **Mutación de estado en métodos `@ModelAttribute` antes del Handler:**
   - En `VisitController#loadPetWithVisit`, anotado con `@ModelAttribute("visit")`, se instancia `new Visit()` y se ejecuta `pet.addVisit(visit)` **antes** siquiera de validar la petición o incluso en un simple `GET /owners/{ownerId}/pets/{petId}/visits/new`. Esto funciona únicamente porque `spring.jpa.open-in-view=false` evita un flush accidental en peticiones `GET`, pero es un antipatrón frágil al migrar a arquitecturas sin estado (Stateless REST).

### 4.3. Validaciones Fragmentadas y Heterogéneas
1. **Mezcla de Bean Validation y `org.springframework.validation.Validator`:**
   - `Owner`, `Person`, `NamedEntity` y `Visit` utilizan anotaciones de Jakarta Bean Validation (`@NotBlank`, `@Size`, `@Pattern`).
   - Sin embargo, `Pet` **no** usa anotaciones de validación en sus campos `birthDate` ni `type`; en su lugar, delega en la clase imperativa `PetValidator` registrada vía `@InitBinder("pet")`.
2. **Validaciones de negocio incrustadas en los Controladores:**
   - Validación de fecha de nacimiento futura de mascota (`pet.getBirthDate().isAfter(LocalDate.now())`) duplicada tanto en `PetController#processCreationForm` como en `PetController#processUpdateForm`.
   - Validación de fecha de visita (`!visit.getDate().isAfter(LocalDate.now())`) escrita directamente en `VisitController#processNewVisitForm`.
   - Validación de nombres duplicados de mascota por propietario implementada en tres lugares distintos: en memoria en `PetController`, en el método `Owner#addPet`, y mediante captura de `DataIntegrityViolationException` inspeccionando en texto plano si el mensaje contiene `"unique_owner_pet_name"`.

### 4.4. Manejo de Errores Orientado a Vistas HTML
- No existe un `@RestControllerAdvice` ni manejo estructurado de excepciones para APIs.
- Cuando no se encuentra un `Owner` o `Pet`, los controladores lanzan `IllegalArgumentException("Owner not found with id: ...")`, lo cual en un entorno REST sin tratamiento adecuado resulta en respuestas HTTP `500 Internal Server Error` en lugar de `404 Not Found`.
- `CrashController` (`GET /oups`) lanza un `RuntimeException` explícito pensado para renderizar la plantilla `error.html`.

### 4.5. Comportamientos No-RESTful en Rutas y Respuestas
- **Redirecciones condicionales según cardinalidad:** `GET /owners` devuelve una vista de formulario si hay 0 resultados, un `302 Redirect` a `/owners/{id}` si hay exactamente 1 resultado, o una tabla HTML paginada si hay más de 1 resultado. En una API REST, una búsqueda paginada siempre debe devolver `200 OK` con una estructura de página consistente (tenga 0, 1 o N elementos).
- **Paginación 1-indexed acoplada a redirección:** Si el parámetro `page` está fuera de rango (`page < 1` o `page > totalPages`), `OwnerController` y `VetController` redirigen vía `302` a `page=1`.
- **Uso de `POST` para actualizaciones:** Todas las ediciones (`POST /owners/{ownerId}/edit`, `POST /owners/{ownerId}/pets/{petId}/edit`) usan el verbo `POST` por limitaciones de formularios HTML estándar, en lugar de `PUT` o `PATCH`.

---

## 5. Catálogo de Reglas de Negocio Originales (Invariantes a Preservar)

Durante la modernización a Java 21 y API REST, es obligatorio preservar las siguientes reglas de negocio extraídas del código fuente y las pruebas (`ClinicServiceTests`, `PetClinicConcurrencyTests`, `ValidatorTests`):

### 5.1. Módulo Propietarios (`Owner`)
1. **Datos personales obligatorios (`Person`):**
   - `firstName`: Obligatorio (`@NotBlank`), longitud máxima de 30 caracteres (`@Size(max = 30)`).
   - `lastName`: Obligatorio (`@NotBlank`), longitud máxima de 30 caracteres (`@Size(max = 30)`).
2. **Datos de contacto y ubicación (`Owner`):**
   - `address`: Obligatorio (`@NotBlank`), longitud máxima de 255 caracteres (`@Size(max = 255)`).
   - `city`: Obligatorio (`@NotBlank`), longitud máxima de 80 caracteres (`@Size(max = 80)`).
   - `telephone`: Obligatorio (`@NotBlank`), debe contener **exactamente 10 dígitos numéricos** (`@Pattern(regexp = "\\d{10}")`).
3. **Búsqueda de propietarios:**
   - Búsqueda por prefijo de apellido (`findByLastNameStartingWith`), *case-insensitive* en base de datos (`VARCHAR_IGNORECASE` en H2). Si no se envía filtro de apellido, retorna todos los propietarios paginados (tamaño de página original: 5 registros).
4. **Integridad de Identificador en Actualización:**
   - Al actualizar un propietario, el ID de la ruta debe coincidir con el recurso objetivo (prohibido mutar el ID).

### 5.2. Módulo Mascotas (`Pet` y `PetType`)
1. **Campos obligatorios (`PetValidator` / `NamedEntity`):**
   - `name`: Obligatorio (`hasText` / `@NotBlank`), longitud máxima de 30 caracteres (`MAX_NAME_LENGTH = 30`).
   - `birthDate`: Obligatorio (`notNull`), formato `yyyy-MM-dd`.
   - `type`: Obligatorio (`notNull`), debe corresponder a un `PetType` válido existente en el catálogo (`cat`, `dog`, `lizard`, `snake`, `bird`, `hamster`).
2. **Restricción temporal de nacimiento:**
   - `birthDate` **no puede ser posterior a la fecha actual** (`!birthDate.isAfter(LocalDate.now())`, equivalente a `@PastOrPresent`).
3. **Unicidad de nombre de mascota por propietario (Regla Crítica y de Concurrencia):**
   - Un mismo propietario (`Owner`) **no puede tener dos mascotas con el mismo nombre** (comparación *case-insensitive*).
   - Diferentes propietarios **sí** pueden tener mascotas con el mismo nombre.
   - Existe una restricción a nivel de base de datos `CONSTRAINT unique_owner_pet_name UNIQUE (owner_id, name)` y una prueba de concurrencia (`PetClinicConcurrencyTests`) que exige que ante peticiones simultáneas con el mismo nombre de mascota para el mismo dueño, exactamente una tenga éxito y la otra sea rechazada limpiamente.
4. **Ordenamiento:**
   - Las mascotas de un propietario se presentan ordenadas alfabéticamente por `name` (`@OrderBy("name")`).

### 5.3. Módulo Visitas (`Visit`)
1. **Asociación jerárquica:**
   - Toda visita pertenece a una mascota (`Pet`) existente, la cual debe pertenecer al propietario (`Owner`) especificado.
2. **Campos y restricciones temporales:**
   - `description`: Obligatoria (`@NotBlank`), longitud máxima de 255 caracteres (`@Size(max = 255)`).
   - `date` (`visit_date`): Por defecto se inicializa al día siguiente (`LocalDate.now().plusDays(1)`). En la creación de una nueva visita (`VisitController#processNewVisitForm`), la fecha **debe ser estrictamente posterior al día actual** (`visit.getDate().isAfter(LocalDate.now())`, equivalente a `@Future`).
3. **Ordenamiento:**
   - Las visitas de una mascota se ordenan cronológicamente de forma ascendente por fecha (`@OrderBy("date ASC")`).

### 5.4. Módulo Veterinarios (`Vet` y `Specialty`)
1. **Operación de solo lectura con Caché:**
   - El catálogo de veterinarios y sus especialidades (`radiology`, `surgery`, `dentistry`) es de consulta pública, respaldado por la caché `"vets"` (`@Cacheable("vets")`).
2. **Ordenamiento de especialidades:**
   - Las especialidades de cada veterinario se exponen ordenadas alfabéticamente por nombre (`Comparator.comparing(NamedEntity::getName)`).

---

## 6. Conclusiones y Preparación para la Fase 2 (Arquitectura)

El diagnóstico confirma que el repositorio base cuenta con una suite de pruebas sólida y un modelo relacional claro, pero requiere una intervención estructural bien planificada para convertirse en una aplicación **Java 21 + API REST modular**:
1. **Actualización de Toolchain a Java 21:** Elevar `<java.version>21</java.version>` en `pom.xml` (y `build.gradle`), habilitando el uso de `record` para DTOs inmutables, *Pattern Matching* y mejoras del lenguaje.
2. **Modularización y Separación de Capas:** Definir límites claros entre los módulos (`owner`/`pet`, `visit`, `vet`, `shared`/`system`), introduciendo servicios de dominio/aplicación y mappers entre Entidades JPA y DTOs (`record`).
3. **Estandarización REST y OpenAPI:** Diseñar controladores `@RestController` versionados o bajo el prefijo `/api/v1/...` (o coexistiendo estratégicamente según el plan de migración), manejo global de excepciones con `@RestControllerAdvice` (`ProblemDetail`) y documentación automática con `springdoc-openapi`.

