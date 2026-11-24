package com.proyecto.servicios.util;

import java.util.Locale;

/**
 * Mapeador de codigos y respuestas de error del switch transaccional GestoPago
 * hacia descripciones comprensibles y amigables para el usuario.
 */
public final class GestoPagoErrorMapper {

    private GestoPagoErrorMapper() {
    }

    public record ErrorGestoPago(
            String codigo,
            String textoOriginal,
            String explicacionAmigable) {
    }

    /**
     * Mapea un codigo de error y/o texto original de GestoPago a una estructura amigable.
     */
    public static ErrorGestoPago mapear(String codigo, String textoOriginal) {
        String cod = (codigo != null && !codigo.isBlank()) ? codigo.trim() : null;
        String raw = (textoOriginal != null && !textoOriginal.isBlank()) ? textoOriginal.trim() : null;
        String amigable = resolverExplicacion(cod, raw);
        String codFinal = cod != null ? cod : inferirCodigoDesdeTexto(raw);
        return new ErrorGestoPago(codFinal, raw != null ? raw : amigable, amigable);
    }

    public static String resolverExplicacion(String codigo, String textoOriginal) {
        if (codigo != null && !codigo.isBlank()) {
            String c = codigo.trim();
            switch (c) {
                case "02" -> {
                    return "La referencia o número ingresado es inválido o no existe para este servicio.";
                }
                case "03" -> {
                    return "El carrier de telecomunicaciones o servicio no responde. Intenta más tarde.";
                }
                case "04" -> {
                    return "El proveedor no cuenta con saldo suficiente en su red de distribución para procesar la transacción.";
                }
                case "05" -> {
                    return "El monto solicitado no es válido para este producto o servicio.";
                }
                case "06" -> {
                    return "Esta operación ya fue procesada previamente (transacción duplicada).";
                }
                case "07" -> {
                    return "El número o referencia no corresponde a la compañía seleccionada.";
                }
                case "08" -> {
                    return "La transacción fue declinada por la empresa proveedora del servicio.";
                }
                case "09" -> {
                    return "El servicio se encuentra temporalmente fuera de servicio por parte del proveedor.";
                }
                case "10" -> {
                    return "El sistema del proveedor se encuentra en mantenimiento temporal. Intenta más tarde.";
                }
                case "11" -> {
                    return "Se ha superado el límite de intentos permitidos para esta referencia.";
                }
                case "12" -> {
                    return "Los datos de la operación no cumplen con los requisitos del servicio.";
                }
                case "50" -> {
                    return "Error de autenticación con el proveedor de pagos.";
                }
                case "51" -> {
                    return "Dispositivo o terminal no autorizado o no registrado ante el proveedor de pagos.";
                }
                case "70" -> {
                    return "La transacción no fue aplicada por el proveedor del servicio.";
                }
                case "71" -> {
                    return "La transacción se encuentra en proceso de validación ante el carrier.";
                }
                case "80" -> {
                    return "Fallo en la comunicación con la compañía del servicio.";
                }
                case "82" -> {
                    return "Tiempo de espera agotado al conectar con el proveedor (Timeout).";
                }
                case "99" -> {
                    return "Error general del sistema en el proveedor de pagos.";
                }
                default -> {
                    // Si trae texto descriptivo util
                    if (textoOriginal != null && !textoOriginal.isBlank()) {
                        String explicacionTexto = inferirPorTexto(textoOriginal);
                        if (explicacionTexto != null) {
                            return explicacionTexto;
                        }
                        return "Operación rechazada por el proveedor (código " + c + "): " + textoOriginal;
                    }
                    return "Operación rechazada por el proveedor de servicios (código " + c + ").";
                }
            }
        }

        if (textoOriginal != null && !textoOriginal.isBlank()) {
            String explicacion = inferirPorTexto(textoOriginal);
            if (explicacion != null) {
                return explicacion;
            }
            return textoOriginal;
        }

        return "La operación fue rechazada por el proveedor del servicio.";
    }

    private static String inferirPorTexto(String texto) {
        String lower = texto.toLowerCase(Locale.ROOT);
        if (lower.contains("carrier no responde") || lower.contains("no responde")) {
            return "El carrier o compañía no responde en este momento. Por favor intenta más tarde.";
        }
        if (lower.contains("dispositivo no registrado")) {
            return "Dispositivo o terminal no registrado ante el proveedor de pagos.";
        }
        if (lower.contains("saldo insuficiente") || lower.contains("sin saldo") || lower.contains("saldo agotado")) {
            return "Saldo insuficiente en la red del proveedor para completar la transacción.";
        }
        if (lower.contains("referencia") || lower.contains("invalida") || lower.contains("inválida") || lower.contains("invalido") || lower.contains("inválido")) {
            return "La referencia ingresada es inválida o inexistente para este servicio.";
        }
        if (lower.contains("timeout") || lower.contains("tiempo de espera")) {
            return "Tiempo de espera agotado al conectar con el proveedor (Timeout).";
        }
        if (lower.contains("no fue aplicada") || lower.contains("no aplicada")) {
            return "La transacción no fue aplicada por el proveedor del servicio.";
        }
        if (lower.contains("monto") || lower.contains("importe")) {
            return "El monto solicitado no es aceptado por el servicio.";
        }
        if (lower.contains("mantenimiento") || lower.contains("fuera de servicio") || lower.contains("fuera de linea")) {
            return "El servicio no está disponible temporalmente por mantenimiento del proveedor.";
        }
        return null;
    }

    private static String inferirCodigoDesdeTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return "ERR-GESTOPAGO";
        }
        String lower = texto.toLowerCase(Locale.ROOT);
        if (lower.contains("carrier no responde") || lower.contains("no responde")) {
            return "03";
        }
        if (lower.contains("dispositivo no registrado")) {
            return "51";
        }
        if (lower.contains("saldo insuficiente") || lower.contains("sin saldo")) {
            return "04";
        }
        if (lower.contains("referencia") || lower.contains("invalida") || lower.contains("inválida")) {
            return "02";
        }
        if (lower.contains("timeout") || lower.contains("tiempo de espera")) {
            return "82";
        }
        if (lower.contains("no fue aplicada") || lower.contains("no aplicada")) {
            return "70";
        }
        return "ERR-GESTOPAGO";
    }
}

