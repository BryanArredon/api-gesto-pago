package com.onboarding.clientes.cliente.api.request;

import com.onboarding.clientes.cliente.domain.model.EstadoCivil;
import com.onboarding.clientes.cliente.domain.model.Sexo;
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

@Schema(description = "Solicitud para actualizar información de un cliente existente")
public record ActualizarClienteRequest(
        @Valid @NotNull PersonaUpdate persona,
        @Valid @NotNull ContactoUpdate contacto,
        @Valid @NotNull DomicilioUpdate ubicacion,
        @Valid @NotNull LaboralUpdate laboral) {

    public record PersonaUpdate(
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

            @Schema(description = "CURP declarada para verificación de inmutabilidad", example = "MAGJ950515HDFRRN01")
            String curpDeclarada,

            @Schema(description = "RFC declarado para verificación de inmutabilidad", example = "MAGJ950515ABC")
            String rfcDeclarado,

            @NotNull @Schema(example = "HOMBRE") Sexo sexo,
            @NotBlank @Size(min = 2, max = 50) @Schema(example = "Mexicana") String nacionalidad,
            @NotNull @Schema(example = "CASADO") EstadoCivil estadoCivil) {}

    public record ContactoUpdate(
            @NotBlank @Email(message = "El correo electrónico debe tener un formato válido") @Size(max = 100)
            @Schema(example = "nuevo.correo@ejemplo.com") String correo,

            @NotBlank @Pattern(regexp = "^[0-9]{10}$", message = "El teléfono móvil debe tener exactamente 10 dígitos")
            @Schema(example = "4181234567") String telefonoMovil,

            @Pattern(regexp = "^[0-9]{10}$", message = "El teléfono alternativo debe tener exactamente 10 dígitos")
            @Schema(example = "4189876543") String telefonoAlterno) {}

    public record DomicilioUpdate(
            @NotBlank @Size(min = 3, max = 100) @Schema(example = "Calle Nueva") String calle,
            @NotBlank @Size(min = 1, max = 10) @Schema(example = "456") String numeroExterior,
            @Size(max = 10) @Schema(example = "A") String numeroInterior,
            @NotBlank @Size(min = 2, max = 100) @Schema(example = "Colonia Moderna") String colonia,
            @NotBlank @Size(min = 2, max = 100) @Schema(example = "Dolores Hidalgo") String municipio,
            @NotBlank @Size(min = 2, max = 50) @Schema(example = "Guanajuato") String estado,
            @NotBlank @Pattern(regexp = "^[0-9]{5}$", message = "El código postal debe contener exactamente 5 dígitos")
            @Schema(example = "37800") String codigoPostal,
            @NotBlank @Size(min = 2, max = 50) @Schema(example = "México") String pais) {}

    public record LaboralUpdate(
            @NotBlank @Size(min = 2, max = 100) @Schema(example = "Arquitecto de Software") String ocupacion,
            @Size(max = 100) @Schema(example = "Global Enterprise") String empresa,
            @NotNull @DecimalMin(value = "0.01", message = "El ingreso mensual debe ser mayor a cero")
            @Schema(example = "45000.00") BigDecimal ingresoMensual,
            @Size(min = 3, max = 3) @Schema(example = "MXN") String monedaIngreso) {}
}
