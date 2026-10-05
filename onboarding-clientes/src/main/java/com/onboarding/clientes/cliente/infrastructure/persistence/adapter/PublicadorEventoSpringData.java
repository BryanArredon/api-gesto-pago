package com.onboarding.clientes.cliente.infrastructure.persistence.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onboarding.clientes.cliente.infrastructure.persistence.entity.EventoDominioEntity;
import com.onboarding.clientes.cliente.infrastructure.persistence.jpa.EventoDominioSpringDataRepository;
import com.onboarding.clientes.shared.evento.EventoDominio;
import com.onboarding.clientes.shared.evento.PublicadorEvento;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Publicador y persistidor de eventos de dominio en la tabla de auditoria append-only.
 */
@Component
public class PublicadorEventoSpringData implements PublicadorEvento {

    private static final Logger LOGGER = LoggerFactory.getLogger(PublicadorEventoSpringData.class);

    private final EventoDominioSpringDataRepository repository;
    private final ObjectMapper objectMapper;

    public PublicadorEventoSpringData(EventoDominioSpringDataRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publicar(EventoDominio evento) {
        try {
            EventoDominioEntity entity = new EventoDominioEntity();
            entity.setId(evento.id() != null ? evento.id() : UUID.randomUUID());
            entity.setTipo(evento.tipo());
            entity.setAgregadoTipo(evento.agregadoTipo());
            entity.setAgregadoId(evento.agregadoId());
            entity.setCargaUtil(objectMapper.writeValueAsString(evento.carga()));
            entity.setCorrelationId(evento.correlationId());
            entity.setActor(evento.actor());
            entity.setCreadoEn(evento.instante());

            repository.save(entity);
            LOGGER.debug("Evento de dominio persistido: tipo={} agregadoId={}", evento.tipo(), evento.agregadoId());
        } catch (Exception e) {
            LOGGER.error("Error al persistir evento de dominio: {}", evento.tipo(), e);
        }
    }
}
