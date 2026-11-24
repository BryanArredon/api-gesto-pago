package com.proyecto.servicios.service.notificacion;

import com.proyecto.servicios.entity.pago.EstadoTransaccion;
import com.proyecto.servicios.evento.MovimientoRegistradoEvent;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ContenidoNotificacion {

    private ContenidoNotificacion() {
    }

    public record Contenido(String titulo, String cuerpo, Map<String, Object> datos) {
    }

    public static Contenido crear(MovimientoRegistradoEvent evento) {
        String descripcion = texto(evento.servicio()) + " - " + texto(evento.producto());
        String monto = dinero(evento.monto());
        String comision = dinero(evento.comision());

        String motivo = primeroNoVacio(
                evento.motivoRechazo(),
                evento.detalleError(),
                evento.detalle(),
                "Operacion rechazada por el proveedor");
        String codError = evento.codigoErrorGestoPago();
        String codigoTexto = (codError != null && !codError.isBlank()) ? " (Código GestoPago: " + codError.trim() + ")" : "";

        String titulo;
        String cuerpo;
        switch (evento.estado()) {
            case PENDIENTE -> {
                titulo = "Pago registrado";
                cuerpo = "Registramos tu intencion de pago de " + descripcion
                        + ". Estamos validando el saldo y la referencia.";
            }
            case RETENIDO -> {
                titulo = "Saldo retenido";
                cuerpo = "Retuvimos temporalmente " + monto + " de tu cuenta para procesar el pago de "
                        + descripcion + ".";
            }
            case EN_PROCESO -> {
                titulo = "Pago en proceso";
                cuerpo = "Tu pago de " + descripcion
                        + " esta en proceso. Te confirmaremos en cuanto el proveedor responda.";
            }
            case LIQUIDADO -> {
                titulo = "Pago exitoso";
                cuerpo = "Tu pago de " + descripcion + " por " + monto
                        + " fue aplicado correctamente.";
            }
            case REVERSANDO -> {
                titulo = "Pago rechazado - Reverso en proceso";
                cuerpo = "Tu pago de " + descripcion + " no pudo completarse" + codigoTexto
                        + ". Por que se rechazo: " + motivo
                        + ". Tu saldo de " + monto + " MXN esta en proceso de liberacion en tu cuenta.";
            }
            case REVERSADO -> {
                titulo = "Pago rechazado";
                cuerpo = "Tu pago de " + descripcion + " fue rechazado" + codigoTexto
                        + ". Por que se rechazo: " + motivo
                        + ". Tu saldo de " + monto + " MXN fue liberado y permanece disponible en tu cuenta.";
            }
            case REQUIERE_REVISION -> {
                titulo = "Pago en revision";
                cuerpo = "Tu pago de " + descripcion + " no pudo confirmarse con el proveedor" + codigoTexto
                        + ". Motivo: " + motivo
                        + ". Tu saldo de " + monto + " MXN permanece protegido mientras nuestro equipo atiende la operacion.";
            }
            default -> {
                titulo = "Movimiento de pago";
                cuerpo = "Se actualizo tu pago de " + descripcion + " por " + monto + ".";
            }
        }

        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("estado", evento.estado().name());
        datos.put("transaccionId", evento.transaccionId());
        datos.put("monto", evento.monto());
        datos.put("comision", evento.comision());
        datos.put("servicio", evento.servicio());
        datos.put("producto", evento.producto());
        datos.put("referencia", evento.referencia());
        datos.put("origen", evento.origen());
        if (evento.codigoErrorGestoPago() != null) {
            datos.put("codigoErrorGestoPago", evento.codigoErrorGestoPago());
        }
        if (evento.motivoRechazo() != null) {
            datos.put("motivoRechazo", evento.motivoRechazo());
        }
        if (evento.detalleError() != null) {
            datos.put("detalleError", evento.detalleError());
        } else if (evento.detalle() != null) {
            datos.put("detalleError", evento.detalle());
        }

        return new Contenido(titulo, cuerpo + " Monto: " + monto + " MXN. Comision: " + comision + " MXN.", datos);
    }

    private static String dinero(BigDecimal valor) {
        BigDecimal seguro = valor == null ? BigDecimal.ZERO : valor;
        return seguro.setScale(2, RoundingMode.HALF_EVEN).toPlainString();
    }

    private static String texto(String valor) {
        return valor == null || valor.isBlank() ? "tu servicio" : valor;
    }

    private static String primeroNoVacio(String... valores) {
        for (String v : valores) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }
}
