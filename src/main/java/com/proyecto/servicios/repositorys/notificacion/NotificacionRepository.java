package com.proyecto.servicios.repositorys.notificacion;

import com.proyecto.servicios.entity.notificacion.EstadoCorreoNotificacion;
import com.proyecto.servicios.entity.notificacion.Notificacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    Page<Notificacion> findByUsuarioIdOrderByCreatedAtDesc(Long usuarioId, Pageable pageable);

    Page<Notificacion> findByUsuarioIdAndLeidaEnIsNullOrderByCreatedAtDesc(Long usuarioId, Pageable pageable);

    Optional<Notificacion> findByIdAndUsuarioId(Long id, Long usuarioId);

    boolean existsByUsuarioIdAndDedupeKey(Long usuarioId, String dedupeKey);

    long countByUsuarioIdAndLeidaEnIsNull(Long usuarioId);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notificacion n SET n.leidaEn = :ahora, n.updatedAt = :ahora " +
           "WHERE n.id = :id AND n.usuarioId = :usuarioId AND n.leidaEn IS NULL")
    int marcarLeida(@Param("id") Long id,
                    @Param("usuarioId") Long usuarioId,
                    @Param("ahora") Instant ahora);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notificacion n SET n.leidaEn = :ahora, n.updatedAt = :ahora " +
           "WHERE n.usuarioId = :usuarioId AND n.leidaEn IS NULL")
    int marcarTodasLeidas(@Param("usuarioId") Long usuarioId, @Param("ahora") Instant ahora);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notificacion n SET n.emailEstado = :enviando, " +
           "n.emailIntentos = n.emailIntentos + 1, n.updatedAt = :ahora " +
           "WHERE n.id = :id AND n.emailEstado IN :estados AND n.emailIntentos < :maxIntentos")
    int reclamarEnvio(@Param("id") Long id,
                      @Param("enviando") EstadoCorreoNotificacion enviando,
                      @Param("estados") Collection<EstadoCorreoNotificacion> estados,
                      @Param("maxIntentos") int maxIntentos,
                      @Param("ahora") Instant ahora);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notificacion n SET n.emailEstado = :estado, n.emailUltimoError = :error, " +
           "n.updatedAt = :ahora WHERE n.id = :id")
    int actualizarEstadoEmail(@Param("id") Long id,
                              @Param("estado") EstadoCorreoNotificacion estado,
                              @Param("error") String error,
                              @Param("ahora") Instant ahora);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notificacion n SET n.emailEstado = :fallido, n.updatedAt = :ahora " +
           "WHERE n.emailEstado = :enviando AND n.updatedAt <= :corte")
    int liberarEnviosAtascados(@Param("enviando") EstadoCorreoNotificacion enviando,
                               @Param("fallido") EstadoCorreoNotificacion fallido,
                               @Param("corte") Instant corte,
                               @Param("ahora") Instant ahora);

    @Query("SELECT n.id FROM Notificacion n " +
           "WHERE n.emailEstado IN :estados AND n.emailIntentos < :maxIntentos " +
           "AND n.updatedAt <= :corte ORDER BY n.createdAt ASC")
    List<Long> buscarPendientesEnvio(@Param("estados") Collection<EstadoCorreoNotificacion> estados,
                                     @Param("maxIntentos") int maxIntentos,
                                     @Param("corte") Instant corte,
                                     Pageable pageable);
}
