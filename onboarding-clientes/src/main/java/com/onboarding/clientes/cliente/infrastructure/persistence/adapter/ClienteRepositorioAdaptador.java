package com.onboarding.clientes.cliente.infrastructure.persistence.adapter;

import com.onboarding.clientes.cliente.application.port.ClienteRepositorio;
import com.onboarding.clientes.cliente.domain.model.Cliente;
import com.onboarding.clientes.cliente.domain.model.Cuenta;
import com.onboarding.clientes.cliente.domain.model.Domicilio;
import com.onboarding.clientes.cliente.domain.model.EstadoCivil;
import com.onboarding.clientes.cliente.domain.model.EstatusCuenta;
import com.onboarding.clientes.cliente.domain.model.Sexo;
import com.onboarding.clientes.cliente.domain.model.TipoCuenta;
import com.onboarding.clientes.cliente.domain.model.Usuario;
import com.onboarding.clientes.cliente.domain.valueobject.CodigoPostal;
import com.onboarding.clientes.cliente.domain.valueobject.Correo;
import com.onboarding.clientes.cliente.domain.valueobject.Curp;
import com.onboarding.clientes.cliente.domain.valueobject.NumeroCuenta;
import com.onboarding.clientes.cliente.domain.valueobject.Rfc;
import com.onboarding.clientes.cliente.domain.valueobject.Telefono;
import com.onboarding.clientes.cliente.infrastructure.persistence.entity.ClienteEntity;
import com.onboarding.clientes.cliente.infrastructure.persistence.entity.CuentaEntity;
import com.onboarding.clientes.cliente.infrastructure.persistence.entity.DomicilioEntity;
import com.onboarding.clientes.cliente.infrastructure.persistence.entity.UsuarioEntity;
import com.onboarding.clientes.cliente.infrastructure.persistence.jpa.ClienteSpringDataRepository;
import com.onboarding.clientes.cliente.infrastructure.persistence.jpa.CuentaSpringDataRepository;
import com.onboarding.clientes.cliente.infrastructure.persistence.jpa.DomicilioSpringDataRepository;
import com.onboarding.clientes.cliente.infrastructure.persistence.jpa.UsuarioSpringDataRepository;
import com.onboarding.clientes.shared.dinero.Dinero;
import com.onboarding.clientes.shared.web.Cursor;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * Adaptador de persistencia JPA para el agregado Cliente.
 */
@Component
public class ClienteRepositorioAdaptador implements ClienteRepositorio {

    private final ClienteSpringDataRepository clienteRepository;
    private final DomicilioSpringDataRepository domicilioRepository;
    private final CuentaSpringDataRepository cuentaRepository;
    private final UsuarioSpringDataRepository usuarioRepository;

    public ClienteRepositorioAdaptador(
            ClienteSpringDataRepository clienteRepository,
            DomicilioSpringDataRepository domicilioRepository,
            CuentaSpringDataRepository cuentaRepository,
            UsuarioSpringDataRepository usuarioRepository) {
        this.clienteRepository = clienteRepository;
        this.domicilioRepository = domicilioRepository;
        this.cuentaRepository = cuentaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public Cliente guardar(Cliente cliente) {
        // 1. Guardar Cliente
        ClienteEntity clienteEntity = new ClienteEntity();
        clienteEntity.setId(cliente.id());
        clienteEntity.setNombre(cliente.nombre());
        clienteEntity.setSegundoNombre(cliente.segundoNombre());
        clienteEntity.setApellidoPaterno(cliente.apellidoPaterno());
        clienteEntity.setApellidoMaterno(cliente.apellidoMaterno());
        clienteEntity.setFechaNacimiento(cliente.fechaNacimiento());
        clienteEntity.setCurp(cliente.curp().valor());
        clienteEntity.setRfc(cliente.rfc().valor());
        clienteEntity.setSexo(cliente.sexo().name());
        clienteEntity.setNacionalidad(cliente.nacionalidad());
        clienteEntity.setEstadoCivil(cliente.estadoCivil().name());
        clienteEntity.setCorreoElectronico(cliente.correo().valor());
        clienteEntity.setTelefonoMovil(cliente.telefonoMovil().valor());
        clienteEntity.setTelefonoAlterno(cliente.telefonoAlterno() != null ? cliente.telefonoAlterno().valor() : null);
        clienteEntity.setOcupacion(cliente.ocupacion());
        clienteEntity.setEmpresa(cliente.empresa());
        clienteEntity.setIngresoMensual(cliente.ingresoMensual().monto());
        clienteEntity.setActivo(cliente.activo());
        clienteEntity.setFechaBaja(cliente.fechaBaja());
        clienteEntity.setCreatedAt(cliente.createdAt());
        clienteEntity.setUpdatedAt(cliente.updatedAt());

        clienteRepository.saveAndFlush(clienteEntity);

        // 2. Guardar Domicilio
        if (cliente.domicilio() != null) {
            Domicilio d = cliente.domicilio();
            DomicilioEntity domicilioEntity = domicilioRepository.findByClienteId(cliente.id())
                    .orElseGet(DomicilioEntity::new);
            if (domicilioEntity.getId() == null) {
                domicilioEntity.setId(UUID.randomUUID());
                domicilioEntity.setClienteId(cliente.id());
                domicilioEntity.setCreatedAt(cliente.createdAt());
            }
            domicilioEntity.setCalle(d.calle());
            domicilioEntity.setNumeroExterior(d.numeroExterior());
            domicilioEntity.setNumeroInterior(d.numeroInterior());
            domicilioEntity.setColonia(d.colonia());
            domicilioEntity.setMunicipio(d.municipio());
            domicilioEntity.setEstado(d.estado());
            domicilioEntity.setCodigoPostal(d.codigoPostal().valor());
            domicilioEntity.setPais(d.pais());
            domicilioEntity.setUpdatedAt(cliente.updatedAt());
            domicilioRepository.saveAndFlush(domicilioEntity);
        }

        // 3. Guardar Usuario
        if (cliente.usuario() != null) {
            Usuario u = cliente.usuario();
            UsuarioEntity usuarioEntity = usuarioRepository.findByClienteId(cliente.id())
                    .orElseGet(UsuarioEntity::new);
            usuarioEntity.setId(u.id());
            usuarioEntity.setClienteId(cliente.id());
            usuarioEntity.setCorreoElectronico(u.correo().valor());
            usuarioEntity.setPasswordHash(u.hashContrasena());
            usuarioEntity.setPinHash(u.hashPin());
            usuarioEntity.setActivo(u.activo());
            usuarioEntity.setIntentosFallidos(u.intentosFallidos());
            usuarioEntity.setBloqueadoHasta(u.bloqueadoHasta());
            usuarioEntity.setUltimoAcceso(u.ultimoAcceso());
            usuarioEntity.setKeycloakSub(u.keycloakSub());
            usuarioEntity.setCreatedAt(u.createdAt());
            usuarioEntity.setUpdatedAt(u.updatedAt());
            usuarioRepository.saveAndFlush(usuarioEntity);
        }

        // 4. Guardar Cuentas
        for (Cuenta c : cliente.cuentas()) {
            CuentaEntity cuentaEntity = cuentaRepository.findById(c.id()).orElseGet(CuentaEntity::new);
            cuentaEntity.setId(c.id());
            cuentaEntity.setClienteId(cliente.id());
            cuentaEntity.setTipoCuenta(c.tipo().name());
            cuentaEntity.setMoneda(c.saldo().moneda());
            cuentaEntity.setSaldo(c.saldo().monto());
            cuentaEntity.setEstatus(c.estatus().name());
            cuentaEntity.setVersion(c.version());
            cuentaEntity.setCreatedAt(c.createdAt());
            cuentaEntity.setUpdatedAt(c.updatedAt());
            if (c.tieneNumero()) {
                cuentaEntity.setNumeroCuenta(c.numeroCuenta().valor());
            }

            CuentaEntity savedCuenta = cuentaRepository.saveAndFlush(cuentaEntity);
            if (!c.tieneNumero() && savedCuenta.getNumeroCuenta() != null) {
                c.asignarNumero(NumeroCuenta.de(savedCuenta.getNumeroCuenta()));
            }
        }

        return reconstruir(clienteEntity);
    }

    @Override
    public Optional<Cliente> buscarPorId(UUID id) {
        return clienteRepository.findById(id).map(this::reconstruir);
    }

    @Override
    public Optional<Cliente> buscarPorCurp(Curp curp) {
        return clienteRepository.findByCurp(curp.valor()).map(this::reconstruir);
    }

    @Override
    public Optional<Cliente> buscarPorRfc(Rfc rfc) {
        return clienteRepository.findByRfc(rfc.valor()).map(this::reconstruir);
    }

    @Override
    public Optional<Cliente> buscarPorCorreo(Correo correo) {
        return clienteRepository.findByCorreoElectronicoIgnoreCase(correo.valor()).map(this::reconstruir);
    }

    @Override
    public List<Cliente> listar(Cursor cursor, int limite, boolean soloActivos) {
        Instant cursorTime = cursor != null ? cursor.marcaTiempo() : null;
        UUID cursorId = cursor != null ? cursor.id() : null;

        List<ClienteEntity> entities = clienteRepository.listarConCursor(
                cursorTime, cursorId, soloActivos, PageRequest.of(0, limite + 1));
        return entities.stream().map(this::reconstruir).toList();
    }

    @Override
    public List<Cliente> listarRegistradosEntre(Instant desde, Instant hasta, Cursor cursor, int limite) {
        Instant cursorTime = cursor != null ? cursor.marcaTiempo() : null;
        UUID cursorId = cursor != null ? cursor.id() : null;

        List<ClienteEntity> entities = clienteRepository.listarEntreFechasConCursor(
                desde, hasta, cursorTime, cursorId, PageRequest.of(0, limite + 1));
        return entities.stream().map(this::reconstruir).toList();
    }

    @Override
    public long contarActivos() {
        return clienteRepository.countByActivoTrue();
    }

    @Override
    public boolean existePorCurp(Curp curp) {
        return clienteRepository.existsByCurp(curp.valor());
    }

    @Override
    public boolean existePorRfc(Rfc rfc) {
        return clienteRepository.existsByRfc(rfc.valor());
    }

    @Override
    public boolean existePorCorreo(Correo correo) {
        return clienteRepository.existsByCorreoElectronicoIgnoreCase(correo.valor());
    }

    private Cliente reconstruir(ClienteEntity e) {
        Domicilio domicilio = domicilioRepository.findByClienteId(e.getId())
                .map(d -> new Domicilio(
                        d.getCalle(),
                        d.getNumeroExterior(),
                        d.getNumeroInterior(),
                        d.getColonia(),
                        d.getMunicipio(),
                        d.getEstado(),
                        CodigoPostal.de(d.getCodigoPostal()),
                        d.getPais()))
                .orElse(null);

        Usuario usuario = usuarioRepository.findByClienteId(e.getId())
                .map(u -> Usuario.rehidratar(
                        u.getId(),
                        u.getClienteId(),
                        Correo.de(u.getCorreoElectronico()),
                        u.getPasswordHash(),
                        u.getPinHash(),
                        u.isActivo(),
                        u.getIntentosFallidos(),
                        u.getBloqueadoHasta(),
                        u.getUltimoAcceso(),
                        u.getKeycloakSub(),
                        u.getCreatedAt(),
                        u.getUpdatedAt()))
                .orElse(null);

        List<Cuenta> cuentas = new ArrayList<>();
        for (CuentaEntity c : cuentaRepository.findByClienteId(e.getId())) {
            cuentas.add(Cuenta.rehidratar(
                    c.getId(),
                    c.getClienteId(),
                    c.getNumeroCuenta() != null ? NumeroCuenta.de(c.getNumeroCuenta()) : null,
                    TipoCuenta.valueOf(c.getTipoCuenta()),
                    Dinero.de(c.getSaldo(), c.getMoneda()),
                    EstatusCuenta.valueOf(c.getEstatus()),
                    c.getVersion() != null ? c.getVersion() : 0L,
                    c.getCreatedAt(),
                    c.getUpdatedAt()));
        }

        return Cliente.rehidratar(
                e.getId(),
                e.getNombre(),
                e.getSegundoNombre(),
                e.getApellidoPaterno(),
                e.getApellidoMaterno(),
                e.getFechaNacimiento(),
                Curp.de(e.getCurp()),
                Rfc.de(e.getRfc()),
                Sexo.valueOf(e.getSexo()),
                e.getNacionalidad(),
                EstadoCivil.valueOf(e.getEstadoCivil()),
                Correo.de(e.getCorreoElectronico()),
                Telefono.de(e.getTelefonoMovil()),
                e.getTelefonoAlterno() != null ? Telefono.de(e.getTelefonoAlterno()) : null,
                e.getOcupacion(),
                e.getEmpresa(),
                Dinero.de(e.getIngresoMensual(), "MXN"),
                domicilio,
                usuario,
                cuentas,
                e.isActivo(),
                e.getFechaBaja(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }
}
