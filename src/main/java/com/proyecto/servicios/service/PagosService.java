package com.proyecto.servicios.service;

import com.proyecto.servicios.model.pago.PagoRequestDto;
import com.proyecto.servicios.model.pago.TransaccionDto;
import com.proyecto.servicios.model.pago.VerificarReferenciaRequest;
import com.proyecto.servicios.model.pago.VerificarReferenciaResponse;

import java.util.List;

public interface PagosService {

    VerificarReferenciaResponse verificarReferencia(VerificarReferenciaRequest request);

    TransaccionDto crearTransaccion(Long usuarioId, PagoRequestDto request);

    TransaccionDto confirmarTransaccion(Long usuarioId, Long id);

    List<TransaccionDto> historial(Long usuarioId);
}