package com.onboarding.clientes.cliente.domain.model;

import com.onboarding.clientes.cliente.domain.valueobject.Curp;
import com.onboarding.clientes.shared.dinero.Dinero;
import com.onboarding.clientes.shared.error.CodigoError;
import com.onboarding.clientes.shared.error.ErrorNegocio;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Datos del proceso de alta: todo lo necesario para crear al cliente, su domicilio, su usuario de acceso
 * y su cuenta bancaria en una sola transaccion.
 *
 * @param datosPersonales identidad del cliente
 * @param datosContacto correo (nombre de usuario) y telefonos
 * @param datosLaborales ocupacion, empresa e ingreso
 * @param domicilio domicilio vigente
 * @param usuario usuario de acceso con su contrasena ya cifrada
 * @param saldoInicial saldo con el que se abre la cuenta, no puede ser negativo
 * @param tipoCuenta producto de la cuenta
 * @param momento instante de la operacion (inyectado para poder congelar el tiempo en pruebas)
 * @param hoy fecha de referencia para las reglas por edad
 */
public record AltaCliente(
        DatosPersonales datosPersonales,
        DatosContacto datosContacto,
        DatosLaborales datosLaborales,
        Domicilio domicilio,
        Usuario usuario,
        Dinero saldoInicial,
        TipoCuenta tipoCuenta,
        Instant momento,
        LocalDate hoy) {}
