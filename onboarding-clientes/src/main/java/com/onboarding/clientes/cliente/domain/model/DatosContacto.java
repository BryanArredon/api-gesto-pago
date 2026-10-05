package com.onboarding.clientes.cliente.domain.model;

import com.onboarding.clientes.cliente.domain.valueobject.Correo;
import com.onboarding.clientes.cliente.domain.valueobject.Telefono;

/**
 * Datos de contacto del cliente.
 *
 * @param correo correo electronico, tambien es su nombre de usuario de acceso
 * @param telefonoMovil movil principal, 10 digitos
 * @param telefonoAlterno movil alterno, opcional
 */
public record DatosContacto(Correo correo, Telefono telefonoMovil, Telefono telefonoAlterno) {}
