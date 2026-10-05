package com.onboarding.clientes.cliente.application.service;

import com.onboarding.clientes.cliente.application.dto.ClienteDetalle;
import com.onboarding.clientes.cliente.application.dto.ClienteResumen;
import com.onboarding.clientes.cliente.application.mapper.ClienteMapeador;
import com.onboarding.clientes.cliente.application.port.ClienteRepositorio;
import com.onboarding.clientes.cliente.application.port.CuentaRepositorio;
import com.onboarding.clientes.cliente.domain.exception.ClienteNoEncontradoException;
import com.onboarding.clientes.cliente.domain.exception.CuentaNoEncontradaException;
import com.onboarding.clientes.cliente.domain.model.Cliente;
import com.onboarding.clientes.cliente.domain.model.Cuenta;
import com.onboarding.clientes.cliente.domain.valueobject.Correo;
import com.onboarding.clientes.cliente.domain.valueobject.Curp;
import com.onboarding.clientes.cliente.domain.valueobject.NumeroCuenta;
import com.onboarding.clientes.cliente.domain.valueobject.Rfc;
import com.onboarding.clientes.config.PropiedadesOnboarding;
import com.onboarding.clientes.shared.web.Cursor;
import com.onboarding.clientes.shared.web.PaginaRespuesta;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de aplicacion para la consulta de clientes.
 */
@Service
@Transactional(readOnly = true)
public class ServicioConsultaCliente {

    private static final String ZONA_OPERATIVA = "America/Mexico_City";

    private final ClienteRepositorio clientes;
    private final CuentaRepositorio cuentas;
    private final ClienteMapeador mapeador;
    private final PropiedadesOnboarding propiedades;

    public ServicioConsultaCliente(
            ClienteRepositorio clientes,
            CuentaRepositorio cuentas,
            ClienteMapeador mapeador,
            PropiedadesOnboarding propiedades) {
        this.clientes = clientes;
        this.cuentas = cuentas;
        this.mapeador = mapeador;
        this.propiedades = propiedades;
    }

    public ClienteDetalle buscarPorId(UUID id) {
        return clientes.buscarPorId(id)
                .map(mapeador::aDetalle)
                .orElseThrow(() -> new ClienteNoEncontradoException(id));
    }

    public ClienteDetalle buscarPorCurp(String curpTexto) {
        Curp curp = Curp.de(curpTexto);
        return clientes.buscarPorCurp(curp)
                .map(mapeador::aDetalle)
                .orElseThrow(() -> new ClienteNoEncontradoException("CURP " + curpTexto));
    }

    public ClienteDetalle buscarPorRfc(String rfcTexto) {
        Rfc rfc = Rfc.de(rfcTexto);
        return clientes.buscarPorRfc(rfc)
                .map(mapeador::aDetalle)
                .orElseThrow(() -> new ClienteNoEncontradoException("RFC " + rfcTexto));
    }

    public ClienteDetalle buscarPorCorreo(String correoTexto) {
        Correo correo = Correo.de(correoTexto);
        return clientes.buscarPorCorreo(correo)
                .map(mapeador::aDetalle)
                .orElseThrow(() -> new ClienteNoEncontradoException("correo " + correoTexto));
    }

    public ClienteDetalle buscarPorNumeroCuenta(String numeroCuentaTexto) {
        NumeroCuenta numero = NumeroCuenta.de(numeroCuentaTexto);
        Cuenta cuenta = cuentas.buscarPorNumero(numero)
                .orElseThrow(() -> new CuentaNoEncontradaException(numero));
        return buscarPorId(cuenta.clienteId());
    }

    public PaginaRespuesta<ClienteResumen> listar(Cursor cursor, Integer limiteSolicitado, boolean soloActivos) {
        int limite = normalizarLimite(limiteSolicitado);
        List<Cliente> elementos = clientes.listar(cursor, limite, soloActivos);
        return PaginaRespuesta.construir(
                elementos,
                limite,
                mapeador::aResumen,
                cliente -> new Cursor(cliente.createdAt(), cliente.id()));
    }

    public PaginaRespuesta<ClienteResumen> listarRegistradosEntre(
            LocalDate desde, LocalDate hasta, Cursor cursor, Integer limiteSolicitado) {
        int limite = normalizarLimite(limiteSolicitado);
        ZoneId zone = ZoneId.of(ZONA_OPERATIVA);
        Instant inicio = desde.atStartOfDay(zone).toInstant();
        Instant fin = hasta.plusDays(1).atStartOfDay(zone).toInstant().minusNanos(1);

        List<Cliente> elementos = clientes.listarRegistradosEntre(inicio, fin, cursor, limite);
        return PaginaRespuesta.construir(
                elementos,
                limite,
                mapeador::aResumen,
                cliente -> new Cursor(cliente.createdAt(), cliente.id()));
    }

    private int normalizarLimite(Integer solicitado) {
        PropiedadesOnboarding.Paginacion pag = propiedades.paginacion();
        if (solicitado == null || solicitado <= 0) {
            return pag.limitePorDefecto();
        }
        return Math.min(solicitado, pag.limiteMaximo());
    }
}
