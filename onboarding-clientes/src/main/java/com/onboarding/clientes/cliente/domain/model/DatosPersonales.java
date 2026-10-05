package com.onboarding.clientes.cliente.domain.model;

import com.onboarding.clientes.cliente.domain.valueobject.Curp;
import com.onboarding.clientes.cliente.domain.valueobject.Rfc;
import com.onboarding.clientes.cliente.domain.valueobject.Telefono;
import java.time.LocalDate;

/**
 * Datos personales declarados por el cliente en el alta.
 *
 * @param nombre nombre(s) de pila
 * @param segundoNombre segundo nombre, opcional
 * @param apellidoPaterno
 * @param apellidoMaterno
 * @param fechaNacimiento
 * @param curp identificacion oficial
 * @param rfc identificacion fiscal
 * @param sexo
 * @param nacionalidad
 * @param estadoCivil
 */
public record DatosPersonales(
        String nombre,
        String segundoNombre,
        String apellidoPaterno,
        String apellidoMaterno,
        LocalDate fechaNacimiento,
        Curp curp,
        Rfc rfc,
        Sexo sexo,
        String nacionalidad,
        EstadoCivil estadoCivil) {}
