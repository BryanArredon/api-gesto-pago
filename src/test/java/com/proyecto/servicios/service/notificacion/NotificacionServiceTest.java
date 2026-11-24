package com.proyecto.servicios.service.notificacion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.entity.auth.Usuario;
import com.proyecto.servicios.entity.notificacion.EstadoCorreoNotificacion;
import com.proyecto.servicios.entity.notificacion.Notificacion;
import com.proyecto.servicios.entity.pago.EstadoTransaccion;
import com.proyecto.servicios.evento.MovimientoRegistradoEvent;
import com.proyecto.servicios.entity.notificacion.TipoNotificacion;
import com.proyecto.servicios.repositorys.auth.UsuarioRepository;
import com.proyecto.servicios.repositorys.notificacion.NotificacionRepository;
import com.proyecto.servicios.service.security.ServicioIdentidadSso;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceTest {

    @Mock
    private NotificacionRepository notificacionRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ServicioIdentidadSso servicioIdentidadSso;

    @Mock
    private NotificacionBroadcaster broadcaster;

    private NotificacionService service;

    @BeforeEach
    void setUp() {
        service = new NotificacionService(
                notificacionRepository,
                usuarioRepository,
                servicioIdentidadSso,
                broadcaster,
                new ObjectMapper(),
                true,
                "smtp.test",
                "LIQUIDADO,REVERSADO");
    }

    @Test
    void generaNotificacionYCorreoParaEstadoFinal() {
        Usuario usuario = usuario();
        when(notificacionRepository.existsByUsuarioIdAndDedupeKey(7L, "tx:99:LIQUIDADO"))
                .thenReturn(false);
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        when(notificacionRepository.save(any(Notificacion.class))).thenAnswer(invocacion -> {
            Notificacion notificacion = invocacion.getArgument(0);
            notificacion.setId(1L);
            notificacion.setCreatedAt(Instant.now());
            notificacion.setUpdatedAt(Instant.now());
            return notificacion;
        });

        Optional<Notificacion> creada = service.registrarMovimiento(evento(EstadoTransaccion.LIQUIDADO));

        assertTrue(creada.isPresent());
        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(captor.capture());
        Notificacion notificacion = captor.getValue();
        assertEquals("Pago exitoso", notificacion.getTitulo());
        assertEquals("cliente@test.com", notificacion.getEmail());
        assertEquals(EstadoCorreoNotificacion.PENDIENTE, notificacion.getEmailEstado());
        assertEquals("tx:99:LIQUIDADO", notificacion.getDedupeKey());
    }

    @Test
    void omiteCorreoCuandoElEstadoNoEstaConfigurado() {
        Usuario usuario = usuario();
        when(notificacionRepository.existsByUsuarioIdAndDedupeKey(7L, "tx:99:PENDIENTE"))
                .thenReturn(false);
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        when(notificacionRepository.save(any(Notificacion.class))).thenAnswer(invocacion -> {
            Notificacion notificacion = invocacion.getArgument(0);
            notificacion.setId(2L);
            notificacion.setCreatedAt(Instant.now());
            notificacion.setUpdatedAt(Instant.now());
            return notificacion;
        });

        Optional<Notificacion> creada = service.registrarMovimiento(evento(EstadoTransaccion.PENDIENTE));

        assertTrue(creada.isPresent());
        assertEquals(EstadoCorreoNotificacion.OMITIDO, creada.get().getEmailEstado());
    }

    @Test
    void registraNotificacionExternaPorCorreo() {
        when(servicioIdentidadSso.obtenerOCrearUsuarioIdPorEmail("cliente@test.com")).thenReturn(7L);
        when(notificacionRepository.existsByUsuarioIdAndDedupeKey(7L, "evt:123")).thenReturn(false);
        when(notificacionRepository.save(any(Notificacion.class))).thenAnswer(invocacion -> {
            Notificacion notificacion = invocacion.getArgument(0);
            notificacion.setId(3L);
            notificacion.setCreatedAt(Instant.now());
            notificacion.setUpdatedAt(Instant.now());
            return notificacion;
        });

        Optional<Notificacion> creada = service.registrarPorEmail(
                "Cliente@Test.com",
                TipoNotificacion.SEGURIDAD,
                "Nuevo inicio de sesion",
                "Se inicio sesion en tu cuenta.",
                java.util.Map.of("canal", "WEB"),
                "evt:123",
                true);

        assertTrue(creada.isPresent());
        assertEquals(TipoNotificacion.SEGURIDAD, creada.get().getTipo());
        assertEquals(EstadoCorreoNotificacion.PENDIENTE, creada.get().getEmailEstado());
        assertEquals("cliente@test.com", creada.get().getEmail());
    }

    @Test
    void noDuplicaNotificacionesParaElMismoEstado() {
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario()));
        when(notificacionRepository.existsByUsuarioIdAndDedupeKey(7L, "tx:99:LIQUIDADO"))
                .thenReturn(true);

        Optional<Notificacion> creada = service.registrarMovimiento(evento(EstadoTransaccion.LIQUIDADO));

        assertTrue(creada.isEmpty());
        verify(notificacionRepository, never()).save(any(Notificacion.class));
    }

    @Test
    void generaNotificacionConDetalleRechazoYCodigoGestoPago() {
        Usuario usuario = usuario();
        when(notificacionRepository.existsByUsuarioIdAndDedupeKey(7L, "tx:99:REVERSADO"))
                .thenReturn(false);
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        when(notificacionRepository.save(any(Notificacion.class))).thenAnswer(invocacion -> {
            Notificacion notificacion = invocacion.getArgument(0);
            notificacion.setId(10L);
            notificacion.setCreatedAt(Instant.now());
            notificacion.setUpdatedAt(Instant.now());
            return notificacion;
        });

        MovimientoRegistradoEvent eventoRechazado = new MovimientoRegistradoEvent(
                99L,
                7L,
                EstadoTransaccion.REVERSADO,
                "PROVEEDOR",
                "Carrier no responde",
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                "Telcel",
                "Recarga 100",
                "5512345678",
                "03",
                "El carrier no responde en este momento",
                "Por el momento el carrier no responde");

        Optional<Notificacion> creada = service.registrarMovimiento(eventoRechazado);

        assertTrue(creada.isPresent());
        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(captor.capture());
        Notificacion notificacion = captor.getValue();
        assertEquals("Pago rechazado", notificacion.getTitulo());
        assertTrue(notificacion.getCuerpo().contains("Código GestoPago: 03"));
        assertTrue(notificacion.getCuerpo().contains("Por que se rechazo:"));
        assertTrue(notificacion.getCuerpo().contains("liberado"));
        assertTrue(notificacion.getDatos().contains("codigoErrorGestoPago"));
        assertTrue(notificacion.getDatos().contains("03"));
        assertTrue(notificacion.getDatos().contains("motivoRechazo"));
    }

    private static MovimientoRegistradoEvent evento(EstadoTransaccion estado) {
        return new MovimientoRegistradoEvent(
                99L,
                7L,
                estado,
                "PROVEEDOR",
                "Operacion realizada con exito",
                new BigDecimal("150.00"),
                new BigDecimal("6.00"),
                "CFE",
                "Pago de servicio",
                "1234567890");
    }

    private static Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setEmail("cliente@test.com");
        return usuario;
    }
}
