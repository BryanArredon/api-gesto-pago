# ==============================================================================
# Etapa 1: Build con Maven y Eclipse Temurin 21
# ==============================================================================
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# 1. Copiar descriptor del proyecto para cachear dependencias
COPY pom.xml .

# 2. Descargar dependencias en modo offline
RUN mvn dependency:go-offline -B || true

# 3. Copiar codigo fuente y compilar artefacto
COPY src ./src
RUN mvn clean package -DskipTests -B

# ==============================================================================
# Etapa 2: Runtime ligero, seguro y sin privilegios de root
# ==============================================================================
FROM eclipse-temurin:21-jre-alpine AS runner

WORKDIR /app

# Crear usuario no privilegiado
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copiar el jar compilado desde el builder
COPY --from=builder /build/target/onboarding-clientes.jar app.jar

# Cambiar permisos al usuario no root
RUN chown -R appuser:appgroup /app
USER appuser

# Exponer el puerto del servicio
EXPOSE 8080

# Variables de entorno por defecto
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:+UseZGC -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

# Healthcheck usando Actuator
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
