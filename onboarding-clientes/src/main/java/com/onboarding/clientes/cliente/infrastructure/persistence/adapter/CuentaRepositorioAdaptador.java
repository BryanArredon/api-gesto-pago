package com.onboarding.clientes.cliente.infrastructure.persistence.adapter;

import com.onboarding.clientes.cliente.application.port.CuentaRepositorio;
import com.onboarding.clientes.cliente.domain.model.Cuenta;
import com.onboarding.clientes.cliente.domain.model.EstatusCuenta;
import com.onboarding.clientes.cliente.domain.model.TipoCuenta;
import com.onboarding.clientes.cliente.domain.valueobject.NumeroCuenta;
import com.onboarding.clientes.cliente.infrastructure.persistence.entity.CuentaEntity;
import com.onboarding.clientes.cliente.infrastructure.persistence.jpa.CuentaSpringDataRepository;
import com.onboarding.clientes.shared.dinero.Dinero;
import com.onboarding.clientes.shared.web.Cursor;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * Adaptador JPA para el puerto CuentaRepositorio.
 */
@Component
public class CuentaRepositorioAdaptador implements CuentaRepositorio {

    private final CuentaSpringDataRepository cuentaRepository;

    public CuentaRepositorioAdaptador(CuentaSpringDataRepository cuentaRepository) {
        this.cuentaRepository = cuentaRepository;
    }

    @Override
    public NumeroCuenta siguienteNumero() {
        String num = cuentaRepository.siguienteNumeroCuenta();
        return NumeroCuenta.de(num.trim());
    }

    @Override
    public Optional<Cuenta> buscarPorNumero(NumeroCuenta numero) {
        return cuentaRepository.findByNumeroCuenta(numero.valor()).map(this::aDominio);
    }

    @Override
    public List<Cuenta> buscarPorCliente(UUID clienteId) {
        return cuentaRepository.findByClienteId(clienteId).stream().map(this::aDominio).toList();
    }

    @Override
    public List<Cuenta> listarActivas(Cursor cursor, int limite) {
        Instant cursorTime = cursor != null ? cursor.marcaTiempo() : null;
        UUID cursorId = cursor != null ? cursor.id() : null;

        List<CuentaEntity> entities = cuentaRepository.listarActivasConCursor(
                cursorTime, cursorId, PageRequest.of(0, limite + 1));
        return entities.stream().map(this::aDominio).toList();
    }

    @Override
    public long contarPorEstatus(EstatusCuenta estatus) {
        return cuentaRepository.countByEstatus(estatus.name());
    }

    private Cuenta aDominio(CuentaEntity c) {
        return Cuenta.rehidratar(
                c.getId(),
                c.getClienteId(),
                c.getNumeroCuenta() != null ? NumeroCuenta.de(c.getNumeroCuenta()) : null,
                TipoCuenta.valueOf(c.getTipoCuenta()),
                Dinero.de(c.getSaldo(), c.getMoneda()),
                EstatusCuenta.valueOf(c.getEstatus()),
                c.getVersion() != null ? c.getVersion() : 0L,
                c.getCreatedAt(),
                c.getUpdatedAt());
    }
}
