# ADR-0002: Uso de UUID v7 para Claves Primarias y Temporalidad

## Estado
Aceptado

## Contexto
Los identificadores autoincrementales (`BIGSERIAL`) exponen secuencias adivinables y provocan contención en inserciones distribuidas. Los UUID v4 tradicionales provocan fragmentación severa en los índices B-Tree al ser completamente aleatorios.

## Decisión
Usar **UUID v7** generado en la capa de aplicación. Los UUID v7 codifican un timestamp Unix en los primeros 48 bits, seguidos de aleatoriedad criptográfica.

## Consecuencias
- **Positivas**: Claves primarias ordenables naturalmente por fecha de creación, excelente rendimiento en índices B-Tree de PostgreSQL, seguras para exponer en APIs REST públicas sin revelar totales transaccionales ni permitir enumeración.
- **Negativas**: Ocupa 16 bytes por registro frente a 8 bytes de un `BIGINT`.
