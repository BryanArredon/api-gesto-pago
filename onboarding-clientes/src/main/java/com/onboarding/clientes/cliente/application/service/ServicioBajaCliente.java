package com.onboarding.clientes.cliente.application.service;

import com.onboarding.clientes.cliente.application.port.ClienteRepositorio;
import com.onboarding.clientes.cliente.domain.exception.ClienteNoEncontradoException;
import com.onboarding.clientes.cliente.domain.model.Cliente;
import com.onboarding.clientes.shared.evento.EventoDominio;
import com.onboarding.clientes.shared.evento.PublicadorEvento;
import com.onboarding.clientes.shared.tiempo.Reloj;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de aplicacion para la baja logica de clientes.
 */
@Service
public class ServicioBajaCliente {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServicioBajaCliente.class);

    private final ClienteRepositorio clientes;
    private final PublicadorEvento publicadorEvento;
    private final Reloj reloj;

    public ServicioBajaCliente(
            ClienteRepositorio clientes,
            PublicadorEvento publicadorEvento,
            Reloj reloj) {
        this.clientes = clientes;
        this.publicadorEvento = publicadorEvento;
        this.reloj = reloj;
    }

    @Transactional
    public void darDeBaja(UUID id) {
        Cliente cliente = clientes.buscarPorId(id)
                .orElseThrow(() -> new ClienteNoEncontradoException(id));

        Instant momento = reloj.ahora();
        cliente.darDeBaja(momento);
        clientes.guardar(cliente);

        publicadorEvento.publicar(EventoDominio.crear(
                "cliente.dado_de_baja",
                EventoDominio.TIPO_CLIENTE,
                cliente.id(),
                Map.of("curp", cliente.curp().valor(), "fechaBaja", momento.toString()),
                "sistema",
                momento));

        LOGGER.info("Cliente dado de baja logica: id={}", cliente.id());
    }
}
