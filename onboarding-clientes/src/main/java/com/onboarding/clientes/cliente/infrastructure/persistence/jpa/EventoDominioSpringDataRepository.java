package com.onboarding.clientes.cliente.infrastructure.persistence.jpa;

import com.onboarding.clientes.cliente.infrastructure.persistence.entity.EventoDominioEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventoDominioSpringDataRepository extends JpaRepository<EventoDominioEntity, UUID> {
}
