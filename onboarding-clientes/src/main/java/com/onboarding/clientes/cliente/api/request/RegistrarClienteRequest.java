package com.onboarding.clientes.cliente.api.request;

import com.onboarding.clientes.cliente.domain.model.EstadoCivil;
import com.onboarding.clientes.cliente.domain.model.Sexo;
import com.onboarding.clientes.cliente.domain.model.TipoCuenta;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Solicitud para registrar un nuevo cliente persona física con apertura de cuenta y usuario de acceso")
public record RegistrarClienteRequest(
        @Valid @NotNull @Schema(description = "Datos personales del cliente") PersonaRequest persona,
        @Valid @NotNull @Schema(description = "Datos de contacto del cliente") ContactoRequest contacto,
        @Valid @NotNull @Schema(description = "Domicilio del cliente") DomicilioRequest ubicacion,
        @Valid @NotNull @Schema(description = "Información laboral y económica") LaboralRequest laboral,
        @Valid @NotNull @Schema(description = "Credenciales iniciales de acceso") CredencialesRequest credenciales,
        @Valid @Schema(description = "Configuración opcional de apertura de cuenta") AperturaCuentaRequest cuenta) {

    public record PersonaRequest(
            @NotBlank @Size(min = 2, max = 50) @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ\\s]+$", message = "El nombre solo puede contener letras y espacios")
            @Schema(example = "Juan") String nombre,

            @Size(min = 2, max = 50) @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ\\s]+$", message = "El segundo nombre solo puede contener letras y espacios")
            @Schema(example = "Francisco") String segundoNombre,

            @NotBlank @Size(min = 2, max = 50) @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ\\s]+$", message = "El apellido paterno solo puede contener letras y espacios")
            @Schema(example = "Manzano") String apellidoPaterno,

            @NotBlank @Size(min = 2, max = 50) @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ\\s]+$", message = "El apellido materno solo puede contener letras y espacios")
            @Schema(example = "Garcia") String apellidoMaterno,

            @NotNull @Past(message = "La fecha de nacimiento debe ser una fecha pasada")
            @Schema(example = "1995-05-15") LocalDate fechaNacimiento,

            @NotBlank @Size(min = 18, max = 18, message = "La CURP debe tener exactamente 18 caracteres")
            @Pattern(regexp = "^[A-Z]{4}[0-9]{6}[A-Z]{6}[0-9A-Z]{2}$", message = "Formato de CURP inválido")
            @Schema(example = "MAGJ950515HDFRRN01") String curp,

            @NotBlank @Pattern(regexp = "^[A-ZÑ&]{4}[0-9]{6}[0-9A-Z]{3}$", message = "Formato de RFC de persona física inválido (13 caracteres)")
            @Schema(example = "MAGJ950515ABC") String rfc,

            @NotNull @Schema(example = "HOMBRE") Sexo sexo,

            @NotBlank @Size(min = 2, max = 50) @Schema(example = "Mexicana") String nacionalidad,

            @NotNull @Schema(example = "SOLTERO") EstadoCivil estadoCivil) {}

    public record ContactoRequest(
            @NotBlank @Email(message = "El correo electrónico debe tener un formato válido") @Size(max = 100)
            @Schema(example = "juan.manzano@ejemplo.com") String correo,

            @NotBlank @Pattern(regexp = "^[0-9]{10}$", message = "El teléfono móvil debe tener exactamente 10 dígitos")
            @Schema(example = "4181234567") String telefonoMovil,

            @Pattern(regexp = "^[0-9]{10}$", message = "El teléfono alternativo debe tener exactamente 10 dígitos")
            @Schema(example = "4189876543") String telefonoAlterno) {}

    public record DomicilioRequest(
            @NotBlank @Size(min = 3, max = 100) @Schema(example = "Av. Universidad") String calle,
            @NotBlank @Size(min = 1, max = 10) @Schema(example = "123") String numeroExterior,
            @Size(max = 10) @Schema(example = "B") String numeroInterior,
            @NotBlank @Size(min = 2, max = 100) @Schema(example = "Centro") String colonia,
            @NotBlank @Size(min = 2, max = 100) @Schema(example = "Dolores Hidalgo") String municipio,
            @NotBlank @Size(min = 2, max = 50) @Schema(example = "Guanajuato") String estado,
            @NotBlank @Pattern(regexp = "^[0-9]{5}$", message = "El código postal debe contener exactamente 5 dígitos")
            @Schema(example = "37800") String codigoPostal,
            @NotBlank @Size(min = 2, max = 50) @Schema(example = "México") String pais) {}

    public record LaboralRequest(
            @NotBlank @Size(min = 2, max = 100) @Schema(example = "Desarrollador de Software") String ocupacion,
            @Size(max = 100) @Schema(example = "Tech Solutions") String empresa,
            @NotNull @DecimalMin(value = "0.01", message = "El ingreso mensual debe ser mayor a cero")
            @Schema(example = "35000.00") BigDecimal ingresoMensual,
            @Size(min = 3, max = 3) @Schema(example = "MXN") String monedaIngreso) {}

    public record CredencialesRequest(
            @NotBlank @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
            @Schema(example = "Segura123!") String contrasenia,
            @Pattern(regexp = "^[0-9]{4,6}$", message = "El PIN debe tener entre 4 y 6 dígitos numéricos")
            @Schema(example = "1234") String pin) {}

    public record AperturaCuentaRequest(
            @DecimalMin(value = "0.00", message = "El saldo inicial no puede ser negativo")
            @Schema(example = "1000.00") BigDecimal saldoInicial,
            @Schema(example = "AHORRO") TipoCuenta tipoCuenta,
            @Schema(example = "MXN") String monedaCuenta) {}
}
