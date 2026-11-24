package com.proyecto.servicios.service.notificacion;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.entity.notificacion.EstadoCorreoNotificacion;
import com.proyecto.servicios.entity.notificacion.Notificacion;
import com.proyecto.servicios.repositorys.notificacion.NotificacionRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Slf4j
public class NotificacionEmailService {

    private static final int LONGITUD_MAXIMA_ERROR = 500;

    private final NotificacionRepository notificacionRepository;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final ObjectMapper objectMapper;
    private final boolean habilitado;
    private final String smtpHost;
    private final String remitente;
    private final int maxIntentos;

    @Autowired
    public NotificacionEmailService(
            NotificacionRepository notificacionRepository,
            ObjectProvider<JavaMailSender> mailSenderProvider,
            ObjectMapper objectMapper,
            @Value("${notificaciones.correo.habilitado:true}") boolean habilitado,
            @Value("${notificaciones.correo.smtp-host:}") String smtpHost,
            @Value("${notificaciones.correo.remitente:no-reply@gestopago.mx}") String remitente,
            @Value("${notificaciones.correo.max-intentos:3}") int maxIntentos) {
        this.notificacionRepository = notificacionRepository;
        this.mailSenderProvider = mailSenderProvider;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.habilitado = habilitado;
        this.smtpHost = smtpHost;
        this.remitente = remitente;
        this.maxIntentos = maxIntentos;
    }

    public NotificacionEmailService(
            NotificacionRepository notificacionRepository,
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${notificaciones.correo.habilitado:true}") boolean habilitado,
            @Value("${notificaciones.correo.smtp-host:}") String smtpHost,
            @Value("${notificaciones.correo.remitente:no-reply@gestopago.mx}") String remitente,
            @Value("${notificaciones.correo.max-intentos:3}") int maxIntentos) {
        this(notificacionRepository, mailSenderProvider, new ObjectMapper(), habilitado, smtpHost, remitente, maxIntentos);
    }

    @Async("notificacionExecutor")
    public void enviarAsync(Long notificacionId) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (!habilitado || !StringUtils.hasText(smtpHost) || mailSender == null) {
            notificacionRepository.actualizarEstadoEmail(
                    notificacionId,
                    EstadoCorreoNotificacion.OMITIDO,
                    "SMTP no configurado",
                    Instant.now());
            return;
        }

        Instant ahora = Instant.now();
        int reclamada = notificacionRepository.reclamarEnvio(
                notificacionId,
                EstadoCorreoNotificacion.ENVIANDO,
                List.of(EstadoCorreoNotificacion.PENDIENTE, EstadoCorreoNotificacion.FALLIDO),
                maxIntentos,
                ahora);
        if (reclamada == 0) {
            return;
        }

        Notificacion notificacion = notificacionRepository.findById(notificacionId).orElse(null);
        if (notificacion == null) {
            return;
        }

        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
            helper.setFrom(remitente);
            helper.setTo(notificacion.getEmail());
            helper.setSubject(notificacion.getTitulo());
            helper.setText(textoPlano(notificacion), html(notificacion));
            mailSender.send(mensaje);
            notificacionRepository.actualizarEstadoEmail(
                    notificacionId, EstadoCorreoNotificacion.ENVIADO, null, Instant.now());
            log.info("Correo de notificacion {} enviado a {}", notificacionId, notificacion.getEmail());
        } catch (Exception e) {
            notificacionRepository.actualizarEstadoEmail(
                    notificacionId,
                    EstadoCorreoNotificacion.FALLIDO,
                    truncar(e.getMessage()),
                    Instant.now());
            log.warn("Fallo el envio del correo de la notificacion {} a {}: {}",
                    notificacionId, notificacion.getEmail(), e.getMessage());
        }
    }

    private String textoPlano(Notificacion notificacion) {
        StringBuilder sb = new StringBuilder();
        sb.append(notificacion.getTitulo()).append("\n\n");
        sb.append(notificacion.getCuerpo()).append("\n");

        Map<String, Object> datos = extraerDatos(notificacion);
        String codigoError = primeroNoVacio(
                datos.get("codigoErrorGestoPago"), datos.get("codigoError"), datos.get("codigo"));
        String motivoRechazo = primeroNoVacio(
                datos.get("motivoRechazo"), datos.get("detalleError"), datos.get("detalle"));

        if (esNotificacionRechazada(notificacion, datos)) {
            sb.append("\n--- Detalle del Rechazo ---\n");
            if (codigoError != null && !codigoError.isBlank()) {
                sb.append("Codigo GestoPago: ").append(codigoError).append("\n");
            }
            if (motivoRechazo != null && !motivoRechazo.isBlank()) {
                sb.append("Motivo del rechazo: ").append(motivoRechazo).append("\n");
            }
            sb.append("Estado de tu saldo: Tu dinero no fue debitado o fue liberado en tu cuenta.\n");
            sb.append("---------------------------\n");
        }

        sb.append("\nGesto Pago");
        return sb.toString();
    }

    private String html(Notificacion notificacion) {
        String titulo = HtmlUtils.htmlEscape(notificacion.getTitulo());
        String cuerpo = HtmlUtils.htmlEscape(notificacion.getCuerpo());

        Map<String, Object> datos = extraerDatos(notificacion);
        String codigoError = primeroNoVacio(
                datos.get("codigoErrorGestoPago"), datos.get("codigoError"), datos.get("codigo"));
        String motivoRechazo = primeroNoVacio(
                datos.get("motivoRechazo"), datos.get("detalleError"), datos.get("detalle"));

        StringBuilder detalleRechazoHtml = new StringBuilder();
        if (esNotificacionRechazada(notificacion, datos)) {
            detalleRechazoHtml.append("""
                <div style="margin-top:20px;padding:16px;background:#fef2f2;border:1px solid #fecaca;border-radius:10px;">
                  <p style="margin:0 0 10px;font-size:14px;font-weight:bold;color:#991b1b;">Detalles del rechazo</p>
            """);
            if (codigoError != null && !codigoError.isBlank()) {
                detalleRechazoHtml.append("""
                  <p style="margin:0 0 8px;font-size:13px;color:#7f1d1d;">
                    <strong>Código GestoPago:</strong> <span style="font-family:monospace;background:#fee2e2;padding:2px 8px;border-radius:4px;font-weight:bold;">%s</span>
                  </p>
                """.formatted(HtmlUtils.htmlEscape(codigoError)));
            }
            if (motivoRechazo != null && !motivoRechazo.isBlank()) {
                detalleRechazoHtml.append("""
                  <p style="margin:0 0 8px;font-size:13px;color:#7f1d1d;">
                    <strong>Motivo del rechazo:</strong> %s
                  </p>
                """.formatted(HtmlUtils.htmlEscape(motivoRechazo)));
            }
            detalleRechazoHtml.append("""
                  <p style="margin:10px 0 0;font-size:13px;color:#15803d;font-weight:600;background:#f0fdf4;padding:8px 12px;border-radius:6px;border:1px solid #bbf7d0;">
                    ✓ Estado de tu saldo: Tu dinero no fue debitado o fue liberado y permanece disponible en tu cuenta.
                  </p>
                </div>
            """);
        }

        return """
                <!doctype html>
                <html lang="es">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>%s</title>
                </head>
                <body style="margin:0;padding:24px;background:#f4f6fb;font-family:Arial,Helvetica,sans-serif;color:#1f2937;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:560px;margin:0 auto;background:#ffffff;border-radius:12px;overflow:hidden;">
                    <tr>
                      <td style="background:#0f766e;padding:20px 24px;color:#ffffff;font-size:18px;font-weight:bold;">Gesto Pago</td>
                    </tr>
                    <tr>
                      <td style="padding:24px;">
                        <h1 style="margin:0 0 12px;font-size:20px;color:#0f172a;">%s</h1>
                        <p style="margin:0;font-size:15px;line-height:1.6;">%s</p>
                        %s
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:16px 24px;background:#f8fafc;color:#64748b;font-size:12px;">
                        Este correo se genero automaticamente. No respondas a este mensaje ni compartas tus contrasenas o PIN.
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(titulo, titulo, cuerpo, detalleRechazoHtml.toString());
    }

    private boolean esNotificacionRechazada(Notificacion notificacion, Map<String, Object> datos) {
        String estado = datos.get("estado") != null ? datos.get("estado").toString().toUpperCase(Locale.ROOT) : "";
        if ("REVERSADO".equals(estado) || "REVERSANDO".equals(estado) || "REQUIERE_REVISION".equals(estado)) {
            return true;
        }
        if (datos.containsKey("codigoErrorGestoPago") || datos.containsKey("motivoRechazo")) {
            return true;
        }
        String t = notificacion.getTitulo() != null ? notificacion.getTitulo().toLowerCase(Locale.ROOT) : "";
        return t.contains("rechaz") || t.contains("revers") || t.contains("fallid");
    }

    private Map<String, Object> extraerDatos(Notificacion notificacion) {
        if (notificacion.getDatos() == null || notificacion.getDatos().isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(notificacion.getDatos(), new TypeReference<>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private static String primeroNoVacio(Object... valores) {
        for (Object v : valores) {
            if (v != null) {
                String str = v.toString().trim();
                if (!str.isBlank()) {
                    return str;
                }
            }
        }
        return null;
    }

    private String truncar(String mensaje) {
        if (mensaje == null) {
            return "Error desconocido";
        }
        return mensaje.length() <= LONGITUD_MAXIMA_ERROR
                ? mensaje
                : mensaje.substring(0, LONGITUD_MAXIMA_ERROR);
    }
}
