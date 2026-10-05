package com.onboarding.clientes.shared.id;

import java.security.SecureRandom;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Generador de UUID version 7 (RFC 9562).
 *
 * <p>Se usa UUID v7 y no UUID v4 porque el identificador es la clave primaria y la clave de ordenacion
 * natural de la base: con el prefijo temporal (48 bits de milisegundos) los inserts son secuenciales en el
 * indice B-tree (menos fragmentacion de paginas y menos escrituras aleatorias en WAL), y a la vez sigue
 * siendo opaco e imposible de adivinar por el cliente, a diferencia de un autoincremental.
 *
 * <p>Java 21 todavia no expone {@code UUID.randomUUID()} con version 7, por lo que se implementa aqui
 * siguiendo la RFC 9562:
 *
 * <pre>
 *   0                   1                   2                   3
 *   0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                           unix_ts_ms                          |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |          unix_ts_ms           |  ver  |       rand_a          |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |var|                        rand_b                             |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 *
 * <p>Los 12 bits de {@code rand_a} se usan como contador monotono cuando varios identificadores se
 * generan dentro del mismo milisegundo, lo que garantiza ordenamiento estricto incluso bajo concurrencia
 * intensa (el metodo es seguro para uso concurrente).
 */
public final class UuidV7 {

    private static final SecureRandom ALEATORIO = new SecureRandom();

    /** semilla aleatoria del contador, para que dos proceso no generen la misma secuencia. */
    private static final AtomicLong CONTADOR = new AtomicLong(ALEATORIO.nextLong() & 0x0FFFL);

    private static final long MAX_CONTADOR = 0x0FFFL;
    private static final long VARIANTE_RFC4122 = 0x8000L;
    private static final long VERSION_7 = 0x7000L;

    private UuidV7() {}

    /**
     * Genera un identificador UUID v7 monotono.
     *
     * @return identificador unico ordenado por tiempo de creacion
     */
    public static UUID generar() {
        long marcasTiempo = System.currentTimeMillis();
        long contador = siguienteContador();

        long menosSignificativos = (ALEATORIO.nextLong() & 0x3FFFFFFFFFFFFFFFL) | VARIANTE_RFC4122;

        long masSignificativos = (marcasTiempo << 16) | (contador & MAX_CONTADOR) | VERSION_7;

        return new UUID(masSignificativos, menosSignificativos);
    }

    /**
     * Extrae el instante de creacion codificado en el identificador.
     *
     * @param uuid identificador generado por {@link #generar()}
     * @return instante en milisegundos desde la epoca
     */
    public static long instanteDe(UUID uuid) {
        return uuid.getMostSignificantBits() >>> 16;
    }

    private static long siguienteContador() {
        long actual;
        long siguiente;
        do {
            actual = CONTADOR.get();
            siguiente = (actual + 1) & MAX_CONTADOR;
        } while (!CONTADOR.compareAndSet(actual, siguiente));
        return siguiente;
    }
}
