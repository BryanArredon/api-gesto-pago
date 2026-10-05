package com.onboarding.clientes.cliente.application.service;

import com.onboarding.clientes.cliente.application.command.ComandoLogin;
import com.onboarding.clientes.cliente.application.dto.LoginRespuesta;
import com.onboarding.clientes.cliente.application.mapper.ClienteMapeador;
import com.onboarding.clientes.cliente.application.port.UsuarioRepositorio;
import com.onboarding.clientes.cliente.domain.exception.CredencialesInvalidasException;
import com.onboarding.clientes.cliente.domain.exception.UsuarioInactivoException;
import com.onboarding.clientes.cliente.domain.model.Usuario;
import com.onboarding.clientes.cliente.domain.valueobject.Correo;
import com.onboarding.clientes.config.JwtTokenService;
import com.onboarding.clientes.config.PropiedadesOnboarding;
import com.onboarding.clientes.shared.evento.PublicadorEvento;
import com.onboarding.clientes.shared.seguridad.GeneradorHash;
import com.onboarding.clientes.shared.tiempo.Reloj;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioAutenticacionTest {

    @Mock
    private UsuarioRepositorio usuarios;
    @Mock
    private GeneradorHash generadorHash;
    @Mock
    private JwtTokenService jwtTokenService;
    @Mock
    private PublicadorEvento publicadorEvento;
    @Mock
    private Reloj reloj;

    private ServicioAutenticacion servicio;
    private final Instant momento = Instant.parse("2026-01-01T12:00:00Z");

    @BeforeEach
    void setUp() {
        PropiedadesOnboarding props = new PropiedadesOnboarding(
                new PropiedadesOnboarding.Cuenta(java.math.BigDecimal.ZERO, "MXN", "AHORRO", true),
                new PropiedadesOnboarding.Seguridad(4, 6, 5, 15, 8),
                new PropiedadesOnboarding.Paginacion(20, 100),
                new PropiedadesOnboarding.Token("onboarding-clientes", Duration.ofMinutes(15), Duration.ofDays(7), "", "", "", ""),
                new PropiedadesOnboarding.Keycloak(false, "", "", "", "", null)
        );

        servicio = new ServicioAutenticacion(
                usuarios,
                generadorHash,
                jwtTokenService,
                new ClienteMapeador(),
                publicadorEvento,
                reloj,
                props);

        when(reloj.ahora()).thenReturn(momento);
    }

    @Test
    @DisplayName("Debe autenticar correctamente con credenciales válidas")
    void debeAutenticarCorrectamente() {
        UUID clienteId = UUID.randomUUID();
        Correo correo = Correo.de("juan.manzano@ejemplo.com");
        Usuario usuario = Usuario.crear(clienteId, correo, "$2a$12$hashedPassword", null, momento);

        when(usuarios.buscarPorCorreo(correo)).thenReturn(Optional.of(usuario));
        when(generadorHash.coincide("Segura123!", "$2a$12$hashedPassword")).thenReturn(true);
        when(jwtTokenService.generarAccessToken(any())).thenReturn("mocked.access.jwt");
        when(jwtTokenService.generarRefreshToken(any(), any())).thenReturn("mocked.refresh.jwt");
        when(jwtTokenService.getDuracionAccesoSegundos()).thenReturn(900L);

        ComandoLogin comando = new ComandoLogin("juan.manzano@ejemplo.com", "Segura123!", "127.0.0.1", "Postman");
        LoginRespuesta respuesta = servicio.login(comando);

        assertThat(respuesta).isNotNull();
        assertThat(respuesta.accessToken()).isEqualTo("mocked.access.jwt");
        assertThat(respuesta.refreshToken()).isEqualTo("mocked.refresh.jwt");
        assertThat(respuesta.clienteId()).isEqualTo(clienteId);
    }

    @Test
    @DisplayName("Debe fallar ante contraseña incorrecta")
    void debeFallarContraseniaIncorrecta() {
        UUID clienteId = UUID.randomUUID();
        Correo correo = Correo.de("juan.manzano@ejemplo.com");
        Usuario usuario = Usuario.crear(clienteId, correo, "$2a$12$hashedPassword", null, momento);

        when(usuarios.buscarPorCorreo(correo)).thenReturn(Optional.of(usuario));
        when(generadorHash.coincide("WrongPassword", "$2a$12$hashedPassword")).thenReturn(false);

        ComandoLogin comando = new ComandoLogin("juan.manzano@ejemplo.com", "WrongPassword", "127.0.0.1", "Postman");

        assertThatThrownBy(() -> servicio.login(comando))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    @DisplayName("Debe denegar acceso si el usuario está inactivo")
    void debeDenegarAccesoUsuarioInactivo() {
        UUID clienteId = UUID.randomUUID();
        Correo correo = Correo.de("inactivo@ejemplo.com");
        Usuario usuario = Usuario.crear(clienteId, correo, "$2a$12$hashedPassword", null, momento);
        usuario.darDeBaja();

        when(usuarios.buscarPorCorreo(correo)).thenReturn(Optional.of(usuario));

        ComandoLogin comando = new ComandoLogin("inactivo@ejemplo.com", "Segura123!", "127.0.0.1", "Postman");

        assertThatThrownBy(() -> servicio.login(comando))
                .isInstanceOf(UsuarioInactivoException.class);
    }
}
