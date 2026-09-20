package com.proyecto.servicios.model.pago;

import com.proyecto.servicios.entity.pago.Transaccion;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Vista de una transaccion para el cliente. Los montos se exponen como texto
 * para no degradar la precision decimal (el servidor es la fuente de verdad).
 */
@Getter
@Setter
@NoArgsConstructor
public class TransaccionDto {

    private static final DateTimeFormatter ISO_UTC =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    private Long id;
    private String upc;
    private Integer idServicio;
    private Integer idProducto;
    private String servicio;
    private String producto;
    private String referencia;
    private String monto;
    private String comision;
    private String estado;
    private String numeroAutorizacion;
    private String idTx;
    private String pin;
    private String legend;
    private String errorMensaje;
    private String fecha;

    public static TransaccionDto from(Transaccion tx) {
        TransaccionDto dto = new TransaccionDto();
        dto.setId(tx.getId());
        dto.setUpc(tx.getUpc());
        dto.setIdServicio(tx.getIdServicio());
        dto.setIdProducto(tx.getIdProducto());
        dto.setServicio(tx.getServicio());
        dto.setProducto(tx.getProducto());
        dto.setReferencia(tx.getReferencia());
        dto.setMonto(texto(tx.getMonto()));
        dto.setComision(texto(tx.getComision()));
        dto.setEstado(tx.getEstado().name());
        dto.setNumeroAutorizacion(tx.getNumeroAutorizacion());
        dto.setIdTx(tx.getIdTx());
        dto.setPin(tx.getPin());
        dto.setLegend(tx.getLegend());
        dto.setErrorMensaje(tx.getErrorMensaje());
        dto.setFecha(fecha(tx.getFecha()));
        return dto;
    }

    private static String texto(java.math.BigDecimal valor) {
        return valor == null ? null : valor.toPlainString();
    }

    private static String fecha(Instant instante) {
        return instante == null ? null : ISO_UTC.format(instante);
    }
}