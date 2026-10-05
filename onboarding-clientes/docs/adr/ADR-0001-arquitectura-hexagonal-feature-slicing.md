# ADR-0001: Arquitectura Hexagonal y Feature Slicing

## Estado
Aceptado

## Contexto
El sistema de Onboarding de Clientes debe garantizar mantenibilidad, aislamiento de la lógica de negocio y escalabilidad. Organizar el código por capas técnicas tradicionales (`controllers/`, `services/`, `repositories/`) tiende a dispersar las reglas de una misma entidad de negocio y acoplar el dominio al framework Spring/JPA.

## Decisión
1. Adoptamos **Arquitectura Hexagonal (Puertos y Adaptadores)** con organización por **Feature Slicing** (`cliente/`, `auth/`, `shared/`).
2. El paquete `domain` no tiene dependencias de Spring ni de JPA; contiene únicamente modelos inmutables, value objects y excepciones puras de Java.
3. El paquete `application` define los puertos de persistencia (`ClienteRepositorio`, etc.), comandos y servicios de casos de uso.
4. El paquete `infrastructure` implementa los adaptadores JPA y persistencia.
5. El paquete `api` maneja la exposición REST HTTP y OpenAPI.

## Consecuencias
- **Positivas**: Pruebas unitarias de dominio puras y ultrarrápidas, independencia total del motor de persistencia, alta cohesión por característica de negocio.
- **Negativas**: Mayor número de clases iniciales (adaptadores y puertos explícitos).
