package com.onboarding.clientes.cliente.application.service;

import com.onboarding.clientes.cliente.application.command.ComandoRegistrarCliente;
import com.onboarding.clientes.cliente.application.dto.ClienteDetalle;
import com.onboarding.clientes.cliente.application.mapper.ClienteMapeador;
import com.onboarding.clientes.cliente.application.port.ClienteRepositorio;
import com.onboarding.clientes.cliente.domain.exception.CurpDuplicadaException;
import com.onboarding.clientes.cliente.domain.model.Cliente;
import com.onboarding.clientes.cliente.domain.model.EstadoCivil;
import com.onboarding.clientes.cliente.domain.model.Sexo;
import com.onboarding.clientes.cliente.domain.model.TipoCuenta;
import com.onboarding.clientes.cliente.domain.valueobject.Curp;
import com.onboarding.clientes.config.PropiedadesOnboarding;
import com.onboarding.clientes.shared.evento.PublicadorEvento;
import com.onboarding.clientes.shared.seguridad.GeneradorHash;
import com.onboarding.clientes.shared.tiempo.Reloj;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioRegistroClienteTest {

    @Mock
    private ClienteRepositorio clientes;
    @Mock
    private GeneradorHash generadorHash;
    @Mock
    private PublicadorEvento publicadorEvento;
    @Mock
    private Reloj reloj;

    private ServicioRegistroCliente servicio;
    private final Instant momento = Instant.parse("2026-01-01T12:00:00Z");
    private final LocalDate hoy = LocalDate.of(2026, 1, 1);

    @BeforeEach
    void setUp() {
        PropiedadesOnboarding props = new PropiedadesOnboarding(
                new PropiedadesOnboarding.Cuenta(BigDecimal.ZERO, "MXN", "AHORRO", true),
                new PropiedadesOnboarding.Seguridad(4, 6, 5, 15, 8),
                new PropiedadesOnboarding.Paginacion(20, 100),
                new PropiedadesOnboarding.Token("onboarding-clientes", Duration.ofMinutes(15), Duration.ofDays(7), "", "", "", ""),
                new PropiedadesOnboarding.Keycloak(false, "", "", "", "", null)
        );

        servicio = new ServicioRegistroCliente(
                clientes,
                generadorHash,
                publicadorEvento,
                reloj,
                props,
                new ClienteMapeador());

        lenient().when(reloj.ahora()).thenReturn(momento);
        lenient().when(reloj.hoy(any(ZoneId.class))).thenReturn(hoy);
    }

    @Test
    @DisplayName("Debe registrar cliente exitosamente y guardar en repositorio")
    void debeRegistrarClienteExitosamente() {
        when(clientes.existePorCurp(any())).thenReturn(false);
        when(clientes.existePorRfc(any())).thenReturn(false);
        when(clientes.existePorCorreo(any())).thenReturn(false);
        when(generadorHash.hash(any())).thenReturn("$2a$12$hashedPassword");
        when(clientes.guardar(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        ComandoRegistrarCliente comando = new ComandoRegistrarCliente(
                new ComandoRegistrarCliente.Persona("Juan", "Francisco", "Manzano", "Garcia", LocalDate.of(1995, 5, 15), "MAGJ950515HDFRRN01", "MAGJ950515ABC", Sexo.HOMBRE, "Mexicana", EstadoCivil.SOLTERO),
                new ComandoRegistrarCliente.Contacto("juan.manzano@ejemplo.com", "4181234567", null),
                new ComandoRegistrarCliente.Ubicacion("Av. Principal", "100", null, "Centro", "Dolores Hidalgo", "Guanajuato", "37800", "México"),
                new ComandoRegistrarCliente.Laboral("Desarrollador", "Tech Co", new BigDecimal("30000.00"), "MXN"),
                new ComandoRegistrarCliente.Credenciales("Segura123!", "1234"),
                new ComandoRegistrarCliente.AperturaCuenta(new BigDecimal("1000.00"), TipoCuenta.AHORRO, "MXN")
        );

        ClienteDetalle detalle = servicio.registrar(comando);

        assertThat(detalle).isNotNull();
        assertThat(detalle.nombreCompleto()).isEqualTo("Juan Francisco Manzano Garcia");
        assertThat(detalle.cuenta().saldo()).isEqualByComparingTo("1000.00");

        verify(clientes).guardar(any(Cliente.class));
        verify(publicadorEvento).publicar(any());
    }

    @Test
    @DisplayName("Debe fallar si la CURP ya está registrada")
    void debeFallarCurpDuplicada() {
        when(clientes.existePorCurp(any(Curp.class))).thenReturn(true);

        ComandoRegistrarCliente comando = new ComandoRegistrarCliente(
                new ComandoRegistrarCliente.Persona("Juan", "Francisco", "Manzano", "Garcia", LocalDate.of(1995, 5, 15), "MAGJ950515HDFRRN01", "MAGJ950515ABC", Sexo.HOMBRE, "Mexicana", EstadoCivil.SOLTERO),
                new ComandoRegistrarCliente.Contacto("juan.manzano@ejemplo.com", "4181234567", null),
                new ComandoRegistrarCliente.Ubicacion("Av. Principal", "100", null, "Centro", "Dolores Hidalgo", "Guanajuato", "37800", "México"),
                new ComandoRegistrarCliente.Laboral("Desarrollador", "Tech Co", new BigDecimal("30000.00"), "MXN"),
                new ComandoRegistrarCliente.Credenciales("Segura123!", "1234"),
                new ComandoRegistrarCliente.AperturaCuenta(new BigDecimal("1000.00"), TipoCuenta.AHORRO, "MXN")
        );

        assertThatThrownBy(() -> servicio.registrar(comando))
                .isInstanceOf(CurpDuplicadaException.class);
    }
}
