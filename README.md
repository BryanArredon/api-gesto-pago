# Prueba Servicios

API Spring Boot (Java 17) con catálogos GestoPago/PuntoRed y autenticación JWT.

## Requisitos

- JDK 17 (toolchain; el build usa `sh gradlew` porque `gradlew` no es ejecutable y el JDK por defecto del sistema es 26).
- PostgreSQL y Redis. Para los tests de integración:

```bash
docker compose -f docker-compose-it.yml up -d   # Postgres en 5434
# Redis: contenedor redis:8 en localhost:6379 (ya existente)
```

## Variables de entorno (`.env`)

El proyecto carga `.env` con `springboot3-dotenv`. Variables requeridas:

- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- `GESTOPAGO_AUTH_URL`, `GESTOPAGO_AUTH_ID_DISTRIBUIDOR`, `GESTOPAGO_AUTH_CODIGO_DISPOSITIVO`, `GESTOPAGO_AUTH_PASSWORD`
- `JWT_SECRET` (mínimo 32 bytes para HS256)
- `ADMIN_EMAIL`, `ADMIN_PASSWORD` (crea el administrador inicial si no existe)
- Opcionales: `REDIS_HOST`, `REDIS_PORT`, `CORS_ALLOWED_ORIGINS`, `RATE_LIMIT_*`, `JWT_ACCESS_TTL_MINUTES`, `JWT_REFRESH_TTL_DAYS`

## Autenticación (JWT)

- `POST /auth/login` — body `{ "email": "...", "password": "..." }` → `accessToken` (15 min) + `refreshToken` (7 días).
- `POST /auth/refresh` — body `{ "refreshToken": "..." }` → rota el refresh token (el anterior queda revocado).
- `POST /auth/logout` — header `Authorization: Refresh <token>` → revoca el refresh token.
- Protección: `/catalog/**` y resto de la API requieren `Authorization: Bearer <accessToken>`; `/admin/**` exige rol `ADMIN`. `/auth/*`, Swagger y `/actuator/health` son públicos.
- Rate limiting fail-open: primario Redis (`rate:login:<ip>:<ventana>`, `rate:api:<usuario>:<ventana>`), fallback PostgreSQL (`rate_limit_hits`), y si ambos fallan se permite la petición.
- Errores: códigos `AUTH-001..005`, `PERM-001`, `RATE-001`, `VAL-001`, `UNK-001` en `ErrorResponse`.

## Tests

```bash
sh gradlew test
```

- Unitarios: `JwtServiceTest`, `AuthServiceImplTest`.
- Integración (`AuthFlowIntegrationTest`, perfil `it`): requiere Postgres 5434 (docker-compose-it.yml) y Redis 6379.

## Swagger

`http://localhost:8080/swagger-ui.html` (esquema `bearerAuth`).