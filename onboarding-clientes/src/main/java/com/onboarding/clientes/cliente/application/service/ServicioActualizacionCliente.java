package com.onboarding.clientes.cliente.application.service;

import com.onboarding.clientes.cliente.application.command.ComandoActualizarCliente;
import com.onboarding.clientes.cliente.application.dto.ClienteDetalle;
import com.onboarding.clientes.cliente.application.mapper.ClienteMapeador;
import com.onboarding.clientes.cliente.application.port.ClienteRepositorio;
import com.onboarding.clientes.cliente.domain.exception.ClienteNoEncontradoException;
import com.onboarding.clientes.cliente.domain.exception.CorreoDuplicadoException;
import com.onboarding.clientes.cliente.domain.model.ActualizacionCliente;
import com.onboarding.clientes.cliente.domain.model.Cliente;
import com.onboarding.clientes.cliente.domain.model.DatosContacto;
import com.onboarding.clientes.cliente.domain.model.DatosLaborales;
import com.onboarding.clientes.cliente.domain.model.DatosPersonales;
import com.onboarding.clientes.cliente.domain.model.Domicilio;
import com.onboarding.clientes.cliente.domain.valueobject.CodigoPostal;
import com.onboarding.clientes.cliente.domain.valueobject.Correo;
import com.onboarding.clientes.cliente.domain.valueobject.Curp;
import com.onboarding.clientes.cliente.domain.valueobject.Rfc;
import com.onboarding.clientes.cliente.domain.valueobject.Telefono;
import com.onboarding.clientes.shared.dinero.Dinero;
import com.onboarding.clientes.shared.evento.EventoDominio;
import com.onboarding.clientes.shared.evento.PublicadorEvento;
import com.onboarding.clientes.shared.tiempo.Reloj;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de aplicacion para la actualizacion de datos de un cliente.
 */
@Service
public class ServicioActualizacionCliente {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServicioActualizacionCliente.class);
    private static final String ZONA_OPERATIVA = "America/Mexico_City";

    private final ClienteRepositorio clientes;
    private final ClienteMapeador mapeador;
    private final PublicadorEvento publicadorEvento;
    private final Reloj reloj;

    public ServicioActualizacionCliente(
            ClienteRepositorio clientes,
            ClienteMapeador mapeador,
            PublicadorEvento publicadorEvento,
            Reloj reloj) {
        this.clientes = clientes;
        this.mapeador = mapeador;
        this.publicadorEvento = publicadorEvento;
        this.reloj = reloj;
    }

    @Transactional
    public ClienteDetalle actualizar(UUID id, ComandoActualizarCliente comando) {
        Cliente cliente = clientes.buscarPorId(id)
                .orElseThrow(() -> new ClienteNoEncontradoException(id));

        Correo nuevoCorreo = Correo.de(comando.contacto().correo());
        if (!cliente.correo().equals(nuevoCorreo)) {
            if (clientes.existePorCorreo(nuevoCorreo)) {
                throw new CorreoDuplicadoException(nuevoCorreo);
            }
        }

        Instant momento = reloj.ahora();
        ZoneId zone = ZoneId.of(ZONA_OPERATIVA);

        Curp curpDeclarada = comando.persona().curpDeclarada() != null && !comando.persona().curpDeclarada().isBlank()
                ? Curp.de(comando.persona().curpDeclarada())
                : null;
        Rfc rfcDeclarado = comando.persona().rfcDeclarado() != null && !comando.persona().rfcDeclarado().isBlank()
                ? Rfc.de(comando.persona().rfcDeclarado())
                : null;

        Telefono alterno = comando.contacto().telefonoAlterno() != null && !comando.contacto().telefonoAlterno().isBlank()
                ? Telefono.de(comando.contacto().telefonoAlterno())
                : null;

        Domicilio domicilio = new Domicilio(
                comando.ubicacion().calle(),
                comando.ubicacion().numeroExterior(),
                comando.ubicacion().numeroInterior(),
                comando.ubicacion().colonia(),
                comando.ubicacion().municipio(),
                comando.ubicacion().estado(),
                CodigoPostal.de(comando.ubicacion().codigoPostal()),
                comando.ubicacion().pais());

        ActualizacionCliente actualizacion = new ActualizacionCliente(
                new DatosPersonales(
                        comando.persona().nombre(),
                        comando.persona().segundoNombre(),
                        comando.persona().apellidoPaterno(),
                        comando.persona().apellidoMaterno(),
                        comando.persona().fechaNacimiento(),
                        cliente.curp(),
                        cliente.rfc(),
                        comando.persona().sexo(),
                        comando.persona().nacionalidad(),
                        comando.persona().estadoCivil()),
                new DatosContacto(nuevoCorreo, Telefono.de(comando.contacto().telefonoMovil()), alterno),
                new DatosLaborales(
                        comando.laboral().ocupacion(),
                        comando.laboral().empresa(),
                        Dinero.de(comando.laboral().ingresoMensual(), comando.laboral().monedaIngreso())),
                domicilio,
                curpDeclarada,
                rfcDeclarado,
                momento,
                reloj.hoy(zone));

        cliente.actualizar(actualizacion);
        Cliente guardado = clientes.guardar(cliente);

        publicadorEvento.publicar(EventoDominio.crear(
                "cliente.actualizado",
                EventoDominio.TIPO_CLIENTE,
                guardado.id(),
                Map.of("curp", guardado.curp().valor(), "correo", guardado.correo().valor()),
                "sistema",
                momento));

        LOGGER.info("Cliente actualizado correctamente: id={}", guardado.id());
        return mapeador.aDetalle(guardado);
    }
}
