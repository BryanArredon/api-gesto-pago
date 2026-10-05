package com.onboarding.clientes.cliente.application.port;

import com.onboarding.clientes.cliente.domain.model.Usuario;
import com.onboarding.clientes.cliente.domain.valueobject.Correo;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de persistencia de usuarios de acceso.
 *
 * <p>Las consultas por correo ignoran mayusculas porque el dominio normaliza el correo al construir el value
 * object, de modo que la comparacion en base de datos es siempre exacta.
 */
public interface UsuarioRepositorio {

    Usuario guardar(Usuario usuario);

    Optional<Usuario> buscarPorId(UUID id);

    Optional<Usuario> buscarPorCorreo(Correo correo);

    boolean existePorCorreo(Correo correo);

    boolean existePorCliente(UUID clienteId);
}
