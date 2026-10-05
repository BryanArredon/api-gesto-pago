package com.onboarding.clientes.cliente.application.mapper;

import com.onboarding.clientes.cliente.application.dto.ClienteDetalle;
import com.onboarding.clientes.cliente.application.dto.ClienteResumen;
import com.onboarding.clientes.cliente.application.dto.CuentaDto;
import com.onboarding.clientes.cliente.application.dto.DomicilioDto;
import com.onboarding.clientes.cliente.application.dto.UsuarioDto;
import com.onboarding.clientes.cliente.domain.model.Cliente;
import com.onboarding.clientes.cliente.domain.model.Cuenta;
import com.onboarding.clientes.cliente.domain.model.Domicilio;
import com.onboarding.clientes.cliente.domain.model.Usuario;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Traductor entre el modelo de dominio y las proyecciones de lectura.
 *
 * <p>Existe para que los controladores nunca expongan entidades de persistencia ni objetos de dominio: la
 * API responde con records inmutables y sin comportamiento.
 *
 * <p>El mapeo se escribe a mano y no con MapStruct a proposito: el dominio expone metodos con nombre de
 * dominio ({@code nombre()}, {@code curp()}, {@code domicilio()}) en vez de accessores JavaBean, y obligar al
 * dominio a adoptar esa convencion para satisfacer una herramienta ensuciaria el nucleo de negocio. Ademas,
 * el compilador de MapStruct solo detecta el campo anadido en tiempo de compilacion si se regenera el
 * implementador, mientras que aqui el fallo aparece en la prueba que verifica la proyeccion completa.
 */
@Component
public class ClienteMapeador {

    public ClienteDetalle aDetalle(Cliente cliente) {
        return new ClienteDetalle(
                cliente.id(),
                cliente.nombre(),
                cliente.segundoNombre(),
                cliente.apellidoPaterno(),
                cliente.apellidoMaterno(),
                cliente.nombreCompleto(),
                cliente.fechaNacimiento(),
                cliente.curp(),
                cliente.rfc(),
                cliente.sexo(),
                cliente.nacionalidad(),
                cliente.estadoCivil(),
                cliente.correo(),
                cliente.telefonoMovil(),
                cliente.telefonoAlterno(),
                cliente.ocupacion(),
                cliente.empresa(),
                cliente.ingresoMensual().monto(),
                cliente.ingresoMensual().moneda(),
                cliente.activo(),
                cliente.fechaBaja(),
                cliente.createdAt(),
                cliente.updatedAt(),
                aDomicilio(cliente.domicilio()),
                cliente.cuenta().map(this::aDto).orElse(null),
                cliente.usuario() == null ? null : aDto(cliente.usuario()));
    }

    public List<ClienteDetalle> aDetalles(List<Cliente> clientes) {
        return clientes.stream().map(this::aDetalle).toList();
    }

    public ClienteResumen aResumen(Cliente cliente) {
        return new ClienteResumen(
                cliente.id(),
                cliente.nombreCompleto(),
                cliente.curp(),
                cliente.rfc(),
                cliente.correo(),
                cliente.fechaNacimiento(),
                cliente.sexo(),
                cliente.estadoCivil(),
                cliente.cuenta()
                        .filter(Cuenta::tieneNumero)
                        .map(cuenta -> cuenta.numeroCuenta().valor())
                        .orElse(null),
                cliente.activo(),
                cliente.createdAt());
    }

    public List<ClienteResumen> aResumen(List<Cliente> clientes) {
        return clientes.stream().map(this::aResumen).toList();
    }

    public CuentaDto aDto(Cuenta cuenta) {
        return new CuentaDto(
                cuenta.id(),
                cuenta.clienteId(),
                cuenta.numeroCuenta(),
                cuenta.tipo(),
                cuenta.saldo().moneda(),
                cuenta.saldo().monto(),
                cuenta.estatus(),
                cuenta.createdAt(),
                cuenta.updatedAt());
    }

    public List<CuentaDto> aDtos(List<Cuenta> cuentas) {
        return cuentas.stream().map(this::aDto).toList();
    }

    public UsuarioDto aDto(Usuario usuario) {
        return new UsuarioDto(
                usuario.id(),
                usuario.clienteId(),
                usuario.correo(),
                usuario.activo(),
                usuario.tienePin(),
                usuario.ultimoAcceso(),
                usuario.createdAt(),
                usuario.updatedAt());
    }

    public DomicilioDto aDomicilio(Domicilio domicilio) {
        if (domicilio == null) {
            return null;
        }
        return new DomicilioDto(
                domicilio.calle(),
                domicilio.numeroExterior(),
                domicilio.numeroInterior(),
                domicilio.colonia(),
                domicilio.municipio(),
                domicilio.estado(),
                domicilio.codigoPostal(),
                domicilio.pais());
    }
}
