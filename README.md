# Users Management — Spring Boot, Arquitectura Hexagonal y DDD

Aplicación de gestión de usuarios construida con Java 17 y Spring Boot. La API REST es el punto de entrada activo. El código de la antigua CLI se conserva como adaptador inactivo y no posee un contenedor de dependencias independiente.

Spring es el único *composition root*: `Main` inicia el contexto y las dependencias se resuelven mediante configuración y component scanning de Spring.

## Verificación

```bash
./mvnw clean test
./mvnw clean package
```

En Windows se puede utilizar `mvnw.cmd`.

## Despliegue en Render

El archivo `render.yaml` define el servicio web, el build con Docker y el despliegue
automático de cada commit que llegue a la rama `main`.

1. Crear un Blueprint en Render y seleccionar este repositorio.
2. Completar en el panel los secretos marcados como requeridos: `DB_HOST`,
   `DB_USERNAME`, `DB_PASSWORD`, `SMTP_USERNAME`, `SMTP_PASSWORD` y
   `SMTP_FROM_ADDRESS`.
3. Usar una instancia MySQL accesible desde Internet o desde la red privada de
   Render y ejecutar `src/main/resources/schema.sql` una vez para crear el esquema.
4. Desplegar el Blueprint. La API quedará disponible en el subdominio
   `onrender.com` asignado por Render y Swagger UI en `/swagger-ui.html`.

Las credenciales nunca deben guardarse en `application.properties` ni en
`render.yaml`. Para desarrollo local, deben proporcionarse como variables de entorno.

## Motores de base de datos

La aplicación admite MySQL y PostgreSQL. El motor se elige con la variable `DB_DIALECT`, que
selecciona cuál de los dos adaptadores de persistencia registra Spring (ambos implementan los
mismos puertos de salida, por lo que solo uno puede estar activo).

| `DB_DIALECT` | Adaptador | DDL a ejecutar | Puerto por defecto |
|---|---|---|---|
| `mysql` (por defecto) | `UserRepositoryMySQL` | `src/main/resources/schema.sql` | 3306 |
| `postgresql` | `UserRepositoryPostgres` | `src/main/resources/schema-postgres.sql` | 5432 |

Las consultas SQL son comunes a ambos motores; lo único específico de cada uno es el DDL y la
URL JDBC.

### MySQL

```bash
docker compose up -d mysql      # aplica schema.sql al iniciar el contenedor
```

### PostgreSQL

```bash
docker compose up -d postgres   # aplica schema-postgres.sql al iniciar el contenedor
```

Para ejecutar la API contra PostgreSQL en local:

```bash
# Linux / macOS
DB_DIALECT=postgresql DB_HOST=localhost DB_PORT=5432 DB_NAME=crud_usuarios \
DB_USERNAME=postgres DB_PASSWORD=local_pg_password ./mvnw spring-boot:run

# Windows (CMD)
set DB_DIALECT=postgresql && set DB_HOST=localhost && set DB_PORT=5432
set DB_NAME=crud_usuarios && set DB_USERNAME=postgres
set DB_PASSWORD=local_pg_password
mvnw.cmd spring-boot:run
```

En Supabase o en cualquier PostgreSQL gestionado, ejecuta `schema-postgres.sql` una sola vez en el
editor SQL y configura `DB_HOST`, `DB_PORT` (5432), `DB_NAME`, `DB_USERNAME` y `DB_PASSWORD` con
los datos que te entregue el proveedor.

## Activar el primer usuario

`POST /api/users` es público, pero todo usuario creado por esa vía nace con `status = PENDING`
(fijado en `UserModel.create()`), y el login solo acepta usuarios `ACTIVE`. Además, el único
usuario `ACTIVE` del `schema.sql` de MySQL trae un hash de ejemplo que no es un BCrypt válido.

Para obtener el primer JWT hay que activar el usuario directamente en la base de datos:

```sql
UPDATE users SET status = 'ACTIVE' WHERE email = 'tu@correo.com';
```

Después, `POST /api/auth/login` con ese mismo correo y contraseña devuelve el token. Crear el
usuario con `"role": "ADMIN"` desde el principio evita tener que pedir permisos de administrador
después.
