package com.proyecto.servicios.entity.users;

import jakarta.persistence.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.UUID;

public class UsersEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id_cliente;

    @Column(name = "nombre_cliente", nullable = false)
    private String nombreCliente;

    @Column(name = "apellido_paterno", nullable = false)
    private String apellidoPaterno;

    @Column(name = "apellido_materno", nullable = false)
    private String apellidoMaterno;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaNacimiento;

    @Column(name = "curp", nullable = false)
    private String curp;

    @Enumerated(EnumType.STRING)
    @Column(name = "sexo_usuario", nullable = false)
    private SexoUsuario sexo;

    @Column(name = "nacionalidad", nullable = true)
    private String nacionalidad;

    @Column(name = "estado_civil", nullable = true)
    private String estadoCivil;
}
