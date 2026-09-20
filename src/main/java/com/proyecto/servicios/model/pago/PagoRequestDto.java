package com.proyecto.servicios.model.pago;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Intencion de pago creada por el cliente. El monto es verificado nuevamente
 * por el servidor contra el catalogo autoritativo; para productos de precio
 * fijo el monto enviado se ignora.
 */
@Getter
@Setter
@NoArgsConstructor
public class PagoRequestDto {

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
}