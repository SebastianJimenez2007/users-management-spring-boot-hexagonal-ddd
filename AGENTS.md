# AGENTS.md

Proyecto Maven de módulo único (`com.jcaa.usersmanagement`, Java 17, Spring Boot 3.3.5).
El README, los comentarios y los mensajes al usuario están en español: mantén ese idioma al
editar prosa, Javadoc o textos de cara al usuario.

## Comandos

Usa el wrapper (`setup-maven-path.ps1` es opcional, solo configura el PATH una vez y no hace falta
para compilar).

```bash
./mvnw test                                        # 211 tests, no requiere servicios externos
./mvnw -Dtest=UserEmailTest test                   # una sola clase de test (verificado)
./mvnw -Dtest='*ServiceTest' test                  # por patrón; añade -DfailIfNoSpecifiedTests=false si puede no coincidir
./mvnw verify                                      # test + informe JaCoCo (solo en `verify`, no en `test`)
./mvnw clean package                               # jar ejecutable en target/
```

- **No hay** plugin de lint, formateador, checkstyle ni typecheck. `test` → `verify` es el único
  orden que importa. El informe de cobertura queda en `target/site/jacoco`.
- El suite tarda unos 2 minutos aunque no contacta nada externo. El coste está en
  `BCRYPT_COST = 12` (`domain/valueobject/UserPassword.java`), no en I/O lento de base de datos
  o SMTP. No "optimices" el coste para acelerar el CI.

## Arquitectura

`Main` (`@SpringBootApplication`) es el **único** composition root. No hay contenedor de
dependencias propio: todo el cableado es por anotaciones de Spring y component scanning.

- `domain/` — Java puro, **cero** imports de `org.springframework` / `jakarta`. Modelos, value
  objects, enums, eventos de dominio y excepciones. Mantenlo libre de framework.
- `application/port/in/*UseCase` + `application/port/out/*Port` — la frontera hexagonal. Los
  servicios en `application/service` implementan los puertos *in* y solo dependen de puertos *out*.
  Ojo: `UserRepositoryMySQL` implementa seis puertos *out* en una sola clase.
- `infrastructure/adapter/` — persistencia (JDBC puro, **sin JPA/Hibernate**) y email.
- `infrastructure/entrypoint/rest/` — el único entrypoint vivo.
- `infrastructure/entrypoint/desktop/**` (CLI + "controller") es **código muerto**: sin
  anotaciones Spring, nunca se instancia, sin contenedor de dependencias. Se conserva como
  referencia. No lo conectes ni trates `UserController` como un endpoint. Tiene tests
  (`UserControllerTest`) que pasan por instanciación directa.

Los mappers son clases escritas a mano (`UserApplicationMapper`, `UserRestMapper`,
`UserPersistenceMapper`, `UserDesktopMapper`). No hay MapStruct. El
`infrastructure/.../entity/UserEntity` es un `record` normal, no una entidad JPA.

## Trampas de configuración

- `application.properties` **excluye** `DataSourceAutoConfiguration`,
  `MailSenderAutoConfiguration` y `UserDetailsServiceAutoConfiguration`. Los beans de `DataSource`
  y JavaMail se construyen a mano en `infrastructure/config/DataSourceSpringConfig.java` y
  `SmtpSpringConfig.java` con claves propias `db.*` / `smtp.*`. **No** migres a
  `spring.datasource.*` / `spring.mail.*`: eso reintroduce el conflicto que las exclusiones evitan.
- `schema.sql` **no** se aplica solo (base de datos no embebida, sin `spring.sql.init.mode`).
  Hay que aplicarlo a mano; `compose.yaml` lo monta en `/docker-entrypoint-initdb.d/001-schema.sql`.
  Tiene `CREATE DATABASE crud_usuarios` hardcodeado, así que con otro `DB_NAME` la base nunca se crea.
- La fila semilla de `schema.sql` (`admin@example.com`) tiene un **hash placeholder que no es
  BCrypt**. La contraseña documentada `Admin1234!` no funciona; reemplaza el hash antes de esperar
  que ese login funcione.
- Todas las credenciales vienen de variables de entorno (`DB_*`, `SMTP_*`, `JWT_SECRET`, `PORT`).
  `.env`, `.env.local`, `.run/` y `*~` están en `.gitignore` por eso. Nunca escribas secretos en
  `application.properties`, `compose.yaml` ni `render.yaml`.
- `JWT_SECRET` debe ser Base64 de al menos 32 bytes aleatorios. En `application.properties` hay un
  valor por defecto solo para desarrollo; `render.yaml` usa `generateValue: true`.
- `lipermi` (`com.tascape.qa`) está declarado en el `pom.xml` pero no se referencia en `src`. No
  está en uso: no asumas que está cableado en algún lado, pero tampoco lo borres por tu cuenta.
- Hay **dos** motores soportados, seleccionados por `db.dialect` / `DB_DIALECT` (`mysql` por
  defecto, `postgresql`). `UserRepositoryMySQL` y `UserRepositoryPostgres` implementan los mismos
  seis puertos *out* y se distinguen por `@ConditionalOnProperty`; ambos extienden
  `AbstractUserRepository`, que concentra el SQL común. El DDL va en `schema.sql` (MySQL) o
  `schema-postgres.sql` (PostgreSQL). Añadir un tercer motor implica tocar esos tres sitios.
- `DatabaseConfig` construye la URL JDBC según el dialecto: es el único lugar donde vive el
  formato de conexión. `DataSourceSpringConfig` inyecta `db.dialect` con `@Value`.
- `schema.sql` y `schema-postgres.sql` **no son intercambiables**: ENUM en línea, `ENGINE=InnoDB`,
  `COLLATE` y `ON UPDATE CURRENT_TIMESTAMP` no existen en PostgreSQL.
- Compose expone `mysql` (3306) y `postgres` (5432), cada uno con su healthcheck. El servicio
  `api` sigue apuntando a MySQL; para PostgreSQL hay que exportar `DB_DIALECT=postgresql` y las
  credenciales de PostgreSQL antes de arrancarla.
- El puerto del host de PostgreSQL es `${POSTGRES_PORT_LOCAL:-5432}`. Si ya tienes un PostgreSQL
  nativo en el 5432, levántalo con `POSTGRES_PORT_LOCAL=5433`; si no, contenedor y nativo se pelean
  el puerto y aparece un error de autenticación engañoso que no tiene nada que ver con la
  contraseña.
- **La URL JDBC de PostgreSQL necesita `stringtype=unspecified`** y es obligatorio, no cosmético: el
  repositorio enlaza con `setString`, que el driver envía como `varchar`, y PostgreSQL rechaza el
  INSERT contra columnas ENUM con `column "role" is of type user_role but expression is of type
  character varying`. No lo quites al "limpiar" la URL.
- `schema-postgres.sql` usa ENUM nativos (`user_role`, `user_status`). El archivo incluye al final
  la variante `VARCHAR` + `CHECK` equivalente, por si prefieres evitar el casteo.

## Seguridad

La autorización vive **solo** en `infrastructure/security/SecurityConfig.java`: no hay
`@PreAuthorize` ni `@Secured` en ninguna parte. Cambia las reglas ahí, no en los controladores.

| Petición | Acceso |
|---|---|
| `POST /api/auth/login`, `POST /api/users` | público |
| `GET /api/users`, `GET /api/users/**` | `ADMIN` o `REVIEWER` |
| `PUT /api/users/**`, `DELETE /api/users/**` | solo `ADMIN` |
| `/swagger-ui/**`, `/swagger-ui.html`, `/v3/api-docs/**` | público |

- El JWT guarda el rol en el claim `role` **sin** prefijo; `JwtAuthenticationFilter` antepone
  `ROLE_` al construir la autoridad. Mantén ambos lados sincronizados si tocas esto.
- El login exige `UserStatus.ACTIVE`; cualquier otro estado lanza `InvalidCredentialsException` (401).
- CSRF, HTTP Basic, form login y sesiones están todos desactivados: solo bearer tokens sin estado.

## Comportamiento a tener en cuenta antes de cambiar cosas

- **Los servicios no son transaccionales.** No hay `@Transactional` en ningún sitio.
  `CreateUserService` y `UpdateUserService` escriben en la base de datos y luego llaman a
  `EmailNotificationService`, cuyo `sendOrLog` hace log WARN y **vuelve a lanzar**. Así que una
  caída de SMTP hace fallar la petición *después* de que la fila del usuario ya está confirmada.
  Es el comportamiento observado e intencionado; no des por sentada la atomicidad.
- Las excepciones se traducen a HTTP en
  `infrastructure/entrypoint/rest/advice/GlobalExceptionHandler.java` (404 / 409 / 401 / 400 / 500).
  Una excepción de dominio nueva necesita un handler ahí o saldrá como 500.
- El cuerpo de los emails usa sustitución ingenua de `{{token}}` sobre
  `resources/templates/user-*.html`; no hay motor de plantillas.
- **No se puede obtener un JWT solo por la API.** `UserModel.create()` fija `UserStatus.PENDING` y
  `CreateUserRestRequest` no acepta `status`, así que todo usuario creado vía `POST /api/users`
  nace `PENDING`, y `LoginService` solo acepta `ACTIVE`. `activate()` existe pero únicamente se
  llama desde los tests. Y el admin de `schema.sql` tiene hash placeholder. La única salida es
  `UPDATE users SET status = 'ACTIVE'` contra la base de datos; cambiarlo a `ACTIVE` por API
  exigiría un token `ADMIN` que aún no existe. Si alguien "arregla" `UserModel.create()` para
  crear en `ACTIVE`, cambia la semántica de negocio: pídelo antes, no lo hagas de pasada.
- `AbstractUserRepository` es el SQL compartido por MySQL y PostgreSQL. El SQL es ANSI a propósito
  (marcadores `?`, `NOW()`, `LIMIT 1`): si añades una sentencia, no introduzcas nada específico de
  un motor aquí; la diferenciación va en el DDL y en `DatabaseConfig`.

## Particularidades de los tests

- **No existen tests de integración.** No hay `@SpringBootTest`, ni Testcontainers, ni
  `@DataJpaTest`. `UserRepositoryMySQLTest` es Mockito puro pese al nombre: mockea el `DataSource`.
  Nada del suite necesita MySQL, SMTP ni red.
- `src/test/resources/application.properties` fija `spring.main.web-application-type=none` para
  que los tests Mockito puros no levanten contexto web. Por eso los slices `@WebMvcTest` deben
  sobreescribirlo con `properties = "spring.main.web-application-type=servlet"` (ver
  `UserRestControllerTest`); consérvalo al añadir tests de slice web.
- Los slices web usan `@Import(SecurityConfig.class, JwtAuthenticationFilter.class, ...)` más
  `@MockBean` para **cada** caso de uso y para `JwtTokenService`; un `@MockBean` que falte rompe
  la carga del contexto, no la aserción.
- Convención en tests: `@DisplayName` en español, `@ExtendWith(MockitoExtension.class)` en los
  unitarios, bloques de comentarios Arrange/Act/Assert.
- `UserRepositoryDialectSelectionTest` es el test que protege la ausencia de colisión de beans
  entre los dos adaptadores de persistencia. Si añades un tercer motor o tocas los
  `@ConditionalOnProperty`, ese test debe seguir verde.
- Para probar de verdad contra PostgreSQL no hay tests automatizados: levanta el servicio de compose
  y lanza la app con `DB_DIALECT=postgresql`. Conviene saber que un JSON mal formado devuelve
  **500** y no 400, porque `GlobalExceptionHandler` no maneja `HttpMessageNotReadableException` y cae
  en el `catch (Exception)` genérico. Si ves 500 instantáneo sin log, sospecha del body, no de la
  base de datos.

## Despliegue

- `render.yaml`: runtime Docker, autodespliegue en cada commit a `main`, health check en
  `/v3/api-docs`. Los secretos van como `sync: false` / `generateValue` y se rellenan en el panel
  de Render.
- El `Dockerfile` compila con `maven:3.9.11-eclipse-temurin-17` usando `-DskipTests` y ejecuta
  como usuario no root `spring`. Los tests **no** son una puerta de despliegue: verifica en local
  antes de hacer push a `main`.
- `docker compose up` levanta MySQL 8.4 (esperado por healthcheck) y la API en `:8080`; deja antes
  `SMTP_USERNAME` / `SMTP_PASSWORD` / `SMTP_FROM_ADDRESS` / `JWT_SECRET` en un archivo `.env`.
