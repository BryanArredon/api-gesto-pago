package com.onboarding.clientes.cliente.infrastructure.persistence.jpa;

import com.onboarding.clientes.cliente.infrastructure.persistence.entity.CuentaEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CuentaSpringDataRepository extends JpaRepository<CuentaEntity, UUID> {

    Optional<CuentaEntity> findByNumeroCuenta(String numeroCuenta);

    List<CuentaEntity> findByClienteId(UUID clienteId);

    long countByEstatus(String estatus);

    @Query(value = "SELECT cuenta_generar_numero()", nativeQuery = true)
    String siguienteNumeroCuenta();

    @Query("SELECT c FROM CuentaEntity c WHERE c.estatus = 'ACTIVA' " +
           "AND (:cursorTime IS NULL OR c.createdAt < :cursorTime OR (c.createdAt = :cursorTime AND c.id < :cursorId)) " +
           "ORDER BY c.createdAt DESC, c.id DESC")
    List<CuentaEntity> listarActivasConCursor(
            @Param("cursorTime") Instant cursorTime,
            @Param("cursorId") UUID cursorId,
            Pageable pageable);
}
