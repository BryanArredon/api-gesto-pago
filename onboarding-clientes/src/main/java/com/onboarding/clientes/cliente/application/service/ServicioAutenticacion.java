package com.onboarding.clientes.cliente.application.service;

import com.onboarding.clientes.cliente.application.command.ComandoCambiarPassword;
import com.onboarding.clientes.cliente.application.command.ComandoLogin;
import com.onboarding.clientes.cliente.application.dto.LoginRespuesta;
import com.onboarding.clientes.cliente.application.dto.UsuarioDto;
import com.onboarding.clientes.cliente.application.mapper.ClienteMapeador;
import com.onboarding.clientes.cliente.application.port.UsuarioRepositorio;
import com.onboarding.clientes.cliente.domain.exception.ContraseniaInvalidaException;
import com.onboarding.clientes.cliente.domain.exception.CredencialesInvalidasException;
import com.onboarding.clientes.cliente.domain.exception.UsuarioBloqueadoException;
import com.onboarding.clientes.cliente.domain.exception.UsuarioInactivoException;
import com.onboarding.clientes.cliente.domain.exception.UsuarioNoEncontradoException;
import com.onboarding.clientes.cliente.domain.model.Usuario;
import com.onboarding.clientes.cliente.domain.valueobject.Correo;
import com.onboarding.clientes.config.JwtTokenService;
import com.onboarding.clientes.config.PropiedadesOnboarding;
import com.onboarding.clientes.shared.evento.EventoDominio;
import com.onboarding.clientes.shared.evento.PublicadorEvento;
import com.onboarding.clientes.shared.id.UuidV7;
import com.onboarding.clientes.shared.seguridad.GeneradorHash;
import com.onboarding.clientes.shared.seguridad.PoliticaCredenciales;
import com.onboarding.clientes.shared.tiempo.Reloj;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de aplicacion para la autenticacion y gestion de usuarios de acceso.
 */
@Service
public class ServicioAutenticacion {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServicioAutenticacion.class);

    private final UsuarioRepositorio usuarios;
    private final GeneradorHash generadorHash;
    private final JwtTokenService jwtTokenService;
    private final ClienteMapeador mapeador;
    private final PublicadorEvento publicadorEvento;
    private final Reloj reloj;
    private final PropiedadesOnboarding propiedades;

    public ServicioAutenticacion(
            UsuarioRepositorio usuarios,
            GeneradorHash generadorHash,
            JwtTokenService jwtTokenService,
            ClienteMapeador mapeador,
            PublicadorEvento publicadorEvento,
            Reloj reloj,
            PropiedadesOnboarding propiedades) {
        this.usuarios = usuarios;
        this.generadorHash = generadorHash;
        this.jwtTokenService = jwtTokenService;
        this.mapeador = mapeador;
        this.publicadorEvento = publicadorEvento;
        this.reloj = reloj;
        this.propiedades = propiedades;
    }

    @Transactional
    public LoginRespuesta login(ComandoLogin comando) {
        Correo correo = Correo.de(comando.correo());
        Instant ahora = reloj.ahora();

        Usuario usuario = usuarios.buscarPorCorreo(correo)
                .orElseThrow(CredencialesInvalidasException::new);

        if (!usuario.activo()) {
            throw new UsuarioInactivoException(correo.valor());
        }

        if (usuario.estaBloqueado(ahora)) {
            throw new UsuarioBloqueadoException(
                    "El usuario esta temporalmente bloqueado por superar los intentos fallidos. Intente mas tarde.");
        }

        boolean coincide = generadorHash.coincide(comando.contrasenia(), usuario.hashContrasena());
        if (!coincide) {
            usuario.registrarIntentoFallido(ahora);
            usuarios.guardar(usuario);
            LOGGER.warn("Intento de login fallido para el usuario: {}", correo.valor());
            throw new CredencialesInvalidasException();
        }

        usuario.registrarAccesoExitoso(ahora);
        usuarios.guardar(usuario);

        String accessToken = jwtTokenService.generarAccessToken(usuario);
        String refreshToken = jwtTokenService.generarRefreshToken(usuario, UuidV7.generar());

        publicadorEvento.publicar(EventoDominio.crear(
                "usuario.login_exitoso",
                EventoDominio.TIPO_USUARIO,
                usuario.id(),
                Map.of("correo", correo.valor(), "ipOrigen", comando.ipOrigen() != null ? comando.ipOrigen() : "desconocida"),
                "sistema",
                ahora));

        LOGGER.info("Login exitoso para usuario: {}", correo.valor());
        return new LoginRespuesta(
                accessToken,
                refreshToken,
                "Bearer",
                jwtTokenService.getDuracionAccesoSegundos(),
                usuario.clienteId(),
                usuario.id());
    }

    @Transactional(readOnly = true)
    public UsuarioDto obtenerPorId(UUID id) {
        return usuarios.buscarPorId(id)
                .map(mapeador::aDto)
                .orElseThrow(() -> new UsuarioNoEncontradoException(id));
    }

    @Transactional
    public void cambiarContrasenia(ComandoCambiarPassword comando) {
        Usuario usuario = usuarios.buscarPorId(comando.usuarioId())
                .orElseThrow(() -> new UsuarioNoEncontradoException(comando.usuarioId()));

        if (!usuario.activo()) {
            throw new UsuarioInactivoException(usuario.correo().valor());
        }

        if (!generadorHash.coincide(comando.contraseniaActual(), usuario.hashContrasena())) {
            throw new CredencialesInvalidasException();
        }

        int minLen = propiedades.seguridad().longitudMinimaContrasenia();
        PoliticaCredenciales.validarContrasenia(comando.nuevaContrasenia(), minLen);

        String nuevoHash = generadorHash.hash(comando.nuevaContrasenia());
        usuario.cambiarContrasena(nuevoHash);
        usuario.marcarActualizada(reloj.ahora());
        usuarios.guardar(usuario);

        publicadorEvento.publicar(EventoDominio.crear(
                "usuario.password_actualizado",
                EventoDominio.TIPO_USUARIO,
                usuario.id(),
                Map.of("correo", usuario.correo().valor()),
                "sistema",
                reloj.ahora()));

        LOGGER.info("Contrasena actualizada exitosamente para el usuario id={}", usuario.id());
    }
}
