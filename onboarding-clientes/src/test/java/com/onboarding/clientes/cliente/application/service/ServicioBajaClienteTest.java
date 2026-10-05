package com.onboarding.clientes.cliente.application.service;

import com.onboarding.clientes.cliente.application.port.ClienteRepositorio;
import com.onboarding.clientes.cliente.domain.exception.ClienteNoEncontradoException;
import com.onboarding.clientes.cliente.domain.model.AltaCliente;
import com.onboarding.clientes.cliente.domain.model.Cliente;
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
import com.onboarding.clientes.shared.dinero.Dinero;
import com.onboarding.clientes.shared.evento.PublicadorEvento;
import com.onboarding.clientes.shared.tiempo.Reloj;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
class ServicioBajaClienteTest {

    @Mock
    private ClienteRepositorio clientes;
    @Mock
    private PublicadorEvento publicadorEvento;
    @Mock
    private Reloj reloj;

    private ServicioBajaCliente servicio;
    private final Instant momento = Instant.parse("2026-01-01T12:00:00Z");
    private final LocalDate hoy = LocalDate.of(2026, 1, 1);
    private Cliente clienteEjemplo;

    @BeforeEach
    void setUp() {
        servicio = new ServicioBajaCliente(clientes, publicadorEvento, reloj);

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
    @DisplayName("Debe dar de baja lógica al cliente y cancelar su cuenta asociada")
    void darDeBajaExitosamente() {
        when(clientes.buscarPorId(clienteEjemplo.id())).thenReturn(Optional.of(clienteEjemplo));
        when(reloj.ahora()).thenReturn(momento);

        servicio.darDeBaja(clienteEjemplo.id());

        assertThat(clienteEjemplo.activo()).isFalse();
        assertThat(clienteEjemplo.cuenta().orElseThrow().estatus()).isEqualTo(EstatusCuenta.INACTIVA);
        verify(clientes).guardar(clienteEjemplo);
        verify(publicadorEvento).publicar(any());
    }

    @Test
    @DisplayName("Debe fallar al dar de baja a cliente inexistente")
    void fallarClienteInexistente() {
        UUID id = UUID.randomUUID();
        when(clientes.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.darDeBaja(id))
                .isInstanceOf(ClienteNoEncontradoException.class);
    }
}
