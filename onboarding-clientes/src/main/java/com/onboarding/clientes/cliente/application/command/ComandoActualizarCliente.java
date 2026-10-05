package com.onboarding.clientes.cliente.application.command;

import com.onboarding.clientes.cliente.domain.model.EstadoCivil;
import com.onboarding.clientes.cliente.domain.model.Sexo;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Comando de actualizacion de cliente.
 *
 * <p>La actualizacion es una operacion de reemplazo (PUT) de todos los datos modificables. Se pide el
 * documento completo en lugar de un parche parcial porque los datos forman un agregado coherente y
 * aceptarlos por partes abriria la puerta a estados intermedios invalidos (por ejemplo, un nuevo correo con
 * el telefono del anterior).
 *
 * <p>CURP y RFC llegan en {@code curpDeclarada} / {@code rfcDeclarado} solo para poder responder con un
 * error explicito si el cliente intenta cambiarlos: son inmutables.
 *
 * @param clienteId cliente a actualizar
 * @param persona datos personales
 * @param contacto datos de contacto
 * @param ubicacion domicilio
 * @param laboral datos laborales
 */
public record ComandoActualizarCliente(
        UUID clienteId,
        Persona persona,
        Contacto contacto,
        Ubicacion ubicacion,
        Laboral laboral) {

    /**
     * @param nombre
     * @param segundoNombre opcional
     * @param apellidoPaterno
     * @param apellidoMaterno
     * @param fechaNacimiento
     * @param curpDeclarada opcional, debe coincidir con la registrada
     * @param rfcDeclarado opcional, debe coincidir con el registrado
     * @param sexo
     * @param nacionalidad
     * @param estadoCivil
     */
    public record Persona(
            String nombre,
            String segundoNombre,
            String apellidoPaterno,
            String apellidoMaterno,
            LocalDate fechaNacimiento,
            String curpDeclarada,
            String rfcDeclarado,
            Sexo sexo,
            String nacionalidad,
            EstadoCivil estadoCivil) {}

    /**
     * @param correo
     * @param telefonoMovil
     * @param telefonoAlterno opcional
     */
    public record Contacto(String correo, String telefonoMovil, String telefonoAlterno) {}

    /**
     * @param calle
     * @param numeroExterior
     * @param numeroInterior opcional
     * @param colonia
     * @param municipio
     * @param estado
     * @param codigoPostal
     * @param pais opcional
     */
    public record Ubicacion(
            String calle,
            String numeroExterior,
            String numeroInterior,
            String colonia,
            String municipio,
            String estado,
            String codigoPostal,
            String pais) {}

    /**
     * @param ocupacion
     * @param empresa opcional
     * @param ingresoMensual
     * @param monedaIngreso
     */
    public record Laboral(String ocupacion, String empresa, java.math.BigDecimal ingresoMensual, String monedaIngreso) {}
}
