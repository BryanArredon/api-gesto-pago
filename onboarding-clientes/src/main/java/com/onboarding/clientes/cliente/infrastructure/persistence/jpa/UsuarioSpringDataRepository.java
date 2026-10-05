package com.onboarding.clientes.cliente.infrastructure.persistence.jpa;

import com.onboarding.clientes.cliente.infrastructure.persistence.entity.UsuarioEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioSpringDataRepository extends JpaRepository<UsuarioEntity, UUID> {

    Optional<UsuarioEntity> findByClienteId(UUID clienteId);

    @Query("SELECT u FROM UsuarioEntity u WHERE LOWER(u.correoElectronico) = LOWER(:correo)")
    Optional<UsuarioEntity> findByCorreoElectronicoIgnoreCase(@Param("correo") String correo);

    @Query("SELECT COUNT(u) > 0 FROM UsuarioEntity u WHERE LOWER(u.correoElectronico) = LOWER(:correo)")
    boolean existsByCorreoElectronicoIgnoreCase(@Param("correo") String correo);

    boolean existsByClienteId(UUID clienteId);
}
