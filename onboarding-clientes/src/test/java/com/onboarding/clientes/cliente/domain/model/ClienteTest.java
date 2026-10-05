package com.onboarding.clientes.cliente.domain.model;

import com.onboarding.clientes.cliente.domain.exception.ClienteMenorDeEdadException;
import com.onboarding.clientes.cliente.domain.valueobject.CodigoPostal;
import com.onboarding.clientes.cliente.domain.valueobject.Correo;
import com.onboarding.clientes.cliente.domain.valueobject.Curp;
import com.onboarding.clientes.cliente.domain.valueobject.NumeroCuenta;
import com.onboarding.clientes.cliente.domain.valueobject.Rfc;
import com.onboarding.clientes.cliente.domain.valueobject.Telefono;
import com.onboarding.clientes.shared.dinero.Dinero;
import com.onboarding.clientes.shared.error.ErrorNegocio;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClienteTest {

    private final Instant momento = Instant.parse("2026-01-01T12:00:00Z");
    private final LocalDate hoy = LocalDate.of(2026, 1, 1);

    @Test
    @DisplayName("Debe registrar un cliente mayor de edad exitosamente con cuenta activa y usuario")
    void debeRegistrarClienteExitosamente() {
        UUID id = UUID.randomUUID();
        LocalDate nacimiento = LocalDate.of(1995, 5, 15);
        Curp curp = Curp.de("MAGJ950515HDFRRN01");
        Rfc rfc = Rfc.de("MAGJ950515ABC");
        Correo correo = Correo.de("juan.manzano@ejemplo.com");

        Usuario usuario = Usuario.crear(id, correo, "$2a$12$abcdefg", null, momento);
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

        Cliente cliente = Cliente.registrar(id, alta);

        assertThat(cliente).isNotNull();
        assertThat(cliente.id()).isEqualTo(id);
        assertThat(cliente.activo()).isTrue();
        assertThat(cliente.nombreCompleto()).isEqualTo("Juan Francisco Manzano Garcia");
        assertThat(cliente.cuentas()).hasSize(1);
        assertThat(cliente.cuentas().get(0).esActiva()).isTrue();
        assertThat(cliente.cuentas().get(0).saldo().monto()).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("Debe fallar al registrar un cliente menor de 18 años")
    void debeFallarClienteMenorDeEdad() {
        UUID id = UUID.randomUUID();
        LocalDate nacimiento = LocalDate.of(2010, 5, 15); // 15 años en 2026
        Curp curp = Curp.de("MAGJ100515HDFRRN01");
        Rfc rfc = Rfc.de("MAGJ100515ABC");
        Correo correo = Correo.de("menor@ejemplo.com");

        Usuario usuario = Usuario.crear(id, correo, "$2a$12$abcdefg", null, momento);
        Domicilio domicilio = new Domicilio("Av. Principal", "100", null, "Centro", "Dolores Hidalgo", "Guanajuato", CodigoPostal.de("37800"), "México");

        AltaCliente alta = new AltaCliente(
                new DatosPersonales("Carlos", null, "Manzano", "Garcia", nacimiento, curp, rfc, Sexo.HOMBRE, "Mexicana", EstadoCivil.SOLTERO),
                new DatosContacto(correo, Telefono.de("4181234567"), null),
                new DatosLaborales("Estudiante", null, Dinero.de(new BigDecimal("1000.00"), "MXN")),
                domicilio,
                usuario,
                Dinero.de(BigDecimal.ZERO, "MXN"),
                TipoCuenta.AHORRO,
                momento,
                hoy);

        assertThatThrownBy(() -> Cliente.registrar(id, alta))
                .isInstanceOf(ClienteMenorDeEdadException.class);
    }

    @Test
    @DisplayName("Debe inactivar cuentas y usuario en cascada al dar de baja lógica al cliente")
    void debeInactivarEnCascadaAlDarDeBaja() {
        UUID id = UUID.randomUUID();
        LocalDate nacimiento = LocalDate.of(1995, 5, 15);
        Curp curp = Curp.de("MAGJ950515HDFRRN01");
        Rfc rfc = Rfc.de("MAGJ950515ABC");
        Correo correo = Correo.de("juan.manzano@ejemplo.com");

        Usuario usuario = Usuario.crear(id, correo, "$2a$12$abcdefg", null, momento);
        Domicilio domicilio = new Domicilio("Av. Principal", "100", null, "Centro", "Dolores Hidalgo", "Guanajuato", CodigoPostal.de("37800"), "México");

        AltaCliente alta = new AltaCliente(
                new DatosPersonales("Juan", null, "Manzano", "Garcia", nacimiento, curp, rfc, Sexo.HOMBRE, "Mexicana", EstadoCivil.SOLTERO),
                new DatosContacto(correo, Telefono.de("4181234567"), null),
                new DatosLaborales("Desarrollador", "Tech Co", Dinero.de(new BigDecimal("30000.00"), "MXN")),
                domicilio,
                usuario,
                Dinero.de(new BigDecimal("500.00"), "MXN"),
                TipoCuenta.AHORRO,
                momento,
                hoy);

        Cliente cliente = Cliente.registrar(id, alta);
        cliente.asignarNumeroCuenta(NumeroCuenta.de("0000000018"));

        cliente.darDeBaja(momento.plusSeconds(3600));

        assertThat(cliente.activo()).isFalse();
        assertThat(cliente.fechaBaja()).isNotNull();
        assertThat(cliente.cuenta().orElseThrow().esActiva()).isFalse();
        assertThat(cliente.usuario().activo()).isFalse();
    }

    @Test
    @DisplayName("Debe rechazar la modificación de CURP o RFC en la actualización")
    void debeRechazarModificacionDeCurpORfc() {
        UUID id = UUID.randomUUID();
        LocalDate nacimiento = LocalDate.of(1995, 5, 15);
        Curp curp = Curp.de("MAGJ950515HDFRRN01");
        Rfc rfc = Rfc.de("MAGJ950515ABC");
        Correo correo = Correo.de("juan.manzano@ejemplo.com");

        Usuario usuario = Usuario.crear(id, correo, "$2a$12$abcdefg", null, momento);
        Domicilio domicilio = new Domicilio("Av. Principal", "100", null, "Centro", "Dolores Hidalgo", "Guanajuato", CodigoPostal.de("37800"), "México");

        AltaCliente alta = new AltaCliente(
                new DatosPersonales("Juan", null, "Manzano", "Garcia", nacimiento, curp, rfc, Sexo.HOMBRE, "Mexicana", EstadoCivil.SOLTERO),
                new DatosContacto(correo, Telefono.de("4181234567"), null),
                new DatosLaborales("Desarrollador", "Tech Co", Dinero.de(new BigDecimal("30000.00"), "MXN")),
                domicilio,
                usuario,
                Dinero.de(new BigDecimal("500.00"), "MXN"),
                TipoCuenta.AHORRO,
                momento,
                hoy);

        Cliente cliente = Cliente.registrar(id, alta);

        ActualizacionCliente intentoCambioCurp = new ActualizacionCliente(
                new DatosPersonales("Juan", null, "Manzano", "Garcia", nacimiento, curp, rfc, Sexo.HOMBRE, "Mexicana", EstadoCivil.SOLTERO),
                new DatosContacto(correo, Telefono.de("4181234567"), null),
                new DatosLaborales("Desarrollador", "Tech Co", Dinero.de(new BigDecimal("30000.00"), "MXN")),
                domicilio,
                Curp.de("OTRA950515HDFRRN02"), // CURP distinta
                rfc,
                momento,
                hoy);

        assertThatThrownBy(() -> cliente.actualizar(intentoCambioCurp))
                .isInstanceOf(ErrorNegocio.class)
                .hasMessageContaining("inmutable");
    }
}
