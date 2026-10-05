package com.onboarding.clientes.cliente.application.service;

import com.onboarding.clientes.cliente.application.command.ComandoActualizarCliente;
import com.onboarding.clientes.cliente.application.dto.ClienteDetalle;
import com.onboarding.clientes.cliente.application.mapper.ClienteMapeador;
import com.onboarding.clientes.cliente.application.port.ClienteRepositorio;
import com.onboarding.clientes.cliente.domain.exception.ClienteNoEncontradoException;
import com.onboarding.clientes.cliente.domain.exception.CorreoDuplicadoException;
import com.onboarding.clientes.cliente.domain.model.AltaCliente;
import com.onboarding.clientes.cliente.domain.model.Cliente;
import com.onboarding.clientes.cliente.domain.model.DatosContacto;
import com.onboarding.clientes.cliente.domain.model.DatosLaborales;
import com.onboarding.clientes.cliente.domain.model.DatosPersonales;
import com.onboarding.clientes.cliente.domain.model.Domicilio;
import com.onboarding.clientes.cliente.domain.model.EstadoCivil;
import com.onboarding.clientes.cliente.domain.model.Sexo;
import com.onboarding.clientes.cliente.domain.model.TipoCuenta;
import com.onboarding.clientes.cliente.domain.model.Usuario;
import com.onboarding.clientes.cliente.domain.valueobject.CodigoPostal;
import com.onboarding.clientes.cliente.domain.valueobject.Correo;
import com.onboarding.clientes.cliente.domain.valueobject.Curp;
import com.onboarding.clientes.cliente.domain.valueobject.NumeroCuenta;
import com.onboarding.clientes.cliente.domain.valueobject.Rfc;
import com.onboarding.clientes.cliente.domain.valueobject.Telefono;
import com.onboarding.clientes.shared.dinero.Dinero;
import com.onboarding.clientes.shared.error.CodigoError;
import com.onboarding.clientes.shared.error.ErrorNegocio;
import com.onboarding.clientes.shared.evento.PublicadorEvento;
import com.onboarding.clientes.shared.tiempo.Reloj;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioActualizacionClienteTest {

    @Mock
    private ClienteRepositorio clientes;
    @Mock
    private PublicadorEvento publicadorEvento;
    @Mock
    private Reloj reloj;

    private ServicioActualizacionCliente servicio;
    private final Instant momento = Instant.parse("2026-01-01T12:00:00Z");
    private final LocalDate hoy = LocalDate.of(2026, 1, 1);
    private Cliente clienteEjemplo;

    @BeforeEach
    void setUp() {
        servicio = new ServicioActualizacionCliente(clientes, new ClienteMapeador(), publicadorEvento, reloj);

        UUID id = UUID.randomUUID();
        LocalDate nacimiento = LocalDate.of(1995, 5, 15);
        Curp curp = Curp.de("MAGJ950515HDFRRN01");
        Rfc rfc = Rfc.de("MAGJ950515ABC");
        Correo correo = Correo.de("juan.manzano@ejemplo.com");
        Usuario usuario = Usuario.crear(id, correo, "$2a$12$hash", null, momento);
        Domicilio domicilio = new Domicilio("Av. Principal", "100", null, "Centro", "Dolores Hidalgo", "Guanajuato", CodigoPostal.de("37800"), "México");

        AltaCliente alta = new AltaCliente(
                new DatosPersonales("Juan", "Francisco", "Manzano", "Garcia", nacimiento, curp, rfc, Sexo.HOMBRE, "Mexicana", EstadoCivil.SOLTERO),
                new DatosContacto(correo, Telefono.de("4181234567"), null),
                new DatosLaborales("Desarrollador", "Tech Co", Dinero.de(new BigDecimal("30000.00"), "MXN")),
                domicilio,
                usuario,
                Dinero.de(new BigDecimal("1000.00"), "MXN"),
                TipoCuenta.AHORRO,
                momento,
                hoy);

        clienteEjemplo = Cliente.registrar(id, alta);
        clienteEjemplo.asignarNumeroCuenta(NumeroCuenta.de("0000000018"));
    }

    @Test
    @DisplayName("Debe actualizar datos de cliente exitosamente")
    void actualizarExitosamente() {
        when(clientes.buscarPorId(clienteEjemplo.id())).thenReturn(Optional.of(clienteEjemplo));
        when(reloj.ahora()).thenReturn(momento);
        when(reloj.hoy(any(ZoneId.class))).thenReturn(hoy);
        when(clientes.guardar(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        ComandoActualizarCliente comando = new ComandoActualizarCliente(
                clienteEjemplo.id(),
                new ComandoActualizarCliente.Persona(
                        "Juan", "Francisco", "Manzano", "Garcia",
                        LocalDate.of(1995, 5, 15),
                        "MAGJ950515HDFRRN01",
                        "MAGJ950515ABC",
                        Sexo.HOMBRE, "Mexicana", EstadoCivil.CASADO),
                new ComandoActualizarCliente.Contacto("juan.nuevo@ejemplo.com", "4189998877", null),
                new ComandoActualizarCliente.Ubicacion("Nueva Calle", "200", "A", "Nueva Col", "Dolores", "Guanajuato", "37800", "México"),
                new ComandoActualizarCliente.Laboral("Lead Architect", "New Tech", new BigDecimal("55000.00"), "MXN")
        );

        when(clientes.existePorCorreo(Correo.de("juan.nuevo@ejemplo.com"))).thenReturn(false);

        ClienteDetalle actualizado = servicio.actualizar(clienteEjemplo.id(), comando);

        assertThat(actualizado.correo().valor()).isEqualTo("juan.nuevo@ejemplo.com");
        assertThat(actualizado.estadoCivil()).isEqualTo(EstadoCivil.CASADO);
        assertThat(actualizado.ocupacion()).isEqualTo("Lead Architect");
        verify(publicadorEvento).publicar(any());
    }

    @Test
    @DisplayName("Debe fallar al intentar modificar CURP inmutable")
    void fallarAlModificarCurp() {
        when(clientes.buscarPorId(clienteEjemplo.id())).thenReturn(Optional.of(clienteEjemplo));
        when(reloj.ahora()).thenReturn(momento);
        when(reloj.hoy(any(ZoneId.class))).thenReturn(hoy);

        ComandoActualizarCliente comando = new ComandoActualizarCliente(
                clienteEjemplo.id(),
                new ComandoActualizarCliente.Persona(
                        "Juan", "Francisco", "Manzano", "Garcia",
                        LocalDate.of(1995, 5, 15),
                        "GARM950515HDFRRN09", // CURP distinta
                        "MAGJ950515ABC",
                        Sexo.HOMBRE, "Mexicana", EstadoCivil.SOLTERO),
                new ComandoActualizarCliente.Contacto("juan.manzano@ejemplo.com", "4181234567", null),
                new ComandoActualizarCliente.Ubicacion("Calle", "1", null, "Col", "Mpio", "Gto", "37800", "México"),
                new ComandoActualizarCliente.Laboral("Dev", "Co", new BigDecimal("30000.00"), "MXN")
        );

        assertThatThrownBy(() -> servicio.actualizar(clienteEjemplo.id(), comando))
                .isInstanceOf(ErrorNegocio.class)
                .satisfies(e -> assertThat(((ErrorNegocio) e).codigoError()).isEqualTo(CodigoError.CURP_INVALIDA));
    }

    @Test
    @DisplayName("Debe fallar al intentar usar un correo ya ocupado por otro cliente")
    void fallarPorCorreoDuplicado() {
        when(clientes.buscarPorId(clienteEjemplo.id())).thenReturn(Optional.of(clienteEjemplo));
        when(clientes.existePorCorreo(Correo.de("ocupado@ejemplo.com"))).thenReturn(true);

        ComandoActualizarCliente comando = new ComandoActualizarCliente(
                clienteEjemplo.id(),
                new ComandoActualizarCliente.Persona(
                        "Juan", "Francisco", "Manzano", "Garcia",
                        LocalDate.of(1995, 5, 15),
                        "MAGJ950515HDFRRN01",
                        "MAGJ950515ABC",
                        Sexo.HOMBRE, "Mexicana", EstadoCivil.SOLTERO),
                new ComandoActualizarCliente.Contacto("ocupado@ejemplo.com", "4181234567", null),
                new ComandoActualizarCliente.Ubicacion("Calle", "1", null, "Col", "Mpio", "Gto", "37800", "México"),
                new ComandoActualizarCliente.Laboral("Dev", "Co", new BigDecimal("30000.00"), "MXN")
        );

        assertThatThrownBy(() -> servicio.actualizar(clienteEjemplo.id(), comando))
                .isInstanceOf(CorreoDuplicadoException.class);
    }

    @Test
    @DisplayName("Debe fallar al actualizar cliente inexistente")
    void fallarClienteInexistente() {
        UUID id = UUID.randomUUID();
        when(clientes.buscarPorId(id)).thenReturn(Optional.empty());

        ComandoActualizarCliente comando = new ComandoActualizarCliente(
                id,
                new ComandoActualizarCliente.Persona(
                        "Juan", "Francisco", "Manzano", "Garcia",
                        LocalDate.of(1995, 5, 15),
                        "MAGJ950515HDFRRN01",
                        "MAGJ950515ABC",
                        Sexo.HOMBRE, "Mexicana", EstadoCivil.SOLTERO),
                new ComandoActualizarCliente.Contacto("juan.manzano@ejemplo.com", "4181234567", null),
                new ComandoActualizarCliente.Ubicacion("Calle", "1", null, "Col", "Mpio", "Gto", "37800", "México"),
                new ComandoActualizarCliente.Laboral("Dev", "Co", new BigDecimal("30000.00"), "MXN")
        );

        assertThatThrownBy(() -> servicio.actualizar(id, comando))
                .isInstanceOf(ClienteNoEncontradoException.class);
    }
}
