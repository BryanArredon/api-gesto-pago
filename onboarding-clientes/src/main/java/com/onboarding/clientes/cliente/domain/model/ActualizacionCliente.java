package com.onboarding.clientes.cliente.domain.model;

import com.onboarding.clientes.cliente.domain.valueobject.Curp;
import com.onboarding.clientes.cliente.domain.valueobject.Rfc;
import com.onboarding.clientes.shared.error.CodigoError;
import com.onboarding.clientes.shared.error.ErrorNegocio;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDate;

/**
 * Datos modificables de un cliente.
 *
 * <p>La CURP, el RFC y el numero de cuenta son inmutables por negocio. El campo {@code curpDeclarada}
 * existe para que la API pueda detectar el intento de modificar un dato protegido y responder con un error
 * explicito en lugar de ignorarlo en silencio.
 *
 * @param datosPersonales nombre, apellidos, sexo, nacionalidad, estado civil y fecha de nacimiento
 * @param datosContacto correo y telefonos
 * @param datosLaborales ocupacion, empresa e ingreso
 * @param domicilio domicilio vigente
 * @param curpDeclarada valores de CURP/RFC presentes en la peticion, pueden ser {@code null}
 * @param momento instante de la operacion
 * @param hoy fecha de referencia para las reglas por edad
 */
public record ActualizacionCliente(
        DatosPersonales datosPersonales,
        DatosContacto datosContacto,
        DatosLaborales datosLaborales,
        Domicilio domicilio,
        Curp curpDeclarada,
        Rfc rfcDeclarado,
        Instant momento,
        LocalDate hoy) {}
