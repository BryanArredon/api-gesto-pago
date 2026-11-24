package com.proyecto.servicios.service.notificacion;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.entity.auth.Usuario;
import com.proyecto.servicios.entity.notificacion.EstadoCorreoNotificacion;
import com.proyecto.servicios.entity.notificacion.Notificacion;
import com.proyecto.servicios.entity.notificacion.TipoNotificacion;
import com.proyecto.servicios.entity.pago.EstadoTransaccion;
import com.proyecto.servicios.evento.MovimientoRegistradoEvent;
import com.proyecto.servicios.model.notificacion.NotificacionDto;
import com.proyecto.servicios.model.notificacion.NotificacionPaginaDto;
import com.proyecto.servicios.repositorys.auth.UsuarioRepository;
import com.proyecto.servicios.repositorys.notificacion.NotificacionRepository;
import com.proyecto.servicios.service.security.ServicioIdentidadSso;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NotificacionService {

    private static final int TAMANIO_MAXIMO = 100;

    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final ServicioIdentidadSso servicioIdentidadSso;
    private final NotificacionBroadcaster broadcaster;
    private final ObjectMapper objectMapper;
    private final boolean correoHabilitado;
    private final String smtpHost;
    private final Set<EstadoTransaccion> estadosCorreo;

    public NotificacionService(
            NotificacionRepository notificacionRepository,
            UsuarioRepository usuarioRepository,
            ServicioIdentidadSso servicioIdentidadSso,
            NotificacionBroadcaster broadcaster,
            ObjectMapper objectMapper,
            @Value("${notificaciones.correo.habilitado:true}") boolean correoHabilitado,
            @Value("${notificaciones.correo.smtp-host:}") String smtpHost,
            @Value("${notificaciones.correo.estados:LIQUIDADO,REVERSADO,REQUIERE_REVISION}") String estadosCsv) {
        this.notificacionRepository = notificacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.servicioIdentidadSso = servicioIdentidadSso;
        this.broadcaster = broadcaster;
        this.objectMapper = objectMapper;
        this.correoHabilitado = correoHabilitado;
        this.smtpHost = smtpHost;
        this.estadosCorreo = parsearEstados(estadosCsv);
    }

    @Transactional
    public Optional<Notificacion> registrarMovimiento(MovimientoRegistradoEvent evento) {
        if (evento == null || evento.usuarioId() == null || evento.transaccionId() == null) {
            return Optional.empty();
        }
        Usuario usuario = usuarioRepository.findById(evento.usuarioId()).orElse(null);
        if (usuario == null) {
            log.warn("No se genero notificacion: usuario {} inexistente", evento.usuarioId());
            return Optional.empty();
        }

        ContenidoNotificacion.Contenido contenido = ContenidoNotificacion.crear(evento);
        String dedupeKey = "tx:" + evento.transaccionId() + ":" + evento.estado().name();
        boolean correo = estadosCorreo.contains(evento.estado());
        return guardar(usuario.getId(), usuario.getEmail(), TipoNotificacion.TRANSACCION,
                contenido.titulo(), contenido.cuerpo(), evento.transaccionId(),
                contenido.datos(), dedupeKey, correo);
    }

    @Transactional
    public Optional<Notificacion> registrarPorEmail(
            String email,
            TipoNotificacion tipo,
            String titulo,
            String cuerpo,
            Map<String, Object> datos,
            String dedupeKey,
            boolean correo) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(titulo) || !StringUtils.hasText(cuerpo)) {
            return Optional.empty();
        }
        String correoNormalizado = email.trim().toLowerCase(Locale.ROOT);
        Long usuarioId;
        try {
            usuarioId = servicioIdentidadSso.obtenerOCrearUsuarioIdPorEmail(correoNormalizado);
        } catch (Exception e) {
            log.warn("No se pudo resolver el usuario {} para la notificacion: {}", email, e.getMessage());
            return Optional.empty();
        }
        String clave = StringUtils.hasText(dedupeKey)
                ? dedupeKey.trim()
                : "ext:" + java.util.UUID.randomUUID();
        TipoNotificacion tipoSeguro = tipo == null ? TipoNotificacion.SISTEMA : tipo;
        return guardar(usuarioId, correoNormalizado, tipoSeguro,
                titulo.trim(), cuerpo.trim(), null, datos == null ? Map.of() : datos, clave, correo);
    }

    public void enviarEnTiempoReal(Notificacion notificacion) {
        broadcaster.enviar(notificacion.getEmail(), aDto(notificacion));
    }

    @Transactional(readOnly = true)
    public NotificacionPaginaDto listar(Long usuarioId, int pagina, int tamanio, boolean soloNoLeidas) {
        int paginaSegura = Math.max(pagina, 0);
        int tamanioSeguro = Math.min(Math.max(tamanio, 1), TAMANIO_MAXIMO);
        PageRequest pageable = PageRequest.of(paginaSegura, tamanioSeguro);
        Page<Notificacion> resultado = soloNoLeidas
                ? notificacionRepository.findByUsuarioIdAndLeidaEnIsNullOrderByCreatedAtDesc(usuarioId, pageable)
                : notificacionRepository.findByUsuarioIdOrderByCreatedAtDesc(usuarioId, pageable);
        return new NotificacionPaginaDto(
                resultado.getContent().stream().map(this::aDto).toList(),
                resultado.getTotalElements(),
                notificacionRepository.countByUsuarioIdAndLeidaEnIsNull(usuarioId),
                paginaSegura,
                tamanioSeguro);
    }

    @Transactional(readOnly = true)
    public long contarNoLeidas(Long usuarioId) {
        return notificacionRepository.countByUsuarioIdAndLeidaEnIsNull(usuarioId);
    }

    @Transactional
    public boolean marcarLeida(Long usuarioId, Long id) {
        Notificacion notificacion = notificacionRepository.findByIdAndUsuarioId(id, usuarioId).orElse(null);
        if (notificacion == null) {
            return false;
        }
        if (notificacion.getLeidaEn() == null) {
            notificacionRepository.marcarLeida(id, usuarioId, Instant.now());
        }
        return true;
    }

    @Transactional
    public int marcarTodasLeidas(Long usuarioId) {
        return notificacionRepository.marcarTodasLeidas(usuarioId, Instant.now());
    }

    public NotificacionDto aDto(Notificacion notificacion) {
        return new NotificacionDto(
                notificacion.getId(),
                notificacion.getTipo().name(),
                notificacion.getTitulo(),
                notificacion.getCuerpo(),
                notificacion.getTransaccionId(),
                deserializar(notificacion.getDatos()),
                notificacion.getLeidaEn() != null,
                notificacion.getLeidaEn(),
                notificacion.getCreatedAt());
    }

    private Optional<Notificacion> guardar(
            Long usuarioId,
            String email,
            TipoNotificacion tipo,
            String titulo,
            String cuerpo,
            Long transaccionId,
            Map<String, Object> datos,
            String dedupeKey,
            boolean correo) {
        if (notificacionRepository.existsByUsuarioIdAndDedupeKey(usuarioId, dedupeKey)) {
            return Optional.empty();
        }
        Notificacion notificacion = new Notificacion();
        notificacion.setUsuarioId(usuarioId);
        notificacion.setEmail(email);
        notificacion.setTipo(tipo);
        notificacion.setTitulo(recortar(titulo, 160));
        notificacion.setCuerpo(cuerpo);
        notificacion.setTransaccionId(transaccionId);
        notificacion.setDedupeKey(recortar(dedupeKey, 160));
        notificacion.setDatos(serializar(datos));
        notificacion.setEmailEstado(decidirEstadoCorreo(correo));
        return Optional.of(notificacionRepository.save(notificacion));
    }

    private EstadoCorreoNotificacion decidirEstadoCorreo(boolean correo) {
        if (!correo || !correoHabilitado || !StringUtils.hasText(smtpHost)) {
            return EstadoCorreoNotificacion.OMITIDO;
        }
        return EstadoCorreoNotificacion.PENDIENTE;
    }

    private String recortar(String valor, int maximo) {
        if (valor == null) {
            return null;
        }
        return valor.length() <= maximo ? valor : valor.substring(0, maximo);
    }

    private String serializar(Map<String, Object> datos) {
        try {
            return objectMapper.writeValueAsString(datos);
        } catch (Exception e) {
            log.warn("No se pudo serializar el detalle de la notificacion: {}", e.getMessage());
            return null;
        }
    }

    private Map<String, Object> deserializar(String datos) {
        if (!StringUtils.hasText(datos)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(datos, new TypeReference<>() {
            });
        } catch (Exception e) {
            return Map.of();
        }
    }

    private static Set<EstadoTransaccion> parsearEstados(String estadosCsv) {
        if (!StringUtils.hasText(estadosCsv)) {
            return Set.of();
        }
        return Arrays.stream(estadosCsv.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(valor -> valor.toUpperCase(Locale.ROOT))
                .map(valor -> {
                    try {
                        return EstadoTransaccion.valueOf(valor);
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(estado -> estado != null)
                .collect(Collectors.toUnmodifiableSet());
    }
}
