package com.onboarding.clientes.cliente.infrastructure.persistence.jpa;

import com.onboarding.clientes.cliente.infrastructure.persistence.entity.DomicilioEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DomicilioSpringDataRepository extends JpaRepository<DomicilioEntity, UUID> {
    Optional<DomicilioEntity> findByClienteId(UUID clienteId);
}
