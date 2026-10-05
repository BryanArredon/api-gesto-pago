package com.onboarding.clientes.cliente.application.port;

import com.onboarding.clientes.cliente.domain.model.Cliente;
import com.onboarding.clientes.cliente.domain.valueobject.Correo;
import com.onboarding.clientes.cliente.domain.valueobject.Curp;
import com.onboarding.clientes.cliente.domain.valueobject.Rfc;
import com.onboarding.clientes.shared.web.Cursor;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de persistencia del agregado Cliente.
 *
 * <p>Definido en la capa de aplicacion y no en el dominio porque el dominio no necesita saber si el
 * almacenamiento es una base relacional, un documento o una API. La implementacion JPA vive en
 * {@code infrastructure}.
 */
public interface ClienteRepositorio {

    /**
     * Persiste el agregado completo (cliente, domicilio, usuario y cuenta) dentro de la transaccion activa.
     *
     * @param cliente agregado a persistir
     * @return el agregado persistido, con los valores generados por la base (numero de cuenta, auditoria)
     */
    Cliente guardar(Cliente cliente);

    Optional<Cliente> buscarPorId(UUID id);

    Optional<Cliente> buscarPorCurp(Curp curp);

    Optional<Cliente> buscarPorRfc(Rfc rfc);

    Optional<Cliente> buscarPorCorreo(Correo correo);

    /**
     * Pagina de clientes por cursor, del mas reciente al mas antiguo.
     *
     * @param cursor posicion previa, {@code null} para la primera pagina
     * @param limite maximo de elementos (acotado por la implementacion)
     * @param soloActivos si {@code true} excluye los clientes dados de baja
     * @return hasta {@code limite + 1} elementos para poder detectar si hay pagina siguiente
     */
    List<Cliente> listar(Cursor cursor, int limite, boolean soloActivos);

    /** Clientes registrados en un rango de fechas de alta (inclusivo). */
    List<Cliente> listarRegistradosEntre(Instant desde, Instant hasta, Cursor cursor, int limite);

    long contarActivos();

    boolean existePorCurp(Curp curp);

    boolean existePorRfc(Rfc rfc);

    boolean existePorCorreo(Correo correo);
}
