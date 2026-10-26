package com.group6.BancoAlimentos.usuario.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(name = "nombre_usuario", nullable = false, unique = true, length = 100)
    private String nombreUsuario;

    @NotBlank
    @Column(name = "clave_hash", nullable = false, length = 255)
    private String claveHash;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private RolUsuario rol;

    @Column(name = "institucion_id")
    private Long institucionId;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @Transient
    @AssertTrue(message = "La institución debe corresponder al rol del usuario.")
    public boolean isInstitucionConsistente() {
        if (rol == null) {
            return true;
        }
        return rol == RolUsuario.USER_INSTITUCIONAL ? institucionId != null : institucionId == null;
    }

    @PrePersist
    void asignarFechaCreacion() {
        if (creadoEn == null) {
            creadoEn = Instant.now();
        }
    }
}
