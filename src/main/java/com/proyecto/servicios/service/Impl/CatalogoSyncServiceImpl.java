package com.proyecto.servicios.service.Impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.model.CatalogoProductoCache;
import com.proyecto.servicios.model.CatalogoSyncResult;
import com.proyecto.servicios.model.gestopago.GestoPagoProducto;
import com.proyecto.servicios.service.CatalogoSyncService;
import com.proyecto.servicios.service.GestoPagoTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class CatalogoSyncServiceImpl implements CatalogoSyncService {

    private static final Set<String> TIPOS_REFERENCIA = Set.of("a", "b", "c", "ab", "bc");

    private final GestoPagoTokenService gestoPagoTokenService;
    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final String schema;
    private final String redisClave;

    public CatalogoSyncServiceImpl(GestoPagoTokenService gestoPagoTokenService,
                                   JdbcTemplate jdbcTemplate,
                                   StringRedisTemplate redisTemplate,
                                   ObjectMapper objectMapper,
                                   @Value("${spring.flyway.schemas}") String schema,
                                   @Value("${catalogo.redis.key}") String redisClave) {
        this.gestoPagoTokenService = gestoPagoTokenService;
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.schema = schema;
        this.redisClave = redisClave;
    }

    @Override
    public CatalogoSyncResult sincronizarCatalogo() {
        OffsetDateTime inicio = OffsetDateTime.now();
        CatalogoSyncResult resultado = new CatalogoSyncResult();
        resultado.setInicio(inicio);
        log.info("Inicio sincronizacion manual de catalogo ({})", inicio);

        List<GestoPagoProducto> api = gestoPagoTokenService.obtenerProductos();
        List<String> erroresDetalle = new ArrayList<>();
        List<CatalogoProductoCache> validos = new ArrayList<>();
        int recibidos = api == null ? 0 : api.size();
        resultado.setRecibidos(recibidos);
        log.info("Recibidos desde API GestoPago: {}", recibidos);

        for (GestoPagoProducto p : api) {
            try {
                validos.add(mapper(p));
            } catch (IllegalArgumentException e) {
                erroresDetalle.add(p.getIdServicio() + "/" + p.getIdProducto() + ": " + e.getMessage());
            }
        }
        resultado.setErrores(erroresDetalle.size());
        resultado.setErroresDetalle(erroresDetalle);
        log.info("Productos validos: {}, errores: {}", validos.size(), erroresDetalle.size());

        if (validos.isEmpty()) {
            resultado.setEstado("FALLIDA");
            resultado.setMensaje("No hay productos validos para publicar");
            log.warn("Sincronizacion terminada sin productos validos");
            return resultado;
        }

        String checksum = checksum(validos);
        resultado.setChecksum(checksum);

        Integer nuevoVersion = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(version), 0) + 1 FROM " + schema + ".catalogo_versiones", Integer.class);
        resultado.setVersion(nuevoVersion);
        log.info("Nueva version de catalogo: {}", nuevoVersion);

        Long versionId = jdbcTemplate.queryForObject(
                "INSERT INTO " + schema + ".catalogo_versiones "
                        + "(version, estado, checksum, fecha_inicio) "
                        + "VALUES (?, 'EN_CARGA', ?, NOW()) RETURNING id",
                Long.class, nuevoVersion, checksum);
        resultado.setVersionId(versionId);

        jdbcTemplate.update("DELETE FROM " + schema + ".productos_staging");
        publicarStaging(validos);
        log.info("Staging poblado con {} filas", validos.size());

        jdbcTemplate.update(
                "UPDATE " + schema + ".catalogo_versiones SET estado='ARCHIVADA', fecha_fin=NOW() "
                        + "WHERE estado='ACTIVA' AND id <> ?", versionId);
        jdbcTemplate.update(
                "UPDATE " + schema + ".catalogo_versiones SET estado='ACTIVA', fecha_fin=NULL WHERE id=?", versionId);

        int publicados = jdbcTemplate.update(
                "INSERT INTO " + schema + ".productos "
                        + "(version_id, id_servicio, id_producto, servicio, producto, id_cat_tipo_servicio, "
                        + "tipo_front, tipo_referencia, precio, legend, has_digito_verificador, show_ayuda, activo) "
                        + "SELECT " + versionId + ", id_servicio, id_producto, servicio, producto, id_cat_tipo_servicio, "
                        + "tipo_front, tipo_referencia, precio, legend, has_digito_verificador, show_ayuda, TRUE "
                        + "FROM " + schema + ".productos_staging");
        resultado.setPublicados(publicados);
        log.info("Productos publicados en BD: {}", publicados);

        resultado.setEstado(publicados > 0 ? "EXITOSA" : "FALLIDA");
        resultado.setFin(OffsetDateTime.now());
        resultado.setDuracionMs(resultado.getFin().toInstant().toEpochMilli()
                - inicio.toInstant().toEpochMilli());
        resultado.setMensaje("Sincronizacion " + resultado.getEstado() + " (version " + nuevoVersion + ")");
        log.info("Resultado BD: {}, duracion {} ms", resultado.getEstado(), resultado.getDuracionMs());

        jdbcTemplate.update(
                "INSERT INTO " + schema + ".catalogo_sincronizaciones "
                        + "(version_id, estado, inicio, fin, productos_recibidos, productos_publicados, "
                        + "errores, checksum, mensaje) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                versionId, resultado.getEstado(), inicio, resultado.getFin(),
                recibidos, publicados, erroresDetalle.size(), checksum, resultado.getMensaje());

        guardarEnRedis(validos, resultado);
        return resultado;
    }

    @Override
    public List<CatalogoProductoCache> obtenerProductosActivos() {
        try {
            String json = redisTemplate.opsForValue().get(redisClave);
            if (json != null && !json.isBlank()) {
                log.info("Catalogo servido desde Redis (clave {})", redisClave);
                return objectMapper.readValue(json, new TypeReference<List<CatalogoProductoCache>>() {
                });
            }
        } catch (Exception e) {
            log.warn("No se pudo leer catalogo desde Redis: {}", e.getMessage());
        }
        RowMapper<CatalogoProductoCache> rm = (rs, n) -> {
            CatalogoProductoCache c = new CatalogoProductoCache();
            c.setIdServicio(rs.getInt("id_servicio"));
            c.setIdProducto(rs.getInt("id_producto"));
            c.setServicio(rs.getString("servicio"));
            c.setProducto(rs.getString("producto"));
            c.setIdCatTipoServicio(rs.getInt("id_cat_tipo_servicio"));
            c.setTipoFront(rs.getShort("tipo_front"));
            c.setTipoReferencia(rs.getString("tipo_referencia"));
            c.setPrecio(rs.getBigDecimal("precio"));
            c.setLegend(rs.getString("legend"));
            c.setHasDigitoVerificador(rs.getBoolean("has_digito_verificador"));
            c.setShowAyuda(rs.getBoolean("show_ayuda"));
            return c;
        };
        List<CatalogoProductoCache> enBd = jdbcTemplate.query(
                "SELECT id_servicio, id_producto, servicio, producto, id_cat_tipo_servicio, tipo_front, "
                        + "tipo_referencia, precio, legend, has_digito_verificador, show_ayuda "
                        + "FROM v_catalogo_activo ORDER BY id_servicio, id_producto", rm);
        log.info("Catalogo servido desde BD ({} filas)", enBd.size());
        return enBd;
    }

    private void publicarStaging(List<CatalogoProductoCache> productos) {
        jdbcTemplate.batchUpdate(
                "INSERT INTO " + schema + ".productos_staging "
                        + "(id_servicio, id_producto, servicio, producto, id_cat_tipo_servicio, "
                        + "tipo_front, tipo_referencia, precio, legend, has_digito_verificador, show_ayuda) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                productos,
                productos.size(),
                (ps, p) -> {
                    ps.setInt(1, p.getIdServicio());
                    ps.setInt(2, p.getIdProducto());
                    ps.setString(3, p.getServicio());
                    ps.setString(4, p.getProducto());
                    ps.setInt(5, p.getIdCatTipoServicio());
                    ps.setShort(6, p.getTipoFront());
                    ps.setString(7, p.getTipoReferencia());
                    ps.setBigDecimal(8, p.getPrecio());
                    ps.setString(9, p.getLegend());
                    ps.setBoolean(10, Boolean.TRUE.equals(p.getHasDigitoVerificador()));
                    ps.setBoolean(11, Boolean.TRUE.equals(p.getShowAyuda()));
                });
    }

    private void guardarEnRedis(List<CatalogoProductoCache> productos, CatalogoSyncResult resultado) {
        try {
            String json = objectMapper.writeValueAsString(productos);
            redisTemplate.opsForValue().set(redisClave, json);
            resultado.setRedisCacheado(true);
            resultado.setRedisClave(redisClave);
            log.info("Catalogo cacheado en Redis (clave {}, {} bytes)", redisClave, json.getBytes(StandardCharsets.UTF_8).length);
        } catch (Exception e) {
            resultado.setRedisCacheado(false);
            log.warn("No se pudo cachear catalogo en Redis: {}", e.getMessage());
        }
    }

    private CatalogoProductoCache mapper(GestoPagoProducto p) {
        Integer idServicio = entero(p.getIdServicio());
        Integer idProducto = entero(p.getIdProducto());
        if (idServicio == null || idProducto == null) {
            throw new IllegalArgumentException("idServicio o idProducto invalidos");
        }
        BigDecimal precio = decimal(p.getPrecio());
        if (precio == null) {
            throw new IllegalArgumentException("precio invalido");
        }
        CatalogoProductoCache c = new CatalogoProductoCache();
        c.setIdServicio(idServicio);
        c.setIdProducto(idProducto);
        c.setServicio(trim(p.getServicio()));
        c.setProducto(trim(p.getDescripcion()));
        c.setIdCatTipoServicio(entero(p.getIdCatTipoServicio()));
        Short tipoFront = corto(p.getTipoFront());
        c.setTipoFront(tipoFront == null ? 0 : tipoFront);
        c.setTipoReferencia(tipoReferencia(p.getTipoReferencia()));
        c.setPrecio(precio);
        c.setLegend(trim(p.getLegend()));
        c.setHasDigitoVerificador(booleano(p.getHasDigitoVerificador()));
        c.setShowAyuda(booleano(p.getShowAyuda()));
        return c;
    }

    private String tipoReferencia(String valor) {
        String v = trim(valor).toLowerCase();
        if (!TIPOS_REFERENCIA.contains(v)) {
            throw new IllegalArgumentException("tipoReferencia invalido: " + valor);
        }
        return v;
    }

    private Integer entero(String v) {
        if (v == null || v.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Short corto(String v) {
        Integer i = entero(v);
        return i == null ? null : i.shortValue();
    }

    private BigDecimal decimal(String v) {
        if (v == null || v.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Boolean booleano(String v) {
        if (v == null) {
            return Boolean.FALSE;
        }
        return switch (v.trim().toLowerCase()) {
            case "true", "1", "si", "s", "yes", "verdadero" -> Boolean.TRUE;
            default -> Boolean.FALSE;
        };
    }

    private String trim(String v) {
        return v == null ? null : v.trim();
    }

    private String checksum(List<CatalogoProductoCache> productos) {
        try {
            List<String> claves = productos.stream()
                    .map(p -> p.getIdServicio() + "|" + p.getIdProducto() + "|" + p.getPrecio())
                    .sorted()
                    .toList();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(String.join(";", claves).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return null;
        }
    }
}