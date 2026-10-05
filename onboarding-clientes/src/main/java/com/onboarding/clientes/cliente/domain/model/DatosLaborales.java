package com.onboarding.clientes.cliente.domain.model;

import com.onboarding.clientes.shared.dinero.Dinero;

/**
 * Informacion laboral del cliente.
 *
 * @param ocupacion profesion u ocupacion
 * @param empresa empresa donde labora, opcional para maestros independientes
 * @param ingresoMensual ingreso mensual neto, debe ser mayor que cero
 */
public record DatosLaborales(String ocupacion, String empresa, Dinero ingresoMensual) {}
