package com.proyecto.servicios.service.notificacion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.entity.notificacion.EstadoCorreoNotificacion;
import com.proyecto.servicios.entity.notificacion.Notificacion;
import com.proyecto.servicios.repositorys.notificacion.NotificacionRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacionEmailServiceTest {

    @Mock
    private NotificacionRepository notificacionRepository;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;

    private NotificacionEmailService service;

    @BeforeEach
    void setUp() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        service = new NotificacionEmailService(
                notificacionRepository,
                mailSenderProvider,
                new ObjectMapper(),
                true,
                "smtp.example.com",
                "no-reply@gestopago.mx",
                3);
    }

    @Test
    void correoParaTransaccionRechazadaMuestraCodigoYMotivoEnHtmlYTexto() throws Exception {
        Notificacion notificacion = new Notificacion();
        notificacion.setId(100L);
        notificacion.setEmail("cliente@test.com");
        notificacion.setTitulo("Pago rechazado");
        notificacion.setCuerpo("Tu pago de Telcel - Recarga 100 fue rechazado. Por que se rechazo: El carrier no responde.");
        notificacion.setDatos("""
                {
                  "estado": "REVERSADO",
                  "codigoErrorGestoPago": "03",
                  "motivoRechazo": "El carrier de telecomunicaciones no responde",
                  "detalleError": "Carrier timeout"
                }
                """);
        notificacion.setEmailEstado(EstadoCorreoNotificacion.PENDIENTE);

        when(notificacionRepository.reclamarEnvio(eq(100L), any(), any(), eq(3), any()))
                .thenReturn(1);
        when(notificacionRepository.findById(100L)).thenReturn(Optional.of(notificacion));

        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        service.enviarAsync(100L);

        verify(mailSender).send(mimeMessage);
        verify(notificacionRepository).actualizarEstadoEmail(
                eq(100L), eq(EstadoCorreoNotificacion.ENVIADO), eq(null), any(Instant.class));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        mimeMessage.writeTo(baos);
        String correoCompleto = baos.toString(StandardCharsets.UTF_8);

        assertTrue(correoCompleto.contains("03"), "El correo debe contener el codigo de error de GestoPago");
        assertTrue(correoCompleto.contains("carrier") || correoCompleto.contains("Carrier"), "El correo debe contener el motivo claro del rechazo");
        assertTrue(correoCompleto.contains("saldo") || correoCompleto.contains("liberado"), "El correo debe informar sobre el saldo");
    }
}

