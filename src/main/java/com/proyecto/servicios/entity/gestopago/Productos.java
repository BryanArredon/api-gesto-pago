package com.proyecto.servicios.entity.gestopago;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.EnableMBeanExport;

@Entity
@Getter
@Setter
@Table(name = "productos")
public class Productos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "producto", nullable = false)
    private String producto;

    @Column(name = "servicio", nullable = false)
    private String servicio;

    @Column(name = "id_prodcuto", nullable = false)
    private Integer idProducto;

    @Column(name = "id_servicio", nullable = false)
    private Integer idServicio;

    @Column(name = "id_cat_tipo_servicio", nullable = false)
    private Integer idCatTipoServicio;

    @Column(name = "tipo_front", nullable = false)
    private Integer tipoFront;

}
