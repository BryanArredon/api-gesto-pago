package com.proyecto.servicios.model.pago;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.proyecto.servicios.util.ValidadorLuhn;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Intencion de pago creada por el cliente (Pizarra / Etapa 1 de la Saga).
 *
 * <p>Incluye el numero de cuenta bancaria de 10 digitos (con digito verificador Luhn) y el PIN de
 * autorizacion de 4 digitos. El monto y la comision son verificados nuevamente por el servidor contra el
 * catalogo autoritativo.
 */
@Getter
@Setter
@NoArgsConstructor
public class PagoRequestDto {

    @NotBlank(message = "numeroCuenta es obligatorio")
    @Pattern(regexp = "^[0-9]{10}$", message = "numeroCuenta debe tener exactamente 10 digitos")
    private String numeroCuenta;

    @NotBlank(message = "pinAutorizacion es obligatorio")
    @Pattern(regexp = "^[0-9]{4}$", message = "pinAutorizacion debe tener exactamente 4 digitos")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String pinAutorizacion;

    @NotNull(message = "idServicio es obligatorio")
    private Integer idServicio;

    @NotNull(message = "idProducto es obligatorio")
    private Integer idProducto;

    @NotBlank(message = "La referencia es obligatoria")
    @Size(max = 100, message = "La referencia no puede exceder 100 caracteres")
    private String referencia;

    private BigDecimal monto;

    @NotBlank(message = "idempotencyKey es obligatoria")
    @Size(min = 3, max = 36, message = "idempotencyKey invalida")
    private String idempotencyKey;

    @Override
    public String toString() {
        return "PagoRequestDto[numeroCuenta=" + ValidadorLuhn.enmascararCuenta(numeroCuenta)
                + ", pinAutorizacion=****"
                + ", idServicio=" + idServicio
                + ", idProducto=" + idProducto
                + ", referencia=" + referencia
                + ", monto=" + monto
                + ", idempotencyKey=" + idempotencyKey + "]";
    }
}
