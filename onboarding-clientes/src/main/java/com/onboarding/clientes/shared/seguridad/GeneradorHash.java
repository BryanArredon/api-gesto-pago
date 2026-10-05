package com.onboarding.clientes.shared.seguridad;

/**
 * Puerto de cifrado de credenciales.
 *
 * <p>Se declara como interfaz para que el dominio y los casos de uso no dependan de BCrypt ni de Spring
 * Security, y para que las pruebas puedan usar una implementacion instantanea.
 *
 * <p>Contrato: la implementacion debe ser unidireccional y con sal por credencial (BCrypt), de modo que el
 * mismo texto plano produzca hashes distintos y que el hash no sea reversible.
 */
public interface GeneradorHash {

    /**
     * @param textoPlano contrasena o PIN en claro
     * @return hash seguro para almacenar
     */
    String hash(String textoPlano);

    /**
     * @param textoPlano texto a verificar
     * @param hash hash almacenado
     * @return {@code true} si coinciden
     */
    boolean coincide(String textoPlano, String hash);

    /**
     * Indica si un hash deberia regenerarse (por ejemplo, si el coste de BCrypt aumento).
     *
     * @param hash hash almacenado
     * @return {@code true} si conviene regenerarlo
     */
    boolean necesitaRehash(String hash);
}
