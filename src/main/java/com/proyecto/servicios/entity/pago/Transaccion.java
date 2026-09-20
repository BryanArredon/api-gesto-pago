package com.proyecto.servicios.entity.pago;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transacciones")
@Getter
@Setter
public class Transaccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "id_servicio", nullable = false)
    private Integer idServicio;

    @Column(name = "id_producto", nullable = false)
    private Integer idProducto;

    @Column(name = "servicio", nullable = false, length = 256)
    private String servicio;

    @Column(name = "producto", nullable = false, length = 256)
    private String producto;

    @Column(name = "referencia", nullable = false, length = 100)
    private String referencia;

    @Column(name = "monto", nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "comision", nullable = false, precision = 12, scale = 2)
    private BigDecimal comision = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoTransaccion estado;

    @Column(name = "idempotency_key", nullable = false, length = 36)
    private String idempotencyKey;

    @Column(name = "upc", length = 15)
    private String upc;

    @Column(name = "unidad", length = 25)
    private String unidad;

    @Column(name = "numero_autorizacion", length = 60)
    private String numeroAutorizacion;

    @Column(name = "id_tx", length = 40)
    private String idTx;

    @Column(name = "pin", columnDefinition = "TEXT")
    private String pin;

    @Column(name = "legend", columnDefinition = "TEXT")
    private String legend;

    @Column(name = "error_mensaje", columnDefinition = "TEXT")
    private String errorMensaje;

    @Column(name = "fecha")
    private Instant fecha;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}