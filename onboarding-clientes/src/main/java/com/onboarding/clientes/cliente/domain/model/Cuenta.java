package com.onboarding.clientes.cliente.domain.model;

import com.onboarding.clientes.cliente.domain.valueobject.NumeroCuenta;
import com.onboarding.clientes.shared.dinero.Dinero;
import com.onboarding.clientes.shared.id.UuidV7;
import java.time.Instant;
import java.util.UUID;

/**
 * Cuenta bancaria del cliente.
 *
 * <p>Es una entidad dependiente del agregado {@link Cliente}: no tiene identidad propia para el negocio
 * y su ciclo de vida esta atado al del cliente. El numero de cuenta lo asigna la base de datos
 * (secuencia + digito verificador Luhn) para garantizar unicidad sin coordinacion entre instancias; por eso
 * puede ser {@code null} hasta que la cuenta se persiste.
 */
public class Cuenta {

    private final UUID id;
    private final UUID clienteId;
    private NumeroCuenta numeroCuenta;
    private final TipoCuenta tipo;
    private Dinero saldo;
    private EstatusCuenta estatus;
    private final long version;
    private final Instant createdAt;
    private Instant updatedAt;

    private Cuenta(
            UUID id,
            UUID clienteId,
            NumeroCuenta numeroCuenta,
            TipoCuenta tipo,
            Dinero saldo,
            EstatusCuenta estatus,
            long version,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.clienteId = clienteId;
        this.numeroCuenta = numeroCuenta;
        this.tipo = tipo;
        this.saldo = saldo;
        this.estatus = estatus;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** Crea la cuenta con saldo inicial y estatus ACTIVA, como exige el proceso de alta. */
    public static Cuenta abrir(UUID clienteId, TipoCuenta tipo, Dinero saldoInicial, Instant momento) {
        return new Cuenta(UuidV7.generar(), clienteId, null, tipo, saldoInicial, EstatusCuenta.ACTIVA, 0L, momento, momento);
    }

    /** Reconstruye una cuenta ya persistida (carga desde la base de datos). */
    public static Cuenta rehidratar(
            UUID id,
            UUID clienteId,
            NumeroCuenta numeroCuenta,
            TipoCuenta tipo,
            Dinero saldo,
            EstatusCuenta estatus,
            long version,
            Instant createdAt,
            Instant updatedAt) {
        return new Cuenta(id, clienteId, numeroCuenta, tipo, saldo, estatus, version, createdAt, updatedAt);
    }

    /** Asigna el numero de cuenta generado por la base de datos. */
    public void asignarNumero(NumeroCuenta numero) {
        if (this.numeroCuenta == null) {
            this.numeroCuenta = numero;
        }
    }

    public void inactivar() {
        this.estatus = this.estatus.transicionarA(EstatusCuenta.INACTIVA);
    }

    public void bloquear() {
        this.estatus = this.estatus.transicionarA(EstatusCuenta.BLOQUEADA);
    }

    public void desbloquear() {
        this.estatus = this.estatus.transicionarA(EstatusCuenta.ACTIVA);
    }

    public void cerrar() {
        this.estatus = this.estatus.transicionarA(EstatusCuenta.CERRADA);
    }

    /** Abona un importe. El saldo nunca puede quedar negativo: es una invariante del agregado. */
    public void acreditar(Dinero importe) {
        this.saldo = this.saldo.sumar(importe);
    }

    /** Retira un importe; falla rapido si dejaria la cuenta en negativo. */
    public void debitar(Dinero importe) {
        this.saldo = this.saldo.restar(importe);
    }

    public UUID id() {
        return id;
    }

    public UUID clienteId() {
        return clienteId;
    }

    public NumeroCuenta numeroCuenta() {
        return numeroCuenta;
    }

    public boolean tieneNumero() {
        return numeroCuenta != null;
    }

    public TipoCuenta tipo() {
        return tipo;
    }

    public Dinero saldo() {
        return saldo;
    }

    public EstatusCuenta estatus() {
        return estatus;
    }

    public long version() {
        return version;
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

    public boolean esActiva() {
        return estatus.admiteOperaciones();
    }
}
