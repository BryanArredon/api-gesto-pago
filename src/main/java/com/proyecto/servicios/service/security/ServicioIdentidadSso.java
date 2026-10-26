package com.proyecto.servicios.service.security;

import com.proyecto.servicios.entity.auth.Rol;
import com.proyecto.servicios.entity.auth.Usuario;
import com.proyecto.servicios.exception.ApiException;
import com.proyecto.servicios.repositorys.auth.RolRepository;
import com.proyecto.servicios.repositorys.auth.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Resuelve la identidad local en {@code api-gesto-pago} a partir de un JWT emitido por
 * {@code onboarding-clientes} (fuente unica de identidad bancaria / SSO).
 *
 * <p>Si el cliente ya completo su onboarding bancario e inicio sesion en {@code onboarding-clientes},
 * la primera vez que invoque un endpoint de {@code api-gesto-pago} se auto-aprovisiona (JIT) su
 * registro espejo en {@code gestor_pagos.usuarios} para mantener la integridad referencial de
 * {@code transacciones.usuario_id} sin obligar al usuario a registrarse dos veces.
 */
@Service
@Slf4j
public class ServicioIdentidadSso {

    private static final String ROL_CLIENTE = "CLIENTE";

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public ServicioIdentidadSso(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Long obtenerOCrearUsuarioId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw ApiException.noAutenticado();
        }
        String email = authentication.getName().trim().toLowerCase(Locale.ROOT);
        Optional<Usuario> existente = usuarioRepository.findByEmail(email);
        if (existente.isPresent()) {
            Usuario usuario = existente.get();
            if (!Boolean.TRUE.equals(usuario.getActivo())) {
                throw ApiException.cuentaInactiva();
            }
            return usuario.getId();
        }

        try {
            Usuario nuevo = new Usuario();
            nuevo.setEmail(email);
            nuevo.setNombre(derivarNombreVisible(email));
            nuevo.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
            nuevo.setActivo(true);
            rolRepository.findByNombre(ROL_CLIENTE)
                    .ifPresent(rol -> nuevo.setRoles(Set.of(rol)));
            Usuario guardado = usuarioRepository.save(nuevo);
            log.info("Perfil local SSO auto-aprovisionado en api-gesto-pago para email={}", email);
            return guardado.getId();
        } catch (DataIntegrityViolationException ex) {
            return usuarioRepository.findByEmail(email)
                    .map(Usuario::getId)
                    .orElseThrow(ApiException::noAutenticado);
        }
    }

    private static String derivarNombreVisible(String email) {
        int arroba = email.indexOf('@');
        if (arroba > 0) {
            return email.substring(0, arroba);
        }
        return email;
    }
}
