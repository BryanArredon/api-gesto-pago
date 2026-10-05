package com.onboarding.clientes.cliente.application.service;

import com.onboarding.clientes.cliente.application.dto.ClienteDetalle;
import com.onboarding.clientes.cliente.application.dto.ClienteResumen;
import com.onboarding.clientes.cliente.application.mapper.ClienteMapeador;
import com.onboarding.clientes.cliente.application.port.ClienteRepositorio;
import com.onboarding.clientes.cliente.application.port.CuentaRepositorio;
import com.onboarding.clientes.cliente.domain.exception.ClienteNoEncontradoException;
import com.onboarding.clientes.cliente.domain.exception.CuentaNoEncontradaException;
import com.onboarding.clientes.cliente.domain.model.AltaCliente;
import com.onboarding.clientes.cliente.domain.model.Cliente;
import com.onboarding.clientes.cliente.domain.model.Cuenta;
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
import com.onboarding.clientes.config.PropiedadesOnboarding;
import com.onboarding.clientes.shared.dinero.Dinero;
import com.onboarding.clientes.shared.web.PaginaRespuesta;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioConsultaClienteTest {

    @Mock
    private ClienteRepositorio clientes;
    @Mock
    private CuentaRepositorio cuentas;

    private ServicioConsultaCliente servicio;
    private final Instant momento = Instant.parse("2026-01-01T12:00:00Z");
    private final LocalDate hoy = LocalDate.of(2026, 1, 1);
    private Cliente clienteEjemplo;

    @BeforeEach
    void setUp() {
        PropiedadesOnboarding props = new PropiedadesOnboarding(
                new PropiedadesOnboarding.Cuenta(BigDecimal.ZERO, "MXN", "AHORRO", true),
                new PropiedadesOnboarding.Seguridad(4, 6, 5, 15, 8),
                new PropiedadesOnboarding.Paginacion(20, 100),
                new PropiedadesOnboarding.Token("onboarding-clientes", Duration.ofMinutes(15), Duration.ofDays(7), "", "", "", ""),
                new PropiedadesOnboarding.Keycloak(false, "", "", "", "", null)
        );

        servicio = new ServicioConsultaCliente(clientes, cuentas, new ClienteMapeador(), props);

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
    @DisplayName("Debe buscar cliente por ID exitosamente")
    void buscarPorId() {
        when(clientes.buscarPorId(clienteEjemplo.id())).thenReturn(Optional.of(clienteEjemplo));

        ClienteDetalle detalle = servicio.buscarPorId(clienteEjemplo.id());
        assertThat(detalle.id()).isEqualTo(clienteEjemplo.id());
        assertThat(detalle.nombreCompleto()).isEqualTo("Juan Francisco Manzano Garcia");
    }

    @Test
    @DisplayName("Debe fallar al buscar cliente inexistente por ID")
    void buscarPorIdInexistente() {
        UUID id = UUID.randomUUID();
        when(clientes.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.buscarPorId(id))
                .isInstanceOf(ClienteNoEncontradoException.class);
    }

    @Test
    @DisplayName("Debe buscar cliente por CURP")
    void buscarPorCurp() {
        when(clientes.buscarPorCurp(any(Curp.class))).thenReturn(Optional.of(clienteEjemplo));

        ClienteDetalle detalle = servicio.buscarPorCurp("MAGJ950515HDFRRN01");
        assertThat(detalle.curp()).isEqualTo(clienteEjemplo.curp());
    }

    @Test
    @DisplayName("Debe buscar cliente por RFC")
    void buscarPorRfc() {
        when(clientes.buscarPorRfc(any(Rfc.class))).thenReturn(Optional.of(clienteEjemplo));

        ClienteDetalle detalle = servicio.buscarPorRfc("MAGJ950515ABC");
        assertThat(detalle.rfc()).isEqualTo(clienteEjemplo.rfc());
    }

    @Test
    @DisplayName("Debe buscar cliente por Correo Electrónico")
    void buscarPorCorreo() {
        when(clientes.buscarPorCorreo(any(Correo.class))).thenReturn(Optional.of(clienteEjemplo));

        ClienteDetalle detalle = servicio.buscarPorCorreo("juan.manzano@ejemplo.com");
        assertThat(detalle.correo()).isEqualTo(clienteEjemplo.correo());
    }

    @Test
    @DisplayName("Debe buscar cliente por Número de Cuenta Bancaria")
    void buscarPorNumeroCuenta() {
        Cuenta cuenta = clienteEjemplo.cuenta().orElseThrow();
        when(cuentas.buscarPorNumero(any(NumeroCuenta.class))).thenReturn(Optional.of(cuenta));
        when(clientes.buscarPorId(clienteEjemplo.id())).thenReturn(Optional.of(clienteEjemplo));

        ClienteDetalle detalle = servicio.buscarPorNumeroCuenta("0000000018");
        assertThat(detalle.id()).isEqualTo(clienteEjemplo.id());
    }

    @Test
    @DisplayName("Debe listar clientes paginados y filtrar por activos")
    void listarClientes() {
        when(clientes.listar(any(), anyInt(), anyBoolean())).thenReturn(List.of(clienteEjemplo));

        PaginaRespuesta<ClienteResumen> pagina = servicio.listar(null, 10, true);
        assertThat(pagina.elementos()).hasSize(1);
        assertThat(pagina.elementos().get(0).nombreCompleto()).isEqualTo("Juan Francisco Manzano Garcia");
    }

    @Test
    @DisplayName("Debe listar clientes registrados entre rango de fechas")
    void listarRegistradosEntreFechas() {
        when(clientes.listarRegistradosEntre(any(), any(), any(), anyInt())).thenReturn(List.of(clienteEjemplo));

        PaginaRespuesta<ClienteResumen> pagina = servicio.listarRegistradosEntre(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), null, 10);
        assertThat(pagina.elementos()).hasSize(1);
    }
}
