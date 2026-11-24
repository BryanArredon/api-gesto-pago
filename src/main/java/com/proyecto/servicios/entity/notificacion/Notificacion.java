package com.proyecto.servicios.entity.notificacion;

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

import java.time.Instant;

@Entity
@Table(name = "notificaciones")
@Getter
@Setter
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoNotificacion tipo = TipoNotificacion.TRANSACCION;

    @Column(name = "titulo", nullable = false, length = 160)
    private String titulo;

    @Column(name = "cuerpo", nullable = false, columnDefinition = "TEXT")
    private String cuerpo;

    @Column(name = "transaccion_id")
    private Long transaccionId;

    @Column(name = "dedupe_key", nullable = false, length = 160)
    private String dedupeKey;

    @Column(name = "datos", columnDefinition = "TEXT")
    private String datos;

    @Column(name = "leida_en")
    private Instant leidaEn;

    @Enumerated(EnumType.STRING)
    @Column(name = "email_estado", nullable = false, length = 20)
    private EstadoCorreoNotificacion emailEstado = EstadoCorreoNotificacion.PENDIENTE;

    @Column(name = "email_intentos", nullable = false)
    private int emailIntentos;

    @Column(name = "email_ultimo_error", columnDefinition = "TEXT")
    private String emailUltimoError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant ahora = Instant.now();
        if (createdAt == null) {
            createdAt = ahora;
        }
        updatedAt = ahora;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
