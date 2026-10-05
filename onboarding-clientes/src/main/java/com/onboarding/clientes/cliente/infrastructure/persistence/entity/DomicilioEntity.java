package com.onboarding.clientes.cliente.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Entidad JPA que mapea la tabla domicilio. */
@Entity
@Table(name = "domicilio")
public class DomicilioEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "cliente_id", nullable = false, unique = true)
    private UUID clienteId;

    @Column(name = "calle", length = 100, nullable = false)
    private String calle;

    @Column(name = "numero_exterior", length = 10, nullable = false)
    private String numeroExterior;

    @Column(name = "numero_interior", length = 10)
    private String numeroInterior;

    @Column(name = "colonia", length = 100, nullable = false)
    private String colonia;

    @Column(name = "municipio", length = 100, nullable = false)
    private String municipio;

    @Column(name = "estado", length = 50, nullable = false)
    private String estado;

    @Column(name = "codigo_postal", length = 5, nullable = false)
    private String codigoPostal;

    @Column(name = "pais", length = 50, nullable = false)
    private String pais;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public DomicilioEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getClienteId() { return clienteId; }
    public void setClienteId(UUID clienteId) { this.clienteId = clienteId; }
    public String getCalle() { return calle; }
    public void setCalle(String calle) { this.calle = calle; }
    public String getNumeroExterior() { return numeroExterior; }
    public void setNumeroExterior(String numeroExterior) { this.numeroExterior = numeroExterior; }
    public String getNumeroInterior() { return numeroInterior; }
    public void setNumeroInterior(String numeroInterior) { this.numeroInterior = numeroInterior; }
    public String getColonia() { return colonia; }
    public void setColonia(String colonia) { this.colonia = colonia; }
    public String getMunicipio() { return municipio; }
    public void setMunicipio(String municipio) { this.municipio = municipio; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getCodigoPostal() { return codigoPostal; }
    public void setCodigoPostal(String codigoPostal) { this.codigoPostal = codigoPostal; }
    public String getPais() { return pais; }
    public void setPais(String pais) { this.pais = pais; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
