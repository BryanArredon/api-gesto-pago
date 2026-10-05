# ADR-0005: Integración con Keycloak y Autenticación Ligera JWT

## Estado
Aceptado

## Contexto
El requerimiento exige autenticación basada en estándares, compatibilidad con Keycloak y la capacidad de ejecutar un entorno liviano mediante contenedores Docker sin sobrecargar los recursos locales de la máquina.

## Decisión
1. Proveer un servidor de autorización **Keycloak 24+** configurado en modo liviano (`start-dev --import-realm`) con un realm `onboarding` precargado con clientes y roles.
2. Implementar servicio de emisión y validación de tokens JWT estándar (Bearer) en la API con BCrypt (coste 12) para proteger contraseñas.
3. Desacoplar la aplicación del SDK pesado de Keycloak utilizando el estándar OAuth2 Resource Server / JWKS.

## Consecuencias
- **Positivas**: Arranque en segundos en Docker, bajo consumo de memoria RAM (< 512MB), soporte para flujos modernos con PKCE y tokens JWT rotativos.
