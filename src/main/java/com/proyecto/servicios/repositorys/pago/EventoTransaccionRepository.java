package com.proyecto.servicios.repositorys.pago;

import com.proyecto.servicios.entity.pago.EventoTransaccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventoTransaccionRepository extends JpaRepository<EventoTransaccion, Long> {
}