package com.onboarding.clientes.cliente.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Entidad JPA que mapea la tabla cliente. */
@Entity
@Table(name = "cliente")
public class ClienteEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "nombre", length = 50, nullable = false)
    private String nombre;

    @Column(name = "segundo_nombre", length = 50)
    private String segundoNombre;

    @Column(name = "apellido_paterno", length = 50, nullable = false)
    private String apellidoPaterno;

    @Column(name = "apellido_materno", length = 50, nullable = false)
    private String apellidoMaterno;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Column(name = "curp", length = 18, nullable = false, unique = true, updatable = false)
    private String curp;

    @Column(name = "rfc", length = 13, nullable = false, unique = true, updatable = false)
    private String rfc;

    @Column(name = "sexo", length = 10, nullable = false)
    private String sexo;

    @Column(name = "nacionalidad", length = 50, nullable = false)
    private String nacionalidad;

    @Column(name = "estado_civil", length = 20, nullable = false)
    private String estadoCivil;

    @Column(name = "correo_electronico", length = 100, nullable = false)
    private String correoElectronico;

    @Column(name = "telefono_movil", length = 10, nullable = false)
    private String telefonoMovil;

    @Column(name = "telefono_alterno", length = 10)
    private String telefonoAlterno;

    @Column(name = "ocupacion", length = 100, nullable = false)
    private String ocupacion;

    @Column(name = "empresa", length = 100)
    private String empresa;

    @Column(name = "ingreso_mensual", precision = 18, scale = 2, nullable = false)
    private BigDecimal ingresoMensual;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    @Column(name = "fecha_baja")
    private Instant fechaBaja;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ClienteEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getSegundoNombre() { return segundoNombre; }
    public void setSegundoNombre(String segundoNombre) { this.segundoNombre = segundoNombre; }
    public String getApellidoPaterno() { return apellidoPaterno; }
    public void setApellidoPaterno(String apellidoPaterno) { this.apellidoPaterno = apellidoPaterno; }
    public String getApellidoMaterno() { return apellidoMaterno; }
    public void setApellidoMaterno(String apellidoMaterno) { this.apellidoMaterno = apellidoMaterno; }
    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }
    public String getCurp() { return curp; }
    public void setCurp(String curp) { this.curp = curp; }
    public String getRfc() { return rfc; }
    public void setRfc(String rfc) { this.rfc = rfc; }
    public String getSexo() { return sexo; }
    public void setSexo(String sexo) { this.sexo = sexo; }
    public String getNacionalidad() { return nacionalidad; }
    public void setNacionalidad(String nacionalidad) { this.nacionalidad = nacionalidad; }
    public String getEstadoCivil() { return estadoCivil; }
    public void setEstadoCivil(String estadoCivil) { this.estadoCivil = estadoCivil; }
    public String getCorreoElectronico() { return correoElectronico; }
    public void setCorreoElectronico(String correoElectronico) { this.correoElectronico = correoElectronico; }
    public String getTelefonoMovil() { return telefonoMovil; }
    public void setTelefonoMovil(String telefonoMovil) { this.telefonoMovil = telefonoMovil; }
    public String getTelefonoAlterno() { return telefonoAlterno; }
    public void setTelefonoAlterno(String telefonoAlterno) { this.telefonoAlterno = telefonoAlterno; }
    public String getOcupacion() { return ocupacion; }
    public void setOcupacion(String ocupacion) { this.ocupacion = ocupacion; }
    public String getEmpresa() { return empresa; }
    public void setEmpresa(String empresa) { this.empresa = empresa; }
    public BigDecimal getIngresoMensual() { return ingresoMensual; }
    public void setIngresoMensual(BigDecimal ingresoMensual) { this.ingresoMensual = ingresoMensual; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public Instant getFechaBaja() { return fechaBaja; }
    public void setFechaBaja(Instant fechaBaja) { this.fechaBaja = fechaBaja; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
