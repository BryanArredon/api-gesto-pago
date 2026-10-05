package com.onboarding.clientes.cliente.application.dto;

import com.onboarding.clientes.cliente.domain.valueobject.CodigoPostal;
import java.time.Instant;

/** Proyeccion de lectura del domicilio. */
public record DomicilioDto(
        String calle,
        String numeroExterior,
        String numeroInterior,
        String colonia,
        String municipio,
        String estado,
        CodigoPostal codigoPostal,
        String pais) {}
