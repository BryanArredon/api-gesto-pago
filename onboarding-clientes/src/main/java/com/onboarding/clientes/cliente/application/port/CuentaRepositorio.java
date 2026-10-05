package com.onboarding.clientes.cliente.application.port;

import com.onboarding.clientes.cliente.domain.model.Cuenta;
import com.onboarding.clientes.cliente.domain.model.EstatusCuenta;
import com.onboarding.clientes.cliente.domain.valueobject.NumeroCuenta;
import com.onboarding.clientes.shared.web.Cursor;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de persistencia de cuentas bancarias. */
public interface CuentaRepositorio {

    /**
     * Reserva el siguiente numero de cuenta valido (9 digitos de secuencia + verificador Luhn).
     *
     * <p>Lo delega en la base de datos (secuencia transaccional) para que dos altas concurrentes jamas
     * obtengan el mismo numero sin necesidad de bloqueos en la aplicacion.
     *
     * @return numero de cuenta asignado
     */
    NumeroCuenta siguienteNumero();

    Optional<Cuenta> buscarPorNumero(NumeroCuenta numero);

    List<Cuenta> buscarPorCliente(UUID clienteId);

    /** @return cuentas en estatus ACTIVA, paginadas por cursor */
    List<Cuenta> listarActivas(Cursor cursor, int limite);

    long contarPorEstatus(EstatusCuenta estatus);
}
