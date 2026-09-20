package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.model.CatalogoProductoCache;
import com.proyecto.servicios.service.CatalogoConsulta;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CatalogoConsultaImpl implements CatalogoConsulta {

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

    public CatalogoConsultaImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<CatalogoProductoCache> buscarActivo(Integer idServicio, Integer idProducto) {
        return jdbcTemplate.query(
                "SELECT id_servicio, id_producto, servicio, producto, id_cat_tipo_servicio, "
                        + "tipo_front, tipo_referencia, precio, legend, has_digito_verificador, show_ayuda "
                        + "FROM v_catalogo_activo WHERE id_servicio = ? AND id_producto = ?",
                MAPPER, idServicio, idProducto).stream().findFirst();
    }
}