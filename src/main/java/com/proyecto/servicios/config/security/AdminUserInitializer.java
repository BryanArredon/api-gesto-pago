package com.proyecto.servicios.config.security;

import com.proyecto.servicios.entity.auth.Rol;
import com.proyecto.servicios.entity.auth.Usuario;
import com.proyecto.servicios.repositorys.auth.RolRepository;
import com.proyecto.servicios.repositorys.auth.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Set;

@Component
@Slf4j
public class AdminUserInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${security.admin.email:}")
    private String adminEmail;

    @Value("${security.admin.password:}")
    private String adminPassword;

    @Value("${security.admin.nombre:Administrador}")
    private String adminNombre;

    public AdminUserInitializer(UsuarioRepository usuarioRepository,
                                RolRepository rolRepository,
                                PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!StringUtils.hasText(adminEmail) || !StringUtils.hasText(adminPassword)) {
            log.warn("ADMIN_EMAIL/ADMIN_PASSWORD no configurados, no se creara administrador");
            return;
        }

        String email = adminEmail.trim().toLowerCase();
        if (usuarioRepository.findByEmail(email).isPresent()) {
            log.info("Administrador ya existe, se omite creacion: {}", email);
            return;
        }

        Rol admin = rolRepository.findByNombre("ADMIN")
                .orElseThrow(() -> new IllegalStateException(
                        "No existe el rol ADMIN. Revisar la migracion R__seed_roles.sql"));
        Rol cliente = rolRepository.findByNombre("CLIENTE")
                .orElseThrow(() -> new IllegalStateException(
                        "No existe el rol CLIENTE. Revisar la migracion R__seed_roles.sql"));

        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setPasswordHash(passwordEncoder.encode(adminPassword));
        usuario.setNombre(adminNombre);
        usuario.setActivo(true);
        usuario.setRoles(Set.of(admin, cliente));
        usuarioRepository.save(usuario);
        log.info("Administrador creado: {}", email);
    }
}