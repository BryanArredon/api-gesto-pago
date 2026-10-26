package com.proyecto.servicios.service.impl;

import com.proyecto.servicios.model.CatalogoProductoCache;
import com.proyecto.servicios.service.CatalogoConsulta;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class CatalogoConsultaImpl implements CatalogoConsulta {

    private static final String COLUMNAS_PRODUCTO =
            "id_servicio, id_producto, servicio, producto, id_cat_tipo_servicio, "
                    + "tipo_front, tipo_referencia, precio, legend, has_digito_verificador, show_ayuda";

    private static final RowMapper<CatalogoProductoCache> MAPPER = (rs, n) -> {
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

    private final JdbcTemplate jdbcTemplate;
    private final String schema;

    @Autowired
    public CatalogoConsultaImpl(
            JdbcTemplate jdbcTemplate,
            @Value("${spring.flyway.schemas:gestor_pagos}") String schema) {
        this.jdbcTemplate = jdbcTemplate;
        this.schema = schema;
    }

    public CatalogoConsultaImpl(JdbcTemplate jdbcTemplate) {
        this(jdbcTemplate, "gestor_pagos");
    }

    @Override
    public Optional<CatalogoProductoCache> buscarActivo(Integer idServicio, Integer idProducto) {
        Optional<CatalogoProductoCache> enVistaEsquema = consultar(
                "SELECT " + COLUMNAS_PRODUCTO + " FROM " + schema
                        + ".v_catalogo_activo WHERE id_servicio = ? AND id_producto = ?",
                idServicio, idProducto);
        if (enVistaEsquema.isPresent()) {
            return enVistaEsquema;
        }

        Optional<CatalogoProductoCache> enTablaEsquema = consultar(
                "SELECT " + COLUMNAS_PRODUCTO + " FROM " + schema
                        + ".productos WHERE id_servicio = ? AND id_producto = ? "
                        + "AND activo = TRUE AND deleted_at IS NULL ORDER BY version_id DESC LIMIT 1",
                idServicio, idProducto);
        if (enTablaEsquema.isPresent()) {
            return enTablaEsquema;
        }

        Optional<CatalogoProductoCache> enVistaDefault = consultar(
                "SELECT " + COLUMNAS_PRODUCTO
                        + " FROM public.v_catalogo_activo WHERE id_servicio = ? AND id_producto = ?",
                idServicio, idProducto);
        if (enVistaDefault.isPresent()) {
            return enVistaDefault;
        }

        return consultar(
                "SELECT " + COLUMNAS_PRODUCTO
                        + " FROM public.productos WHERE id_servicio = ? AND id_producto = ? "
                        + "AND activo = TRUE AND deleted_at IS NULL ORDER BY version_id DESC LIMIT 1",
                idServicio, idProducto);
    }

    private Optional<CatalogoProductoCache> consultar(String sql, Integer idServicio, Integer idProducto) {
        try {
            return jdbcTemplate.query(sql, MAPPER, idServicio, idProducto).stream().findFirst();
        } catch (DataAccessException ex) {
            log.debug("Consulta de catalogo omitida ({}): {}", sql, ex.getMessage());
            return Optional.empty();
        }
    }
}