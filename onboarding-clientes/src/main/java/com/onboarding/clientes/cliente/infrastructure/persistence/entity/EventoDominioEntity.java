package com.onboarding.clientes.cliente.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Entidad JPA que mapea la tabla evento_dominio. */
@Entity
@Table(name = "evento_dominio")
public class EventoDominioEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tipo", length = 80, nullable = false)
    private String tipo;

    @Column(name = "agregado_tipo", length = 40, nullable = false)
    private String agregadoTipo;

    @Column(name = "agregado_id", nullable = false)
    private UUID agregadoId;

    @Column(name = "carga_util", columnDefinition = "jsonb", nullable = false)
    private String cargaUtil;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "actor", length = 120)
    private String actor;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    public EventoDominioEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getAgregadoTipo() { return agregadoTipo; }
    public void setAgregadoTipo(String agregadoTipo) { this.agregadoTipo = agregadoTipo; }
    public UUID getAgregadoId() { return agregadoId; }
    public void setAgregadoId(UUID agregadoId) { this.agregadoId = agregadoId; }
    public String getCargaUtil() { return cargaUtil; }
    public void setCargaUtil(String cargaUtil) { this.cargaUtil = cargaUtil; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }
    public Instant getCreadoEn() { return creadoEn; }
    public void setCreadoEn(Instant creadoEn) { this.creadoEn = creadoEn; }
}
