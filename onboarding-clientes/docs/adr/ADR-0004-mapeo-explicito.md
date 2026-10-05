# ADR-0004: Mapeo Explícito y Controlado sin Frameworks Ocultos

## Estado
Aceptado

## Contexto
El uso de herramientas de mapeo automático como ModelMapper o MapStruct suele requerir que el dominio adopte convenciones JavaBeans (`getNombre()`, `setNombre()`), perdiendo nombres semánticos de negocio (`nombre()`, `curp()`, `tieneNumero()`).

## Decisión
Implementar un `ClienteMapeador` explícito escrito a mano en la capa de aplicación.

## Consecuencias
- **Positivas**: Control absoluto del flujo de datos, tipos seguros en tiempo de compilación, cero reflexiones o generadores de código opacos, el dominio mantiene sus métodos semánticos puros.
