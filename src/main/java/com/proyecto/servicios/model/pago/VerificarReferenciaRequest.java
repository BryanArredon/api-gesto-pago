package com.proyecto.servicios.model.pago;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class VerificarReferenciaRequest {

    @NotNull(message = "idServicio es obligatorio")
    private Integer idServicio;

    @NotNull(message = "idProducto es obligatorio")
    private Integer idProducto;

    @NotBlank(message = "La referencia es obligatoria")
    @Size(max = 100, message = "La referencia no puede exceder 100 caracteres")
    private String referencia;
}