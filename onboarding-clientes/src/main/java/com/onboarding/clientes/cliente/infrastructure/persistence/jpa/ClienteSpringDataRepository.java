package com.onboarding.clientes.cliente.infrastructure.persistence.jpa;

import com.onboarding.clientes.cliente.infrastructure.persistence.entity.ClienteEntity;
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
public interface ClienteSpringDataRepository extends JpaRepository<ClienteEntity, UUID> {

    Optional<ClienteEntity> findByCurp(String curp);

    Optional<ClienteEntity> findByRfc(String rfc);

    @Query("SELECT c FROM ClienteEntity c WHERE LOWER(c.correoElectronico) = LOWER(:correo)")
    Optional<ClienteEntity> findByCorreoElectronicoIgnoreCase(@Param("correo") String correo);

    boolean existsByCurp(String curp);

    boolean existsByRfc(String rfc);

    @Query("SELECT COUNT(c) > 0 FROM ClienteEntity c WHERE LOWER(c.correoElectronico) = LOWER(:correo)")
    boolean existsByCorreoElectronicoIgnoreCase(@Param("correo") String correo);

    long countByActivoTrue();

    @Query("SELECT c FROM ClienteEntity c WHERE (:soloActivos = false OR c.activo = true) " +
           "AND (:cursorTime IS NULL OR c.createdAt < :cursorTime OR (c.createdAt = :cursorTime AND c.id < :cursorId)) " +
           "ORDER BY c.createdAt DESC, c.id DESC")
    List<ClienteEntity> listarConCursor(
            @Param("cursorTime") Instant cursorTime,
            @Param("cursorId") UUID cursorId,
            @Param("soloActivos") boolean soloActivos,
            Pageable pageable);

    @Query("SELECT c FROM ClienteEntity c WHERE c.createdAt >= :desde AND c.createdAt <= :hasta " +
           "AND (:cursorTime IS NULL OR c.createdAt < :cursorTime OR (c.createdAt = :cursorTime AND c.id < :cursorId)) " +
           "ORDER BY c.createdAt DESC, c.id DESC")
    List<ClienteEntity> listarEntreFechasConCursor(
            @Param("desde") Instant desde,
            @Param("hasta") Instant hasta,
            @Param("cursorTime") Instant cursorTime,
            @Param("cursorId") UUID cursorId,
            Pageable pageable);
}
