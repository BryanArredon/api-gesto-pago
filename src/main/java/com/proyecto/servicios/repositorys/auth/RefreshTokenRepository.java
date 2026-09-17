package com.proyecto.servicios.repositorys.auth;

import com.proyecto.servicios.entity.auth.RefreshToken;
import com.proyecto.servicios.entity.auth.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Transactional
    @Query("update RefreshToken rt set rt.revocado = true where rt.usuario = :usuario")
    void revocarTodosDe(@Param("usuario") Usuario usuario);

    @Modifying
    @Transactional
    long deleteByExpiresAtBefore(Instant expiresBefore);
}