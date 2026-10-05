package com.onboarding.clientes.cliente.domain.valueobject;

import com.onboarding.clientes.shared.dinero.Dinero;
import com.onboarding.clientes.shared.error.ErrorNegocio;
import com.onboarding.clientes.shared.seguridad.PoliticaCredenciales;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValueObjectsTest {

    @Nested
    @DisplayName("Pruebas para CURP")
    class CurpTests {
        @Test
        @DisplayName("Debe aceptar CURP válida y coincidente con fecha de nacimiento")
        void curpValida() {
            Curp curp = Curp.de("MAGJ950515HDFRRN01");
            assertThat(curp.valor()).isEqualTo("MAGJ950515HDFRRN01");
            curp.exigeCoincidirCon(LocalDate.of(1995, 5, 15));
        }

        @Test
        @DisplayName("Debe rechazar CURP si la longitud es diferente de 18")
        void curpLongitudInvalida() {
            assertThatThrownBy(() -> Curp.de("MAGJ950515HDFRRN0"))
                    .isInstanceOf(ErrorNegocio.class);
        }

        @Test
        @DisplayName("Debe rechazar CURP si no coincide con la fecha de nacimiento")
        void curpFechaNoCoincide() {
            Curp curp = Curp.de("MAGJ950515HDFRRN01");
            assertThatThrownBy(() -> curp.exigeCoincidirCon(LocalDate.of(1990, 1, 1)))
                    .isInstanceOf(ErrorNegocio.class);
        }
    }

    @Nested
    @DisplayName("Pruebas para RFC")
    class RfcTests {
        @Test
        @DisplayName("Debe aceptar RFC válido de persona física (13 caracteres)")
        void rfcValidoPersonaFisica() {
            Rfc rfc = Rfc.de("MAGJ950515ABC");
            assertThat(rfc.esPersonaFisica()).isTrue();
            rfc.exigeCoincidirCon(LocalDate.of(1995, 5, 15));
        }

        @Test
        @DisplayName("Debe rechazar RFC con formato inválido")
        void rfcInvalido() {
            assertThatThrownBy(() -> Rfc.de("RFC_INVALIDO_123"))
                    .isInstanceOf(ErrorNegocio.class);
        }
    }

    @Nested
    @DisplayName("Pruebas para Número de Cuenta y Algoritmo Luhn")
    class NumeroCuentaTests {
        @Test
        @DisplayName("Debe aceptar número de cuenta de 10 dígitos con dígito verificador Luhn correcto")
        void numeroCuentaValidoLuhn() {
            NumeroCuenta cuenta = NumeroCuenta.de("0000000018");
            assertThat(cuenta.valor()).isEqualTo("0000000018");
        }

        @Test
        @DisplayName("Debe rechazar número de cuenta con dígito verificador Luhn incorrecto")
        void numeroCuentaLuhnInvalido() {
            assertThatThrownBy(() -> NumeroCuenta.de("0000000017"))
                    .isInstanceOf(ErrorNegocio.class)
                    .hasMessageContaining("digito verificador incorrecto");
        }

        @Test
        @DisplayName("Debe rechazar número de cuenta con longitud diferente de 10")
        void numeroCuentaLongitudInvalida() {
            assertThatThrownBy(() -> NumeroCuenta.de("12345"))
                    .isInstanceOf(ErrorNegocio.class);
        }
    }

    @Nested
    @DisplayName("Pruebas para Código Postal y Teléfono")
    class ContactoTests {
        @Test
        @DisplayName("Debe validar código postal de 5 dígitos")
        void codigoPostal() {
            CodigoPostal cp = CodigoPostal.de("37800");
            assertThat(cp.valor()).isEqualTo("37800");

            assertThatThrownBy(() -> CodigoPostal.de("3780"))
                    .isInstanceOf(ErrorNegocio.class);
        }

        @Test
        @DisplayName("Debe validar teléfono de 10 dígitos")
        void telefono() {
            Telefono tel = Telefono.de("4181234567");
            assertThat(tel.valor()).isEqualTo("4181234567");

            assertThatThrownBy(() -> Telefono.de("12345"))
                    .isInstanceOf(ErrorNegocio.class);
        }
    }

    @Nested
    @DisplayName("Pruebas para Dinero")
    class DineroTests {
        @Test
        @DisplayName("Debe operar correctamente y validar montos no negativos")
        void dineroOperaciones() {
            Dinero saldo = Dinero.de(new BigDecimal("100.50"), "MXN");
            Dinero abono = Dinero.de(new BigDecimal("50.25"), "MXN");
            Dinero resultado = saldo.sumar(abono);

            assertThat(resultado.monto()).isEqualByComparingTo("150.75");
            assertThat(resultado.esPositivo()).isTrue();

            assertThatThrownBy(() -> saldo.restar(Dinero.de(new BigDecimal("200.00"), "MXN")))
                    .isInstanceOf(ErrorNegocio.class)
                    .hasMessageContaining("negativo");
        }
    }

    @Nested
    @DisplayName("Pruebas para Política de Complejidad de Contraseña")
    class PoliticaContraseniaTests {
        @Test
        @DisplayName("Debe aceptar contraseñas que cumplan con todos los requisitos de seguridad")
        void contraseniaValida() {
            PoliticaCredenciales.validarContrasenia("Segura123!", 8);
        }

        @Test
        @DisplayName("Debe rechazar contraseñas sin mayúsculas, minúsculas, números o caracteres especiales")
        void contraseniaInsegura() {
            assertThatThrownBy(() -> PoliticaCredenciales.validarContrasenia("todominusculas1!", 8))
                    .isInstanceOf(ErrorNegocio.class);

            assertThatThrownBy(() -> PoliticaCredenciales.validarContrasenia("TODOMAYUSCULAS1!", 8))
                    .isInstanceOf(ErrorNegocio.class);

            assertThatThrownBy(() -> PoliticaCredenciales.validarContrasenia("SinNumerosNiEspecial", 8))
                    .isInstanceOf(ErrorNegocio.class);

            assertThatThrownBy(() -> PoliticaCredenciales.validarContrasenia("Corta1!", 8))
                    .isInstanceOf(ErrorNegocio.class);
        }
    }
}
