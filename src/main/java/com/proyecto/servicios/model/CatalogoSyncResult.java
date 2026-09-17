package com.proyecto.servicios.model;

import java.time.OffsetDateTime;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CatalogoSyncResult {

    private Long versionId;
    private Integer version;
    private String estado;
    private Integer recibidos;
    private Integer publicados;
    private Integer errores;
    private String checksum;
    private OffsetDateTime inicio;
    private OffsetDateTime fin;
    private Long duracionMs;
    private boolean redisCacheado;
    private String redisClave;
    private String mensaje;
    private List<String> erroresDetalle;
}