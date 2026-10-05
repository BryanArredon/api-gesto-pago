package com.onboarding.clientes.cliente.domain.model;

import com.onboarding.clientes.cliente.domain.valueobject.Correo;
import com.onboarding.clientes.shared.error.CodigoError;
import com.onboarding.clientes.shared.error.ErrorNegocio;
import com.onboarding.clientes.shared.id.UuidV7;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Usuario de acceso del cliente (relacion 1:1 con {@link Cliente}).
 *
 * <p>Solo guarda el hash de la contrasena y del PIN: el texto plano nunca existe mas alla del borde de la
 * peticion. El campo {@code hashContrasena} no tiene setter publico; la unica via para cambiarlo es
 * {@link #cambiarContrasena(String)}, que ademas limpia el bloqueo por intentos fallidos.
 *
 * <p>Incluye proteccion contra fuerza bruta: tras {@code maxIntentos} fallidos la cuenta queda bloqueada
 * durante {@code duracionBloqueo}, de modo que un atacante no puede probar contrasenas indefinidamente.
 */
public class Usuario {

    /** Numero maximo de intentos antes de bloquear la cuenta. */
    public static final int MAX_INTENTOS = 5;

    /** Duracion del bloqueo por intentos fallidos. */
    public static final Duration DURACION_BLOQUEO = Duration.ofMinutes(15);

    private final UUID id;
    private final UUID clienteId;
    private final Correo correo;
    private String hashContrasena;
    private String hashPin;
    private boolean activo;
    private int intentosFallidos;
    private Instant bloqueadoHasta;
    private Instant ultimoAcceso;
    private String keycloakSub;
    private final Instant createdAt;
    private Instant updatedAt;

    private Usuario(
            UUID id,
            UUID clienteId,
            Correo correo,
            String hashContrasena,
            String hashPin,
            boolean activo,
            int intentosFallidos,
            Instant bloqueadoHasta,
            Instant ultimoAcceso,
            String keycloakSub,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.clienteId = clienteId;
        this.correo = correo;
        this.hashContrasena = hashContrasena;
        this.hashPin = hashPin;
        this.activo = activo;
        this.intentosFallidos = intentosFallidos;
        this.bloqueadoHasta = bloqueadoHasta;
        this.ultimoAcceso = ultimoAcceso;
        this.keycloakSub = keycloakSub;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** Crea el usuario en el alta del cliente: activo y sin PIN (el PIN es opcional). */
    public static Usuario crear(UUID clienteId, Correo correo, String hashContrasena, String hashPin, Instant momento) {
        return new Usuario(
                UuidV7.generar(), clienteId, correo, hashContrasena, hashPin, true, 0, null, null, null, momento, momento);
    }

    public static Usuario rehidratar(
            UUID id,
            UUID clienteId,
            Correo correo,
            String hashContrasena,
            String hashPin,
            boolean activo,
            int intentosFallidos,
            Instant bloqueadoHasta,
            Instant ultimoAcceso,
            String keycloakSub,
            Instant createdAt,
            Instant updatedAt) {
        return new Usuario(
                id,
                clienteId,
                correo,
                hashContrasena,
                hashPin,
                activo,
                intentosFallidos,
                bloqueadoHasta,
                ultimoAcceso,
                keycloakSub,
                createdAt,
                updatedAt);
    }

    public void cambiarContrasena(String nuevoHash) {
        if (nuevoHash == null || nuevoHash.isBlank()) {
            throw new ErrorNegocio(CodigoError.CONTRASENA_INVALIDA, "El hash de contrasena es obligatorio");
        }
        this.hashContrasena = nuevoHash;
        this.intentosFallidos = 0;
        this.bloqueadoHasta = null;
    }

    public void establecerPin(String nuevoHash) {
        if (nuevoHash == null || nuevoHash.isBlank()) {
            throw new ErrorNegocio(CodigoError.CONTRASENA_INVALIDA, "El hash de PIN es obligatorio");
        }
        this.hashPin = nuevoHash;
    }

    public void quitarPin() {
        this.hashPin = null;
    }

    /** Registra un intento de acceso fallido y bloquea la cuenta al superar el maximo permitido. */
    public void registrarIntentoFallido(Instant momento) {
        this.intentosFallidos++;
        if (this.intentosFallidos >= MAX_INTENTOS) {
            this.bloqueadoHasta = momento.plus(DURACION_BLOQUEO);
        }
    }

    /** Registra un acceso exitoso: reinicia el contador de intentos. */
    public void registrarAccesoExitoso(Instant momento) {
        this.intentosFallidos = 0;
        this.bloqueadoHasta = null;
        this.ultimoAcceso = momento;
    }

    /** Baja logica: el cliente dado de baja deja de poder autenticarse. */
    public void darDeBaja() {
        this.activo = false;
        this.intentosFallidos = 0;
        this.bloqueadoHasta = null;
    }

    public void reactivar() {
        this.activo = true;
    }

    public void vincularConKeycloak(String subject) {
        this.keycloakSub = subject;
    }

    public void desbloquear() {
        this.bloqueadoHasta = null;
        this.intentosFallidos = 0;
    }

    public boolean estaBloqueado(Instant momento) {
        return bloqueadoHasta != null && momento.isBefore(bloqueadoHasta);
    }

    public void exigeAccesoPermitido(Instant momento) {
        if (!activo) {
            throw new ErrorNegocio(CodigoError.USUARIO_INACTIVO, "El usuario se encuentra inactivo");
        }
        if (estaBloqueado(momento)) {
            throw new ErrorNegocio(
                    CodigoError.USUARIO_BLOQUEADO,
                    "El usuario esta bloqueado temporalmente por intentos fallidos. Intentelo despues de "
                            + bloqueadoHasta);
        }
    }

    public UUID id() {
        return id;
    }

    public UUID clienteId() {
        return clienteId;
    }

    public Correo correo() {
        return correo;
    }

    public String hashContrasena() {
        return hashContrasena;
    }

    public String hashPin() {
        return hashPin;
    }

    public boolean tienePin() {
        return hashPin != null;
    }

    public boolean activo() {
        return activo;
    }

    public int intentosFallidos() {
        return intentosFallidos;
    }

    public Instant bloqueadoHasta() {
        return bloqueadoHasta;
    }

    public Instant ultimoAcceso() {
        return ultimoAcceso;
    }

    public String keycloakSub() {
        return keycloakSub;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    /** Marca de auditoria que actualiza el adaptador de persistencia al guardar. */
    public void marcarActualizada(Instant momento) {
        this.updatedAt = momento;
    }
}
