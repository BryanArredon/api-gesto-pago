package com.proyecto.servicios.repositorys.pago;

import com.proyecto.servicios.entity.pago.EstadoTransaccion;
import com.proyecto.servicios.entity.pago.Transaccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransaccionRepository extends JpaRepository<Transaccion, Long> {

    Optional<Transaccion> findByIdAndUsuarioId(Long id, Long usuarioId);

    Optional<Transaccion> findByUsuarioIdAndIdempotencyKey(Long usuarioId, String idempotencyKey);

    List<Transaccion> findByUsuarioIdOrderByCreatedAtDesc(Long usuarioId);

    List<Transaccion> findByEstado(EstadoTransaccion estado);

    List<Transaccion> findByEstadoInAndProximaConfirmacionEnLessThanEqualOrderByProximaConfirmacionEnAsc(
            Collection<EstadoTransaccion> estados, Instant corte);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Transaccion t SET t.estado = :nuevoEstado, t.updatedAt = :ahora " +
           "WHERE t.id = :id AND t.estado = :estadoEsperado")
    int actualizarEstadoAtomico(
            @Param("id") Long id,
            @Param("estadoEsperado") EstadoTransaccion estadoEsperado,
            @Param("nuevoEstado") EstadoTransaccion nuevoEstado,
            @Param("ahora") Instant ahora);
}
