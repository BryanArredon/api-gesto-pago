package com.proyecto.servicios.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidadorLuhnTest {

    @Test
    @DisplayName("Debe validar correctamente cuentas de 10 dígitos con dígito Luhn válido")
    void validaCuentaExitosa() {
        assertThat(ValidadorLuhn.esNumeroCuentaValido("0000000018")).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0000000019", "0000000017", "12345", "abcdefghij", "", "   "})
    @DisplayName("Debe rechazar cuentas con longitud inválida o checksum Luhn erróneo")
    void rechazaCuentasInvalidas(String cuenta) {
        assertThat(ValidadorLuhn.esNumeroCuentaValido(cuenta)).isFalse();
    }

    @Test
    @DisplayName("Debe calcular correctamente el dígito verificador Luhn")
    void calcularDigitoLuhn() {
        // Base de 9 dígitos de cuenta bancaria "000000001" -> dígito esperado '8'
        assertThat(ValidadorLuhn.calcularDigitoLuhn("000000001")).isEqualTo("8");

        // Base "7" -> dígito esperado '5' (7*2 = 14 -> 5, suma=5 -> (10-5)=5)
        assertThat(ValidadorLuhn.calcularDigitoLuhn("7")).isEqualTo("5");
    }

    @Test
    @DisplayName("Debe generar números de tarjeta de 16 dígitos válidos según Luhn")
    void generarTarjeta() {
        for (int i = 0; i < 20; i++) {
            String tarjeta = ValidadorLuhn.generarTarjeta("453201", 16);
            assertThat(tarjeta).hasSize(16);
            assertThat(tarjeta).startsWith("453201");
            assertThat(ValidadorLuhn.esTarjetaValida(tarjeta)).isTrue();
            assertThat(ValidadorLuhn.esLuhnValido(tarjeta)).isTrue();
            assertThat(ValidadorLuhn.obtenerFranquicia(tarjeta)).isEqualTo(ValidadorLuhn.FranquiciaTarjeta.VISA);
        }
    }

    @Test
    @DisplayName("Debe identificar franquicias correctamente")
    void identificarFranquicias() {
        assertThat(ValidadorLuhn.obtenerFranquicia("4532015112345678")).isEqualTo(ValidadorLuhn.FranquiciaTarjeta.VISA);
        assertThat(ValidadorLuhn.obtenerFranquicia("5200123456789010")).isEqualTo(ValidadorLuhn.FranquiciaTarjeta.MASTERCARD);
        assertThat(ValidadorLuhn.obtenerFranquicia("378282246310005")).isEqualTo(ValidadorLuhn.FranquiciaTarjeta.AMERICAN_EXPRESS);
        assertThat(ValidadorLuhn.obtenerFranquicia("5063011234567890")).isEqualTo(ValidadorLuhn.FranquiciaTarjeta.CARNET);
        assertThat(ValidadorLuhn.obtenerFranquicia("6011123456789012")).isEqualTo(ValidadorLuhn.FranquiciaTarjeta.DISCOVER);
    }

    @Test
    @DisplayName("Debe formatear y enmascarar tarjetas de manera segura")
    void formatearYEnmascarar() {
        String pan = "4532015112345678";
        assertThat(ValidadorLuhn.formatearTarjeta(pan)).isEqualTo("4532 0151 1234 5678");
        assertThat(ValidadorLuhn.enmascararTarjeta(pan)).isEqualTo("**** **** **** 5678");
        assertThat(ValidadorLuhn.enmascararCuenta("0000000018")).isEqualTo("******0018");
    }

    @Test
    @DisplayName("Debe lanzar excepción si el BIN es mayor a la longitud solicitada")
    void errorBinMuyLargo() {
        assertThatThrownBy(() -> ValidadorLuhn.generarTarjeta("4532015112345678", 16))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

