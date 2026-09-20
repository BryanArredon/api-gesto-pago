package com.proyecto.servicios.entity.pago;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Evento inmutable de una transaccion. Registra cada cambio de estado y su
 * origen. Nunca se edita ni se elimina.
 */
@Entity
@Table(name = "eventos_transaccion", indexes = {
        @Index(name = "idx_eventos_transaccion", columnList = "transaccion_id, created_at")
})
@Getter
@Setter
public class EventoTransaccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaccion_id", nullable = false)
    private Long transaccionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoTransaccion estado;

    @Column(name = "origen", nullable = false, length = 30)
    private String origen;

    @Column(name = "detalle", columnDefinition = "TEXT")
    private String detalle;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}