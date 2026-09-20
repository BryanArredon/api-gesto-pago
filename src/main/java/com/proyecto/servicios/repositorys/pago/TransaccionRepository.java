package com.proyecto.servicios.repositorys.pago;

import com.proyecto.servicios.entity.pago.EstadoTransaccion;
import com.proyecto.servicios.entity.pago.Transaccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransaccionRepository extends JpaRepository<Transaccion, Long> {

    Optional<Transaccion> findByIdAndUsuarioId(Long id, Long usuarioId);

    Optional<Transaccion> findByUsuarioIdAndIdempotencyKey(Long usuarioId, String idempotencyKey);

    List<Transaccion> findByUsuarioIdOrderByCreatedAtDesc(Long usuarioId);

    List<Transaccion> findByEstado(EstadoTransaccion estado);
}