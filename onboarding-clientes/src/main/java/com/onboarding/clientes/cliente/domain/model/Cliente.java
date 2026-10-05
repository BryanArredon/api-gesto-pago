package com.onboarding.clientes.cliente.domain.model;

import com.onboarding.clientes.cliente.domain.exception.ClienteMenorDeEdadException;
import com.onboarding.clientes.cliente.domain.valueobject.Correo;
import com.onboarding.clientes.cliente.domain.valueobject.Curp;
import com.onboarding.clientes.cliente.domain.valueobject.NumeroCuenta;
import com.onboarding.clientes.cliente.domain.valueobject.Rfc;
import com.onboarding.clientes.cliente.domain.valueobject.Telefono;
import com.onboarding.clientes.shared.dinero.Dinero;
import com.onboarding.clientes.shared.error.CodigoError;
import com.onboarding.clientes.shared.error.DatoInvalidoException;
import com.onboarding.clientes.shared.error.ErrorNegocio;
import com.onboarding.clientes.shared.id.UuidV7;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Raiz del agregado Cliente: identidad, contacto, domicilio, informacion laboral, su usuario de acceso y sus
 * cuentas bancarias.
 *
 * <p>Decisiones de diseno:
 *
 * <ul>
 *   <li><b>La invariantes viven aqui, no en el servicio ni en la base.</b> Todo cambio pasa por un metodo
 *       con nombre de intencion ({@code darDeBaja}, {@code actualizarContacto}) que valida antes de
 *       mutar, de forma que un agregado nunca queda en estado invalido, ni siquiera de forma transitoria.
 *   <li><b>CURP y RFC son inmutables</b> una vez registrado el cliente: identifican legalmente a la persona y
 *       cambiarlos es un fraude, no una actualizacion de perfil.
 *   <li><b>Baja logica</b>: la informacion nunca se borra (obligacion regulatoria de conservar el
 *       expediente); se marca {@code activo = false} con su fecha, y en cascada se inactivan la cuenta y
 *       el usuario de acceso.
 *   <li><b>Sin dependencias de framework:</b> esta clase no importa nada de Spring ni de JPA, por lo que el
 *       nucleo de negocio es testeable en la JVM sin contenedor (ver {@code ClienteTest}).
 * </ul>
 */
public class Cliente {

    /** Edad minima exigida para registrar un cliente persona fisica. */
    public static final int EDAD_MINIMA = 18;

    private final UUID id;
    private String nombre;
    private String segundoNombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private LocalDate fechaNacimiento;
    private final Curp curp;
    private final Rfc rfc;
    private Sexo sexo;
    private String nacionalidad;
    private EstadoCivil estadoCivil;
    private Correo correo;
    private Telefono telefonoMovil;
    private Telefono telefonoAlterno;
    private String ocupacion;
    private String empresa;
    private Dinero ingresoMensual;
    private Domicilio domicilio;
    private Usuario usuario;
    private final List<Cuenta> cuentas = new ArrayList<>();
    private boolean activo;
    private Instant fechaBaja;
    private Instant createdAt;
    private Instant updatedAt;

    private Cliente(UUID id, Curp curp, Rfc rfc, Instant createdAt) {
        this.id = id;
        this.curp = curp;
        this.rfc = rfc;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
        this.activo = true;
    }

    /**
     * Alta de cliente: valida las reglas de negocio y construye el agregado completo (persona, domicilio,
     * usuario de acceso y cuenta bancaria) listo para persistir en una sola transaccion.
     *
     * @param alta datos del proceso de alta
     * @return cliente activo con usuario y cuenta creada
     */
    public static Cliente registrar(AltaCliente alta) {
        return registrar(UuidV7.generar(), alta);
    }

    /**
     * Alta de cliente con identificador asignado por el caso de uso.
     *
     * <p>El identificador se genera en la capa de aplicacion porque el usuario de acceso necesita conocerlo
     * para vincularse al cliente en la misma transaccion: generar ambos identificadores en el mismo sitio
     * evita una segunda escritura o una relacion a medio construir.
     *
     * @param id identificador del cliente
     * @param alta datos del proceso de alta
     * @return cliente activo con usuario y cuenta creada
     */
    public static Cliente registrar(UUID id, AltaCliente alta) {
        Objects.requireNonNull(alta, "Los datos de alta son obligatorios");
        DatosPersonales persona = Objects.requireNonNull(alta.datosPersonales(), "Los datos personales son obligatorios");
        DatosContacto contacto = Objects.requireNonNull(alta.datosContacto(), "Los datos de contacto son obligatorios");
        DatosLaborales laborales = Objects.requireNonNull(alta.datosLaborales(), "Los datos laborales son obligatorios");

        Cliente cliente = new Cliente(Objects.requireNonNull(id, "El identificador del cliente es obligatorio"),
                persona.curp(), persona.rfc(), alta.momento());
        cliente.aplicarDatosPersonales(persona, alta.hoy());
        cliente.aplicarDatosContacto(contacto);
        cliente.aplicarDatosLaborales(laborales);
        cliente.domicilio = Objects.requireNonNull(alta.domicilio(), "El domicilio es obligatorio");
        cliente.usuario = Objects.requireNonNull(alta.usuario(), "El usuario de acceso es obligatorio");
        cliente.cuentas.add(Cuenta.abrir(cliente.id, Objects.requireNonNull(alta.tipoCuenta(), "El tipo de cuenta es obligatorio"),
                Objects.requireNonNull(alta.saldoInicial(), "El saldo inicial es obligatorio"), alta.momento()));
        return cliente;
    }

    /**
     * Reconstruye el agregado desde la base de datos. No vuelve a validar las invariantes (ya validas al
     * escribirse): duplicar la validacion en cada lectura solo anadiria coste.
     */
    public static Cliente rehidratar(
            UUID id,
            String nombre,
            String segundoNombre,
            String apellidoPaterno,
            String apellidoMaterno,
            LocalDate fechaNacimiento,
            Curp curp,
            Rfc rfc,
            Sexo sexo,
            String nacionalidad,
            EstadoCivil estadoCivil,
            Correo correo,
            Telefono telefonoMovil,
            Telefono telefonoAlterno,
            String ocupacion,
            String empresa,
            Dinero ingresoMensual,
            Domicilio domicilio,
            Usuario usuario,
            List<Cuenta> cuentas,
            boolean activo,
            Instant fechaBaja,
            Instant createdAt,
            Instant updatedAt) {
        Cliente cliente = new Cliente(id, curp, rfc, createdAt);
        cliente.nombre = nombre;
        cliente.segundoNombre = segundoNombre;
        cliente.apellidoPaterno = apellidoPaterno;
        cliente.apellidoMaterno = apellidoMaterno;
        cliente.fechaNacimiento = fechaNacimiento;
        cliente.sexo = sexo;
        cliente.nacionalidad = nacionalidad;
        cliente.estadoCivil = estadoCivil;
        cliente.correo = correo;
        cliente.telefonoMovil = telefonoMovil;
        cliente.telefonoAlterno = telefonoAlterno;
        cliente.ocupacion = ocupacion;
        cliente.empresa = empresa;
        cliente.ingresoMensual = ingresoMensual;
        cliente.domicilio = domicilio;
        cliente.usuario = usuario;
        cliente.cuentas.addAll(cuentas == null ? List.of() : cuentas);
        cliente.activo = activo;
        cliente.fechaBaja = fechaBaja;
        cliente.updatedAt = updatedAt;
        return cliente;
    }

    /**
     * Actualiza los datos modificables del cliente.
     *
     * <p>Rechaza de forma explicita el intento de cambiar la CURP o el RFC (inmutables por negocio) en lugar
     * de ignorarlo, para que el cliente de la API reciba un error claro y no una respuesta engaosa.
     *
     * @param actualizacion datos modificables
     */
    public void actualizar(ActualizacionCliente actualizacion) {
        exigeActivo();
        if (actualizacion.curpDeclarada() != null && !actualizacion.curpDeclarada().equals(this.curp)) {
            throw new ErrorNegocio(
                    CodigoError.CURP_INVALIDA, "La CURP es un dato inmutable y no puede modificarse despues del alta");
        }
        if (actualizacion.rfcDeclarado() != null && !actualizacion.rfcDeclarado().equals(this.rfc)) {
            throw new ErrorNegocio(
                    CodigoError.RFC_INVALIDO, "El RFC es un dato inmutable y no puede modificarse despues del alta");
        }
        aplicarDatosPersonales(actualizacion.datosPersonales(), actualizacion.hoy());
        aplicarDatosContacto(actualizacion.datosContacto());
        aplicarDatosLaborales(actualizacion.datosLaborales());
        this.domicilio = Objects.requireNonNull(actualizacion.domicilio(), "El domicilio es obligatorio");
        this.updatedAt = actualizacion.momento();
    }

    /**
     * Baja logica del cliente. Desactiva en cascada sus cuentas y su usuario de acceso: un cliente dado de
     * baja no puede tener cuentas activas ni autenticarse.
     *
     * @param momento instante de la baja
     */
    public void darDeBaja(Instant momento) {
        if (!activo) {
            throw new ErrorNegocio(CodigoError.CLIENTE_INACTIVO, "El cliente ya se encuentra inactivo");
        }
        activo = false;
        fechaBaja = momento;
        updatedAt = momento;
        cuentas.stream().filter(Cuenta::esActiva).forEach(Cuenta::inactivar);
        if (usuario != null && usuario.activo()) {
            usuario.darDeBaja();
        }
    }

    /** Reactiva un cliente dado de baja y rehabilita su usuario de acceso (no reactiva cuentas cerradas). */
    public void reactivar(Instant momento) {
        if (activo) {
            return;
        }
        activo = true;
        fechaBaja = null;
        updatedAt = momento;
        if (usuario != null) {
            usuario.reactivar();
        }
    }

    /** Asigna el numero de cuenta que genero la base de datos tras el alta. */
    public void asignarNumeroCuenta(NumeroCuenta numero) {
        cuentas.stream()
                .filter(cuenta -> !cuenta.tieneNumero())
                .findFirst()
                .ifPresent(cuenta -> cuenta.asignarNumero(numero));
    }

    public Optional<Cuenta> cuenta() {
        return cuentas.stream().findFirst();
    }

    public Optional<Cuenta> cuentaActiva() {
        return cuentas.stream().filter(Cuenta::esActiva).findFirst();
    }

    public List<Cuenta> cuentas() {
        return List.copyOf(cuentas);
    }

    /** @return nombre completo en formato de exhibicion */
    public String nombreCompleto() {
        StringBuilder nombre = new StringBuilder(this.nombre);
        if (segundoNombre != null && !segundoNombre.isBlank()) {
            nombre.append(' ').append(segundoNombre);
        }
        return nombre.append(' ').append(apellidoPaterno).append(' ').append(apellidoMaterno).toString();
    }

    public UUID id() {
        return id;
    }

    public String nombre() {
        return nombre;
    }

    public String segundoNombre() {
        return segundoNombre;
    }

    public String apellidoPaterno() {
        return apellidoPaterno;
    }

    public String apellidoMaterno() {
        return apellidoMaterno;
    }

    public LocalDate fechaNacimiento() {
        return fechaNacimiento;
    }

    public Curp curp() {
        return curp;
    }

    public Rfc rfc() {
        return rfc;
    }

    public Sexo sexo() {
        return sexo;
    }

    public String nacionalidad() {
        return nacionalidad;
    }

    public EstadoCivil estadoCivil() {
        return estadoCivil;
    }

    public Correo correo() {
        return correo;
    }

    public Telefono telefonoMovil() {
        return telefonoMovil;
    }

    public Telefono telefonoAlterno() {
        return telefonoAlterno;
    }

    public String ocupacion() {
        return ocupacion;
    }

    public String empresa() {
        return empresa;
    }

    public Dinero ingresoMensual() {
        return ingresoMensual;
    }

    public Domicilio domicilio() {
        return domicilio;
    }

    public Usuario usuario() {
        return usuario;
    }

    public boolean activo() {
        return activo;
    }

    public Instant fechaBaja() {
        return fechaBaja;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    private void exigeActivo() {
        if (!activo) {
            throw new ErrorNegocio(CodigoError.CLIENTE_INACTIVO, "No se puede modificar un cliente dado de baja");
        }
    }

    private void aplicarDatosPersonales(DatosPersonales persona, LocalDate hoy) {
        this.nombre = texto(persona.nombre(), "nombre");
        this.segundoNombre = textoOpcional(persona.segundoNombre(), "segundoNombre");
        this.apellidoPaterno = texto(persona.apellidoPaterno(), "apellidoPaterno");
        this.apellidoMaterno = texto(persona.apellidoMaterno(), "apellidoMaterno");

        LocalDate nacimiento = Objects.requireNonNull(persona.fechaNacimiento(), "La fecha de nacimiento es obligatoria");
        if (nacimiento.isAfter(hoy)) {
            throw new ErrorNegocio(
                    CodigoError.DATO_INVALIDO, "La fecha de nacimiento no puede ser una fecha futura");
        }
        if (ClienteMenorDeEdadException.edadEn(nacimiento, hoy) < EDAD_MINIMA) {
            throw new ClienteMenorDeEdadException(nacimiento, hoy, EDAD_MINIMA);
        }
        this.fechaNacimiento = nacimiento;

        // La identificacion oficial debe ser coherente con la fecha de nacimiento declarada.
        this.curp.exigeCoincidirCon(nacimiento);
        this.rfc.exigeCoincidirCon(nacimiento);

        this.sexo = Objects.requireNonNull(persona.sexo(), "El sexo es obligatorio");
        this.nacionalidad = texto(persona.nacionalidad(), "nacionalidad");
        this.estadoCivil = Objects.requireNonNull(persona.estadoCivil(), "El estado civil es obligatorio");
    }

    private void aplicarDatosContacto(DatosContacto contacto) {
        this.correo = Objects.requireNonNull(contacto.correo(), "El correo electronico es obligatorio");
        this.telefonoMovil = Objects.requireNonNull(contacto.telefonoMovil(), "El telefono movil es obligatorio");
        this.telefonoAlterno = contacto.telefonoAlterno();
    }

    private void aplicarDatosLaborales(DatosLaborales laborales) {
        this.ocupacion = texto(laborales.ocupacion(), "ocupacion");
        this.empresa = textoOpcional(laborales.empresa(), "empresa");
        Dinero ingreso = Objects.requireNonNull(laborales.ingresoMensual(), "El ingreso mensual es obligatorio");
        if (!ingreso.esPositivo()) {
            throw new ErrorNegocio(CodigoError.DATO_INVALIDO, "El ingreso mensual debe ser mayor que cero");
        }
        this.ingresoMensual = ingreso;
    }

    /** Normaliza y valida un texto obligatorio de 2 a 50 caracteres composed solo de letras y espacios. */
    private static String texto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new DatoInvalidoException("El campo " + campo + " es obligatorio", java.util.List.of(campo + ": es obligatorio"));
        }
        String normalizado = valor.trim().replaceAll("\\s+", " ");
        if (normalizado.length() < 2 || normalizado.length() > 50) {
            throw new DatoInvalidoException(
                    "El campo " + campo + " debe tener entre 2 y 50 caracteres",
                    java.util.List.of(campo + ": debe tener entre 2 y 50 caracteres"));
        }
        if (!normalizado.matches("^[\\p{L}]+( [\\p{L}]+)*$")) {
            throw new DatoInvalidoException(
                    "El campo " + campo + " solo admite letras y espacios",
                    java.util.List.of(campo + ": solo admite letras y espacios"));
        }
        return normalizado;
    }

    private static String textoOpcional(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return texto(valor, campo);
    }
}
