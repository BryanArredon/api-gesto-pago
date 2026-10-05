package com.onboarding.clientes.shared.dinero;

import com.onboarding.clientes.shared.error.CodigoError;
import com.onboarding.clientes.shared.error.ErrorNegocio;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Importe monetario inmutable: monto exacto + moneda ISO 4217.
 *
 * <p>Decisiones deliberadas:
 *
 * <ul>
 *   <li>{@link BigDecimal} y nunca {@code double}: los importes son exactos por definicion y el redondeo
 *       flotante produce centavos que no cuadran.
 *   <li>Escala fija de 2 decimales para MXN, normalizada en la construccion.
 *   <li>Precision maxima 18 digitos: alineada con {@code NUMERIC(18,2)} en PostgreSQL, de modo que el
 *       dominio nunca puede producir un valor que la base rechace.
 *   <li>Es inmutable: la aritmetica devuelve instancias nuevas, lo que evita aliasing en el agregado.
 * </ul>
 */
public record Dinero(BigDecimal monto, String moneda) implements Comparable<Dinero> {

    private static final Pattern CODIGO_MONEDA = Pattern.compile("^[A-Z]{3}$");
    private static final int PRECISION_ESCALA = 18;
    private static final int ESCALA = 2;

    public Dinero {
        Objects.requireNonNull(monto, "El monto no puede ser nulo");
        Objects.requireNonNull(moneda, "La moneda no puede ser nula");
        if (!CODIGO_MONEDA.matcher(moneda).matches()) {
            throw new ErrorNegocio(CodigoError.DATO_INVALIDO, "Moneda invalida: debe ser un codigo ISO 4217 de 3 letras");
        }
        if (monto.precision() - monto.scale() > PRECISION_ESCALA - ESCALA) {
            throw new ErrorNegocio(CodigoError.DATO_INVALIDO, "El monto excede la precision maxima soportada (16 digitos)");
        }
        if (monto.signum() < 0) {
            throw new ErrorNegocio(CodigoError.SALDO_INVALIDO, "El monto no puede ser negativo");
        }
        monto = monto.setScale(ESCALA, RoundingMode.HALF_UP);
    }

    public static Dinero cero(String moneda) {
        return new Dinero(BigDecimal.ZERO, moneda);
    }

    public static Dinero de(BigDecimal monto, String moneda) {
        return new Dinero(monto, moneda);
    }

    public boolean esPositivo() {
        return monto.signum() > 0;
    }

    public boolean esCero() {
        return monto.signum() == 0;
    }

    public Dinero sumar(Dinero otro) {
        exigeMismaMoneda(otro);
        return new Dinero(monto.add(otro.monto), moneda);
    }

    public Dinero restar(Dinero otro) {
        exigeMismaMoneda(otro);
        BigDecimal resultado = monto.subtract(otro.monto);
        if (resultado.signum() < 0) {
            throw new ErrorNegocio(CodigoError.SALDO_INVALIDO, "La operacion dejaria un saldo negativo");
        }
        return new Dinero(resultado, moneda);
    }

    public boolean esMayorQue(Dinero otro) {
        exigeMismaMoneda(otro);
        return monto.compareTo(otro.monto) > 0;
    }

    @Override
    public int compareTo(Dinero otro) {
        exigeMismaMoneda(otro);
        return monto.compareTo(otro.monto);
    }

    @Override
    public String toString() {
        return moneda + " " + monto.toPlainString();
    }

    private void exigeMismaMoneda(Dinero otro) {
        if (!moneda.equals(otro.moneda)) {
            throw new ErrorNegocio(
                    CodigoError.DATO_INVALIDO, "No se pueden operar importes en monedas distintas: " + moneda + " y " + otro.moneda);
        }
    }
}
