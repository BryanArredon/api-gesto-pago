package com.onboarding.clientes.cliente.application.command;

import com.onboarding.clientes.cliente.domain.model.EstadoCivil;
import com.onboarding.clientes.cliente.domain.model.Sexo;
import com.onboarding.clientes.cliente.domain.model.TipoCuenta;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Comando de alta de cliente persona fisica, agrupado igual que el cuerpo de la peticion HTTP.
 *
 * <p>Los valores llegan en texto plano: es la frontera de la capa de aplicacion, y el servicio es quien los
 * convierte a value objects, que son los que validan e invariantes. El formato de cada campo tambien se
 * valida antes (Bean Validation en el DTO de entrada) para que el error se reporte sin tocar el dominio.
 *
 * @param persona identidad y datos personales
 * @param contacto correo y telefonos
 * @param ubicacion domicilio
 * @param laboral ocupacion, empresa e ingreso
 * @param credenciales contrasena y PIN de acceso
 * @param cuenta saldo inicial y producto
 */
public record ComandoRegistrarCliente(
        Persona persona, Contacto contacto, Ubicacion ubicacion, Laboral laboral, Credenciales credenciales, AperturaCuenta cuenta) {

    /**
     * @param nombre nombre de pila
     * @param segundoNombre opcional
     * @param apellidoPaterno
     * @param apellidoMaterno
     * @param fechaNacimiento
     * @param curp
     * @param rfc
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
            String curp,
            String rfc,
            Sexo sexo,
            String nacionalidad,
            EstadoCivil estadoCivil) {}

    /**
     * @param correo correo electronico y nombre de usuario de acceso
     * @param telefonoMovil 10 digitos
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
     * @param codigoPostal 5 digitos
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
     * @param ingresoMensual mayor que cero
     * @param monedaIngreso
     */
    public record Laboral(String ocupacion, String empresa, BigDecimal ingresoMensual, String monedaIngreso) {}

    /**
     * @param contrasenia se cifra en el servicio con BCrypt; nunca se registra ni se persiste en claro
     * @param pin opcional, 4 a 6 digitos
     */
    public record Credenciales(String contrasenia, String pin) {}

    /**
     * @param saldoInicial no puede ser negativo
     * @param tipoCuenta
     * @param monedaCuenta
     */
    public record AperturaCuenta(BigDecimal saldoInicial, TipoCuenta tipoCuenta, String monedaCuenta) {}
}
