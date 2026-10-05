# ADR-0003: Inmutabilidad por Defecto con Java 21 Records y Value Objects

## Estado
Aceptado

## Contexto
El uso indiscriminado de entidades mutables y anotaciones Lombok (`@Data`, `@Setter`) produce código propenso a estados inconsistentes y bugs de concurrencia.

## Decisión
1. Usar `record` nativo de Java 21 para todos los DTOs, Comandos, Value Objects y Respuestas de la API.
2. Encapsular conceptos de negocio en Value Objects inmutables (`Curp`, `Rfc`, `Correo`, `Telefono`, `CodigoPostal`, `NumeroCuenta`, `Dinero`).
3. Constructores compactos en records para validación Fail-Fast.

## Consecuencias
- **Positivas**: Inmutabilidad garantizada en tiempo de compilación, código libre de dependencias mágicas de procesamiento de anotaciones, código autodocumentado.
