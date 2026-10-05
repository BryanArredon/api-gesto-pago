package com.onboarding.clientes.cliente.application.service;

import com.onboarding.clientes.cliente.application.dto.CuentaDto;
import com.onboarding.clientes.cliente.application.dto.SaldoCuentaDto;
import com.onboarding.clientes.cliente.application.mapper.ClienteMapeador;
import com.onboarding.clientes.cliente.application.port.CuentaRepositorio;
import com.onboarding.clientes.cliente.domain.exception.CuentaNoEncontradaException;
import com.onboarding.clientes.cliente.domain.model.Cuenta;
import com.onboarding.clientes.cliente.domain.valueobject.NumeroCuenta;
import com.onboarding.clientes.config.PropiedadesOnboarding;
import com.onboarding.clientes.shared.web.Cursor;
import com.onboarding.clientes.shared.web.PaginaRespuesta;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de aplicacion para la consulta de cuentas bancarias.
 */
@Service
@Transactional(readOnly = true)
public class ServicioConsultaCuenta {

    private final CuentaRepositorio cuentas;
    private final ClienteMapeador mapeador;
    private final PropiedadesOnboarding propiedades;

    public ServicioConsultaCuenta(
            CuentaRepositorio cuentas,
            ClienteMapeador mapeador,
            PropiedadesOnboarding propiedades) {
        this.cuentas = cuentas;
        this.mapeador = mapeador;
        this.propiedades = propiedades;
    }

    public CuentaDto buscarPorNumero(String numeroCuentaTexto) {
        NumeroCuenta numero = NumeroCuenta.de(numeroCuentaTexto);
        return cuentas.buscarPorNumero(numero)
                .map(mapeador::aDto)
                .orElseThrow(() -> new CuentaNoEncontradaException(numero));
    }

    public SaldoCuentaDto consultarSaldo(String numeroCuentaTexto) {
        NumeroCuenta numero = NumeroCuenta.de(numeroCuentaTexto);
        Cuenta cuenta = cuentas.buscarPorNumero(numero)
                .orElseThrow(() -> new CuentaNoEncontradaException(numero));
        return new SaldoCuentaDto(
                cuenta.numeroCuenta(),
                cuenta.saldo().moneda(),
                cuenta.saldo().monto(),
                cuenta.estatus());
    }

    public PaginaRespuesta<CuentaDto> listarActivas(Cursor cursor, Integer limiteSolicitado) {
        int limite = normalizarLimite(limiteSolicitado);
        List<Cuenta> elementos = cuentas.listarActivas(cursor, limite);
        return PaginaRespuesta.construir(
                elementos,
                limite,
                mapeador::aDto,
                cuenta -> new Cursor(cuenta.createdAt(), cuenta.id()));
    }

    private int normalizarLimite(Integer solicitado) {
        PropiedadesOnboarding.Paginacion pag = propiedades.paginacion();
        if (solicitado == null || solicitado <= 0) {
            return pag.limitePorDefecto();
        }
        return Math.min(solicitado, pag.limiteMaximo());
    }
}
