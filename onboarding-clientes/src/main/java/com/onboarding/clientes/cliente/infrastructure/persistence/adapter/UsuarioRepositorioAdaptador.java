package com.onboarding.clientes.cliente.infrastructure.persistence.adapter;

import com.onboarding.clientes.cliente.application.port.UsuarioRepositorio;
import com.onboarding.clientes.cliente.domain.model.Usuario;
import com.onboarding.clientes.cliente.domain.valueobject.Correo;
import com.onboarding.clientes.cliente.infrastructure.persistence.entity.UsuarioEntity;
import com.onboarding.clientes.cliente.infrastructure.persistence.jpa.UsuarioSpringDataRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adaptador JPA para el puerto UsuarioRepositorio.
 */
@Component
public class UsuarioRepositorioAdaptador implements UsuarioRepositorio {

    private final UsuarioSpringDataRepository usuarioRepository;

    public UsuarioRepositorioAdaptador(UsuarioSpringDataRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public Usuario guardar(Usuario usuario) {
        UsuarioEntity entity = usuarioRepository.findById(usuario.id()).orElseGet(UsuarioEntity::new);
        entity.setId(usuario.id());
        entity.setClienteId(usuario.clienteId());
        entity.setCorreoElectronico(usuario.correo().valor());
        entity.setPasswordHash(usuario.hashContrasena());
        entity.setPinHash(usuario.hashPin());
        entity.setActivo(usuario.activo());
        entity.setIntentosFallidos(usuario.intentosFallidos());
        entity.setBloqueadoHasta(usuario.bloqueadoHasta());
        entity.setUltimoAcceso(usuario.ultimoAcceso());
        entity.setKeycloakSub(usuario.keycloakSub());
        entity.setCreatedAt(usuario.createdAt());
        entity.setUpdatedAt(usuario.updatedAt());

        UsuarioEntity saved = usuarioRepository.saveAndFlush(entity);
        return aDominio(saved);
    }

    @Override
    public Optional<Usuario> buscarPorId(UUID id) {
        return usuarioRepository.findById(id).map(this::aDominio);
    }

    @Override
    public Optional<Usuario> buscarPorCorreo(Correo correo) {
        return usuarioRepository.findByCorreoElectronicoIgnoreCase(correo.valor()).map(this::aDominio);
    }

    @Override
    public boolean existePorCorreo(Correo correo) {
        return usuarioRepository.existsByCorreoElectronicoIgnoreCase(correo.valor());
    }

    @Override
    public boolean existePorCliente(UUID clienteId) {
        return usuarioRepository.existsByClienteId(clienteId);
    }

    private Usuario aDominio(UsuarioEntity u) {
        return Usuario.rehidratar(
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
                u.getUpdatedAt());
    }
}
