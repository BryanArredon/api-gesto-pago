package com.onboarding.clientes.cliente.api;

import com.onboarding.clientes.cliente.application.dto.CuentaDto;
import com.onboarding.clientes.cliente.application.dto.SaldoCuentaDto;
import com.onboarding.clientes.cliente.application.service.ServicioConsultaCuenta;
import com.onboarding.clientes.shared.web.Cursor;
import com.onboarding.clientes.shared.web.PaginaRespuesta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para consultas de cuentas bancarias.
 */
@RestController
@RequestMapping("/cuentas")
@Tag(name = "Cuentas", description = "Consultas de cuentas bancarias y saldos")
public class CuentaController {

    private final ServicioConsultaCuenta servicioConsultaCuenta;

    public CuentaController(ServicioConsultaCuenta servicioConsultaCuenta) {
        this.servicioConsultaCuenta = servicioConsultaCuenta;
    }

    @GetMapping("/{numeroCuenta}")
    @Operation(summary = "Consultar cuenta por número")
    public ResponseEntity<CuentaDto> buscarPorNumero(@PathVariable String numeroCuenta) {
        return ResponseEntity.ok(servicioConsultaCuenta.buscarPorNumero(numeroCuenta));
    }

    @GetMapping("/{numeroCuenta}/saldo")
    @Operation(summary = "Consultar saldo disponible de una cuenta")
    public ResponseEntity<SaldoCuentaDto> consultarSaldo(@PathVariable String numeroCuenta) {
        return ResponseEntity.ok(servicioConsultaCuenta.consultarSaldo(numeroCuenta));
    }

    @GetMapping("/activas")
    @Operation(summary = "Consultar lista de cuentas activas con paginación")
    public ResponseEntity<PaginaRespuesta<CuentaDto>> listarActivas(
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limite) {

        Cursor cursorObj = Cursor.decodificar(cursor);
        return ResponseEntity.ok(servicioConsultaCuenta.listarActivas(cursorObj, limite));
    }
}
