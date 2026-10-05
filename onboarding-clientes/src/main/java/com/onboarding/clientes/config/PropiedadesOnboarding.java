package com.onboarding.clientes.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Parametros de negocio externalizables.
 *
 * <p>Todo lo que una regulacion o el negocio pueda cambiar sin desplegar (saldo minimo de apertura, politica
 * de PIN, vigencia de los tokens) vive aqui y no en constantes dentro del codigo.
 *
 * @param cuenta parametros de la cuenta bancaria
 * @param seguridad parametros de credenciales
 * @param paginacion limites de las listas paginadas
 * @param token parametros de emision de tokens propios
 * @param keycloak parametros del proveedor de identidad
 */
@ConfigurationProperties(prefix = "onboarding")
public record PropiedadesOnboarding(
        @DefaultValue Cuenta cuenta,
        @DefaultValue Seguridad seguridad,
        @DefaultValue Paginacion paginacion,
        @DefaultValue Token token,
        @DefaultValue Keycloak keycloak) {

    /**
     * @param saldoInicial saldo con el que se abre la cuenta cuando la peticion no especifica otro
     * @param moneda moneda de la cuenta
     * @param tipo tipo de cuenta por defecto
     * @param permitirSaldoEnPeticion si {@code false} (valor por defecto) el saldo siempre lo define el
     *     sistema, tal como exige el proceso de alta
     */
    public record Cuenta(
            @DefaultValue("0.00") BigDecimal saldoInicial,
            @DefaultValue("MXN") String moneda,
            @DefaultValue("AHORRO") String tipo,
            @DefaultValue("false") boolean permitirSaldoEnPeticion) {}

    /**
     * @param longitudMinimaPin longitud minima del PIN de acceso
     * @param longitudMaximaPin longitud maxima del PIN de acceso
     * @param intentosMaximos intentos fallidos antes de bloquear la cuenta
     * @param minutosBloqueo duracion del bloqueo por intentos fallidos
     * @param longitudMinimaContrasenia longitud minima de la contrasena
     */
    public record Seguridad(
            @DefaultValue("4") int longitudMinimaPin,
            @DefaultValue("6") int longitudMaximaPin,
            @DefaultValue("5") int intentosMaximos,
            @DefaultValue("15") long minutosBloqueo,
            @DefaultValue("8") int longitudMinimaContrasenia) {}

    /**
     * @param limitePorDefecto numero de elementos devueltos si el cliente no especifica
     * @param limiteMaximo tope duro para evitar respuestas gigantes
     */
    public record Paginacion(@DefaultValue("20") int limitePorDefecto, @DefaultValue("100") int limiteMaximo) {}

    /**
     * @param emisor valor de laClaim {@code iss} de los tokens emitidos por esta API
     * @param duracionAcceso vigencia del token de acceso
     * @param duracionRefresh vigencia del token de renovacion
     * @param rutaClavePrivada ruta del keystore PKCS#12 con la clave RSA de firma
     * @param alias alias de la clave dentro del keystore
     * @param clavePrivadaInline clave PEM en linea, usada solo en desarrollo y pruebas
     * @param aliasPublica alias de la clave publica distribution
     */
    public record Token(
            @DefaultValue("onboarding-clientes") String emisor,
            @DefaultValue("PT15M") java.time.Duration duracionAcceso,
            @DefaultValue("P7D") java.time.Duration duracionRefresh,
            @DefaultValue("config/keys/onboarding.p12") String rutaClavePrivada,
            @DefaultValue("onboarding") String alias,
            @DefaultValue("") String clavePrivadaInline,
            @DefaultValue("onboarding") String aliasPublica) {}

    /**
     * @param habilitado si {@code false} la API no acepta tokens de Keycloak (util en pruebas aisladas)
     * @param emisor emisor esperado en los tokens de Keycloak
     * @param uriJwks endpoint de claves publicas de Keycloak
     * @param audiencia cliente para el que se valida el token
     * @param mapeoPerfiles claim de realm roles que se convierte en authorities
     * @param administrador cliente del servicio de Keycloak usado para aprovisionar usuarios
     */
    public record Keycloak(
            @DefaultValue("true") boolean habilitado,
            @DefaultValue("http://localhost:8081/realms/onboarding") String emisor,
            @DefaultValue("http://localhost:8081/realms/onboarding/protocol/openid-connect/certs") String uriJwks,
            @DefaultValue("onboarding-api") String audiencia,
            @DefaultValue("realm_access.roles") String mapeoPerfiles,
            @DefaultValue Administrador administrador) {}

    /**
     * @param habilitado si {@code true} la API crea el usuario en Keycloak durante el alta
     * @param urlBase URL de la API de administracion
     * @param realm realm destino
     * @param clienteId cliente de servicio con permiso de creacion de usuarios
     * @param secretoSecreto del cliente de servicio
     * @param contrasenaInicial contrasena temporal del usuario creado en Keycloak
     */
    public record Administrador(
            @DefaultValue("false") boolean habilitado,
            @DefaultValue("http://localhost:8081") String urlBase,
            @DefaultValue("onboarding") String realm,
            @DefaultValue("onboarding-api-admin") String clienteId,
            @DefaultValue("") String secretoSecreto,
            @DefaultValue("CambiarEstePassword1!") String contrasenaInicial) {}
}
