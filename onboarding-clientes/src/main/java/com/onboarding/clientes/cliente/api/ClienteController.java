package com.onboarding.clientes.cliente.api;

import com.onboarding.clientes.cliente.api.request.ActualizarClienteRequest;
import com.onboarding.clientes.cliente.api.request.RegistrarClienteRequest;
import com.onboarding.clientes.cliente.application.command.ComandoActualizarCliente;
import com.onboarding.clientes.cliente.application.command.ComandoRegistrarCliente;
import com.onboarding.clientes.cliente.application.dto.ClienteDetalle;
import com.onboarding.clientes.cliente.application.dto.ClienteResumen;
import com.onboarding.clientes.cliente.application.service.ServicioActualizacionCliente;
import com.onboarding.clientes.cliente.application.service.ServicioBajaCliente;
import com.onboarding.clientes.cliente.application.service.ServicioConsultaCliente;
import com.onboarding.clientes.cliente.application.service.ServicioRegistroCliente;
import com.onboarding.clientes.shared.web.Cursor;
import com.onboarding.clientes.shared.web.PaginaRespuesta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para la gestion de clientes personas fisicas.
 */
@RestController
@RequestMapping("/clientes")
@Tag(name = "Clientes", description = "Operaciones de onboarding, consulta, actualizacion y baja de clientes")
public class ClienteController {

    private final ServicioRegistroCliente servicioRegistro;
    private final ServicioConsultaCliente servicioConsulta;
    private final ServicioActualizacionCliente servicioActualizacion;
    private final ServicioBajaCliente servicioBaja;

    public ClienteController(
            ServicioRegistroCliente servicioRegistro,
            ServicioConsultaCliente servicioConsulta,
            ServicioActualizacionCliente servicioActualizacion,
            ServicioBajaCliente servicioBaja) {
        this.servicioRegistro = servicioRegistro;
        this.servicioConsulta = servicioConsulta;
        this.servicioActualizacion = servicioActualizacion;
        this.servicioBaja = servicioBaja;
    }

    @PostMapping
    @Operation(summary = "Registrar nuevo cliente", description = "Crea el cliente, su domicilio, su cuenta bancaria y su usuario de acceso en una sola transaccion.")
    @ApiResponse(responseCode = "201", description = "Cliente registrado exitosamente")
    @ApiResponse(responseCode = "400", description = "Datos de entrada invalidos")
    @ApiResponse(responseCode = "409", description = "CURP, RFC o Correo duplicado")
    public ResponseEntity<ClienteDetalle> registrar(
            @Valid @RequestBody RegistrarClienteRequest request,
            HttpServletRequest httpRequest) {

        ComandoRegistrarCliente.Persona persona = new ComandoRegistrarCliente.Persona(
                request.persona().nombre(),
                request.persona().segundoNombre(),
                request.persona().apellidoPaterno(),
                request.persona().apellidoMaterno(),
                request.persona().fechaNacimiento(),
                request.persona().curp(),
                request.persona().rfc(),
                request.persona().sexo(),
                request.persona().nacionalidad(),
                request.persona().estadoCivil());

        ComandoRegistrarCliente.Contacto contacto = new ComandoRegistrarCliente.Contacto(
                request.contacto().correo(),
                request.contacto().telefonoMovil(),
                request.contacto().telefonoAlterno());

        ComandoRegistrarCliente.Ubicacion ubicacion = new ComandoRegistrarCliente.Ubicacion(
                request.ubicacion().calle(),
                request.ubicacion().numeroExterior(),
                request.ubicacion().numeroInterior(),
                request.ubicacion().colonia(),
                request.ubicacion().municipio(),
                request.ubicacion().estado(),
                request.ubicacion().codigoPostal(),
                request.ubicacion().pais());

        ComandoRegistrarCliente.Laboral laboral = new ComandoRegistrarCliente.Laboral(
                request.laboral().ocupacion(),
                request.laboral().empresa(),
                request.laboral().ingresoMensual(),
                request.laboral().monedaIngreso());

        ComandoRegistrarCliente.Credenciales credenciales = new ComandoRegistrarCliente.Credenciales(
                request.credenciales().contrasenia(),
                request.credenciales().pin());

        ComandoRegistrarCliente.AperturaCuenta cuenta = request.cuenta() != null
                ? new ComandoRegistrarCliente.AperturaCuenta(
                        request.cuenta().saldoInicial(),
                        request.cuenta().tipoCuenta(),
                        request.cuenta().monedaCuenta())
                : new ComandoRegistrarCliente.AperturaCuenta(null, null, null);

        ComandoRegistrarCliente comando = new ComandoRegistrarCliente(
                persona, contacto, ubicacion, laboral, credenciales, cuenta);

        ClienteDetalle detalle = servicioRegistro.registrar(comando);
        return ResponseEntity.created(URI.create("/clientes/" + detalle.id())).body(detalle);
    }

    @GetMapping
    @Operation(summary = "Listar clientes", description = "Consulta clientes con paginacion por cursor y filtros opcionales (activos o rango de fechas).")
    public ResponseEntity<PaginaRespuesta<ClienteResumen>> listar(
            @RequestParam(required = false, defaultValue = "false") boolean soloActivos,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limite) {

        Cursor cursorObj = Cursor.decodificar(cursor);

        PaginaRespuesta<ClienteResumen> respuesta;
        if (desde != null && hasta != null) {
            respuesta = servicioConsulta.listarRegistradosEntre(desde, hasta, cursorObj, limite);
        } else {
            respuesta = servicioConsulta.listar(cursorObj, limite, soloActivos);
        }
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar cliente por ID")
    public ResponseEntity<ClienteDetalle> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(servicioConsulta.buscarPorId(id));
    }

    @GetMapping("/curp/{curp}")
    @Operation(summary = "Buscar cliente por CURP")
    public ResponseEntity<ClienteDetalle> buscarPorCurp(@PathVariable String curp) {
        return ResponseEntity.ok(servicioConsulta.buscarPorCurp(curp));
    }

    @GetMapping("/rfc/{rfc}")
    @Operation(summary = "Buscar cliente por RFC")
    public ResponseEntity<ClienteDetalle> buscarPorRfc(@PathVariable String rfc) {
        return ResponseEntity.ok(servicioConsulta.buscarPorRfc(rfc));
    }

    @GetMapping("/correo/{correo}")
    @Operation(summary = "Buscar cliente por correo electrónico")
    public ResponseEntity<ClienteDetalle> buscarPorCorreo(@PathVariable String correo) {
        return ResponseEntity.ok(servicioConsulta.buscarPorCorreo(correo));
    }

    @GetMapping("/cuenta/{numeroCuenta}")
    @Operation(summary = "Buscar cliente por número de cuenta bancaria")
    public ResponseEntity<ClienteDetalle> buscarPorNumeroCuenta(@PathVariable String numeroCuenta) {
        return ResponseEntity.ok(servicioConsulta.buscarPorNumeroCuenta(numeroCuenta));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar información de cliente", description = "Actualiza datos personales, contacto, domicilio y laboral. Protege la inmutabilidad de CURP, RFC y cuenta.")
    public ResponseEntity<ClienteDetalle> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ActualizarClienteRequest request) {

        ComandoActualizarCliente.Persona persona = new ComandoActualizarCliente.Persona(
                request.persona().nombre(),
                request.persona().segundoNombre(),
                request.persona().apellidoPaterno(),
                request.persona().apellidoMaterno(),
                request.persona().fechaNacimiento(),
                request.persona().curpDeclarada(),
                request.persona().rfcDeclarado(),
                request.persona().sexo(),
                request.persona().nacionalidad(),
                request.persona().estadoCivil());

        ComandoActualizarCliente.Contacto contacto = new ComandoActualizarCliente.Contacto(
                request.contacto().correo(),
                request.contacto().telefonoMovil(),
                request.contacto().telefonoAlterno());

        ComandoActualizarCliente.Ubicacion ubicacion = new ComandoActualizarCliente.Ubicacion(
                request.ubicacion().calle(),
                request.ubicacion().numeroExterior(),
                request.ubicacion().numeroInterior(),
                request.ubicacion().colonia(),
                request.ubicacion().municipio(),
                request.ubicacion().estado(),
                request.ubicacion().codigoPostal(),
                request.ubicacion().pais());

        ComandoActualizarCliente.Laboral laboral = new ComandoActualizarCliente.Laboral(
                request.laboral().ocupacion(),
                request.laboral().empresa(),
                request.laboral().ingresoMensual(),
                request.laboral().monedaIngreso());

        ComandoActualizarCliente comando = new ComandoActualizarCliente(id, persona, contacto, ubicacion, laboral);
        return ResponseEntity.ok(servicioActualizacion.actualizar(id, comando));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Baja lógica de cliente", description = "Desactiva al cliente y en cascada a sus cuentas bancarias y usuario de acceso.")
    public ResponseEntity<Void> darDeBaja(@PathVariable UUID id) {
        servicioBaja.darDeBaja(id);
        return ResponseEntity.noContent().build();
    }
}
