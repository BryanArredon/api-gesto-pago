package com.onboarding.clientes.cliente.application.service;

import com.onboarding.clientes.cliente.application.command.ComandoRegistrarCliente;
import com.onboarding.clientes.cliente.application.dto.ClienteDetalle;
import com.onboarding.clientes.cliente.application.mapper.ClienteMapeador;
import com.onboarding.clientes.cliente.application.port.ClienteRepositorio;
import com.onboarding.clientes.cliente.domain.exception.CorreoDuplicadoException;
import com.onboarding.clientes.cliente.domain.exception.CurpDuplicadaException;
import com.onboarding.clientes.cliente.domain.exception.RfcDuplicadoException;
import com.onboarding.clientes.cliente.domain.model.AltaCliente;
import com.onboarding.clientes.cliente.domain.model.Cliente;
import com.onboarding.clientes.cliente.domain.model.DatosContacto;
import com.onboarding.clientes.cliente.domain.model.DatosLaborales;
import com.onboarding.clientes.cliente.domain.model.DatosPersonales;
import com.onboarding.clientes.cliente.domain.model.Domicilio;
import com.onboarding.clientes.cliente.domain.model.TipoCuenta;
import com.onboarding.clientes.cliente.domain.model.Usuario;
import com.onboarding.clientes.cliente.domain.valueobject.CodigoPostal;
import com.onboarding.clientes.cliente.domain.valueobject.Correo;
import com.onboarding.clientes.cliente.domain.valueobject.Curp;
import com.onboarding.clientes.cliente.domain.valueobject.Rfc;
import com.onboarding.clientes.cliente.domain.valueobject.Telefono;
import com.onboarding.clientes.config.PropiedadesOnboarding;
import com.onboarding.clientes.shared.dinero.Dinero;
import com.onboarding.clientes.shared.error.CodigoError;
import com.onboarding.clientes.shared.error.ErrorNegocio;
import com.onboarding.clientes.shared.evento.EventoDominio;
import com.onboarding.clientes.shared.evento.PublicadorEvento;
import com.onboarding.clientes.shared.id.UuidV7;
import com.onboarding.clientes.shared.seguridad.GeneradorHash;
import com.onboarding.clientes.shared.seguridad.PoliticaCredenciales;
import com.onboarding.clientes.shared.tiempo.Reloj;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: alta de cliente persona fisica.
 *
 * <p>Es el unico camino para crear un cliente y, en una sola transaccion, deja creado lo que el proceso exige:
 * cliente, domicilio, usuario de acceso con su contrasena cifrada y cuenta bancaria con saldo inicial y estatus
 * ACTIVA. O se crea todo o no se crea nada: un cliente sin cuenta, o una cuenta sin cliente, es un estado
 * invalido que el negocio no tolera.
 *
 * <p>Orden de validaciones (de mas barata a mas cara):
 *
 * <ol>
 *   <li>Formato de los campos y reglas de la base (validadas en el borde HTTP).
 *   <li>Duplicados: consultas indexadas y baratas sobre las claves unicas.
 *   <li>Invariantes del dominio (edad, consistencia CURP/RFC con la fecha, ingreso positivo).
 *   <li>Cifrado de la contrasena: la operacion mas cara (~250 ms), al final y solo si todo lo demas es valido.
 * </ol>
 */
@Service
public class ServicioRegistroCliente {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServicioRegistroCliente.class);
    private static final String ZONA_OPERATIVA = "America/Mexico_City";

    private final ClienteRepositorio clientes;
    private final GeneradorHash generadorHash;
    private final PublicadorEvento publicadorEvento;
    private final Reloj reloj;
    private final PropiedadesOnboarding propiedades;
    private final ClienteMapeador mapeador;

    public ServicioRegistroCliente(
            ClienteRepositorio clientes,
            GeneradorHash generadorHash,
            PublicadorEvento publicadorEvento,
            Reloj reloj,
            PropiedadesOnboarding propiedades,
            ClienteMapeador mapeador) {
        this.clientes = clientes;
        this.generadorHash = generadorHash;
        this.publicadorEvento = publicadorEvento;
        this.reloj = reloj;
        this.propiedades = propiedades;
        this.mapeador = mapeador;
    }

    /**
     * Registra un cliente y todo lo que el proceso exige a la vez.
     *
     * @param comando datos del alta
     * @return detalle del cliente creado, con su cuenta y su usuario
     * @throws CurpDuplicadaException, RfcDuplicadoException, CorreoDuplicadoException si ya existe
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ClienteDetalle registrar(ComandoRegistrarCliente comando) {
        Curp curp = Curp.de(comando.persona().curp());
        Rfc rfc = Rfc.de(comando.persona().rfc());
        Correo correo = Correo.de(comando.contacto().correo());

        exigeUnicidad(curp, rfc, correo);

        PropiedadesOnboarding.Seguridad seguridad = propiedades.seguridad();
        PoliticaCredenciales.validarContrasenia(comando.credenciales().contrasenia(), seguridad.longitudMinimaContrasenia());
        String hashPin = hasheaPinSiPresente(comando);

        Instant momento = reloj.ahora();
        // El identificador se genera aqui para poder vincular usuario y cliente en la misma unidad de trabajo.
        UUID identificador = UuidV7.generar();
        Usuario usuario = Usuario.crear(
                identificador, correo, generadorHash.hash(comando.credenciales().contrasenia()), hashPin, momento);

        Cliente cliente = Cliente.registrar(identificador, new AltaCliente(
                new DatosPersonales(
                        comando.persona().nombre(),
                        comando.persona().segundoNombre(),
                        comando.persona().apellidoPaterno(),
                        comando.persona().apellidoMaterno(),
                        comando.persona().fechaNacimiento(),
                        curp,
                        rfc,
                        comando.persona().sexo(),
                        comando.persona().nacionalidad(),
                        comando.persona().estadoCivil()),
                new DatosContacto(correo, Telefono.de(comando.contacto().telefonoMovil()), telefonoAlterno(comando)),
                new DatosLaborales(
                        comando.laboral().ocupacion(),
                        comando.laboral().empresa(),
                        Dinero.de(comando.laboral().ingresoMensual(), comando.laboral().monedaIngreso())),
                domicilio(comando),
                usuario,
                saldoInicial(comando.cuenta()),
                tipoCuenta(comando.cuenta()),
                momento,
                reloj.hoy(ZoneId.of(ZONA_OPERATIVA))));

        Cliente persistido = clientes.guardar(cliente);

        Map<String, Object> carga = new LinkedHashMap<>();
        carga.put("curp", curp.valor());
        carga.put("numeroCuenta", persistido.cuenta()
                .filter(cuenta -> cuenta.tieneNumero())
                .map(cuenta -> cuenta.numeroCuenta().valor())
                .orElse(null));
        carga.put("saldoInicial", comando.cuenta().saldoInicial());
        publicadorEvento.publicar(EventoDominio.crear(
                "cliente.registrado", EventoDominio.TIPO_CLIENTE, persistido.id(), carga, "sistema", momento));

        LOGGER.info(
                "Cliente registrado: id={} curp={} numeroCuenta={}",
                persistido.id(),
                curp.valor(),
                carga.get("numeroCuenta"));
        return mapeador.aDetalle(persistido);
    }

    private void exigeUnicidad(Curp curp, Rfc rfc, Correo correo) {
        if (clientes.existePorCurp(curp)) {
            throw new CurpDuplicadaException(curp);
        }
        if (clientes.existePorRfc(rfc)) {
            throw new RfcDuplicadoException(rfc);
        }
        if (clientes.existePorCorreo(correo)) {
            throw new CorreoDuplicadoException(correo);
        }
    }

    private String hasheaPinSiPresente(ComandoRegistrarCliente comando) {
        String pin = comando.credenciales().pin();
        if (pin == null || pin.isBlank()) {
            return null;
        }
        PropiedadesOnboarding.Seguridad seguridad = propiedades.seguridad();
        PoliticaCredenciales.validarPin(pin, seguridad.longitudMinimaPin(), seguridad.longitudMaximaPin());
        PoliticaCredenciales.exigePinDistintoDeContrasena(pin, comando.credenciales().contrasenia());
        return generadorHash.hash(pin);
    }

    private Telefono telefonoAlterno(ComandoRegistrarCliente comando) {
        String alterno = comando.contacto().telefonoAlterno();
        return alterno == null || alterno.isBlank() ? null : Telefono.de(alterno);
    }

    private Domicilio domicilio(ComandoRegistrarCliente comando) {
        ComandoRegistrarCliente.Ubicacion ubicacion = comando.ubicacion();
        return new Domicilio(
                ubicacion.calle(),
                ubicacion.numeroExterior(),
                ubicacion.numeroInterior(),
                ubicacion.colonia(),
                ubicacion.municipio(),
                ubicacion.estado(),
                CodigoPostal.de(ubicacion.codigoPostal()),
                ubicacion.pais());
    }

    /**
     * El saldo inicial lo define el sistema por defecto. Solo se acepta el valor de la peticion si la
     * operacion viene de un operador autorizado y la configuracion lo habilita de forma explicita.
     */
    private Dinero saldoInicial(ComandoRegistrarCliente.AperturaCuenta cuenta) {
        PropiedadesOnboarding.Cuenta config = propiedades.cuenta();
        BigDecimal solicitado = cuenta.saldoInicial();
        if (solicitado == null || !config.permitirSaldoEnPeticion()) {
            return Dinero.de(config.saldoInicial(), cuenta.monedaCuenta() == null ? config.moneda() : cuenta.monedaCuenta());
        }
        if (solicitado.signum() < 0) {
            throw new ErrorNegocio(CodigoError.SALDO_INVALIDO, "El saldo inicial no puede ser negativo: " + solicitado);
        }
        return Dinero.de(solicitado, cuenta.monedaCuenta() == null ? config.moneda() : cuenta.monedaCuenta());
    }

    private TipoCuenta tipoCuenta(ComandoRegistrarCliente.AperturaCuenta cuenta) {
        if (cuenta.tipoCuenta() != null) {
            return cuenta.tipoCuenta();
        }
        return TipoCuenta.valueOf(propiedades.cuenta().tipo());
    }
}
