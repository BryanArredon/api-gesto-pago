package com.proyecto.servicios.service.security;

import com.proyecto.servicios.entity.auth.Rol;
import com.proyecto.servicios.entity.auth.Usuario;
import com.proyecto.servicios.exception.ApiException;
import com.proyecto.servicios.repositorys.auth.RolRepository;
import com.proyecto.servicios.repositorys.auth.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioIdentidadSsoTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private RolRepository rolRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private ServicioIdentidadSso servicio;

    @BeforeEach
    void setUp() {
        servicio = new ServicioIdentidadSso(usuarioRepository, rolRepository, passwordEncoder);
    }

    @Test
    void devuelveIdSiElUsuarioYaExisteEnGestorPagos() {
        Usuario existente = new Usuario();
        existente.setId(42L);
        existente.setEmail("cliente@banco.mx");
        existente.setActivo(true);
        when(usuarioRepository.findByEmail("cliente@banco.mx")).thenReturn(Optional.of(existente));

        Authentication auth = new UsernamePasswordAuthenticationToken("Cliente@Banco.mx", null, List.of());
        Long id = servicio.obtenerOCrearUsuarioId(auth);

        assertEquals(42L, id);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void autoAprovisionaPerfilLocalCuandoElUsuarioVieneDeOnboardingClientes() {
        when(usuarioRepository.findByEmail("nuevo@banco.mx")).thenReturn(Optional.empty());
        Rol rolCliente = new Rol();
        rolCliente.setNombre("CLIENTE");
        when(rolRepository.findByNombre("CLIENTE")).thenReturn(Optional.of(rolCliente));
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(99L);
            return u;
        });

        Authentication auth = new UsernamePasswordAuthenticationToken("nuevo@banco.mx", null, List.of());
        Long id = servicio.obtenerOCrearUsuarioId(auth);

        assertEquals(99L, id);
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void rechazaUsuarioLocalInactivo() {
        Usuario inactivo = new Usuario();
        inactivo.setId(7L);
        inactivo.setEmail("bloqueado@banco.mx");
        inactivo.setActivo(false);
        when(usuarioRepository.findByEmail("bloqueado@banco.mx")).thenReturn(Optional.of(inactivo));

        Authentication auth = new UsernamePasswordAuthenticationToken("bloqueado@banco.mx", null, List.of());
        ApiException ex = assertThrows(ApiException.class, () -> servicio.obtenerOCrearUsuarioId(auth));
        assertEquals("AUTH-002", ex.getCode());
    }
}
