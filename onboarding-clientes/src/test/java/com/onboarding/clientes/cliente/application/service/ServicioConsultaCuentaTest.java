package com.onboarding.clientes.cliente.application.service;

import com.onboarding.clientes.cliente.application.dto.CuentaDto;
import com.onboarding.clientes.cliente.application.dto.SaldoCuentaDto;
import com.onboarding.clientes.cliente.application.mapper.ClienteMapeador;
import com.onboarding.clientes.cliente.application.port.CuentaRepositorio;
import com.onboarding.clientes.cliente.domain.exception.CuentaNoEncontradaException;
import com.onboarding.clientes.cliente.domain.model.AltaCliente;
import com.onboarding.clientes.cliente.domain.model.Cliente;
import com.onboarding.clientes.cliente.domain.model.Cuenta;
import com.onboarding.clientes.cliente.domain.model.DatosContacto;
import com.onboarding.clientes.cliente.domain.model.DatosLaborales;
import com.onboarding.clientes.cliente.domain.model.DatosPersonales;
import com.onboarding.clientes.cliente.domain.model.Domicilio;
import com.onboarding.clientes.cliente.domain.model.EstadoCivil;
import com.onboarding.clientes.cliente.domain.model.EstatusCuenta;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioConsultaCuentaTest {

    @Mock
    private CuentaRepositorio cuentas;

    private ServicioConsultaCuenta servicio;
    private final Instant momento = Instant.parse("2026-01-01T12:00:00Z");
    private final LocalDate hoy = LocalDate.of(2026, 1, 1);
    private Cuenta cuentaEjemplo;

    @BeforeEach
    void setUp() {
        PropiedadesOnboarding props = new PropiedadesOnboarding(
                new PropiedadesOnboarding.Cuenta(BigDecimal.ZERO, "MXN", "AHORRO", true),
                new PropiedadesOnboarding.Seguridad(4, 6, 5, 15, 8),
                new PropiedadesOnboarding.Paginacion(20, 100),
                new PropiedadesOnboarding.Token("onboarding-clientes", Duration.ofMinutes(15), Duration.ofDays(7), "", "", "", ""),
                new PropiedadesOnboarding.Keycloak(false, "", "", "", "", null)
        );

        servicio = new ServicioConsultaCuenta(cuentas, new ClienteMapeador(), props);

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
                Dinero.de(new BigDecimal("1500.50"), "MXN"),
                TipoCuenta.AHORRO,
                momento,
                hoy);

        Cliente cliente = Cliente.registrar(id, alta);
        cliente.asignarNumeroCuenta(NumeroCuenta.de("0000000018"));
        cuentaEjemplo = cliente.cuenta().orElseThrow();
    }

    @Test
    @DisplayName("Debe buscar cuenta por número de cuenta exitosamente")
    void buscarPorNumero() {
        when(cuentas.buscarPorNumero(any(NumeroCuenta.class))).thenReturn(Optional.of(cuentaEjemplo));

        CuentaDto dto = servicio.buscarPorNumero("0000000018");
        assertThat(dto.numero().valor()).isEqualTo("0000000018");
        assertThat(dto.saldo()).isEqualByComparingTo("1500.50");
        assertThat(dto.estatus()).isEqualTo(EstatusCuenta.ACTIVA);
    }

    @Test
    @DisplayName("Debe fallar al buscar cuenta inexistente por número")
    void buscarPorNumeroInexistente() {
        when(cuentas.buscarPorNumero(any(NumeroCuenta.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.buscarPorNumero("0000000018"))
                .isInstanceOf(CuentaNoEncontradaException.class);
    }

    @Test
    @DisplayName("Debe consultar saldo de cuenta bancaria exitosamente")
    void consultarSaldo() {
        when(cuentas.buscarPorNumero(any(NumeroCuenta.class))).thenReturn(Optional.of(cuentaEjemplo));

        SaldoCuentaDto saldo = servicio.consultarSaldo("0000000018");
        assertThat(saldo.saldo()).isEqualByComparingTo("1500.50");
        assertThat(saldo.moneda()).isEqualTo("MXN");
        assertThat(saldo.estatus()).isEqualTo(EstatusCuenta.ACTIVA);
    }

    @Test
    @DisplayName("Debe listar cuentas activas paginadas")
    void listarActivas() {
        when(cuentas.listarActivas(any(), anyInt())).thenReturn(List.of(cuentaEjemplo));

        PaginaRespuesta<CuentaDto> pagina = servicio.listarActivas(null, 10);
        assertThat(pagina.elementos()).hasSize(1);
        assertThat(pagina.elementos().get(0).numero().valor()).isEqualTo("0000000018");
    }
}
