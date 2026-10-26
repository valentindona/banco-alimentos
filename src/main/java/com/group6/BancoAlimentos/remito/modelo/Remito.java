package com.group6.BancoAlimentos.remito.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "remitos")
@Audited
@AuditTable(value = "remitos_aud")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Remito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "institucion_id", nullable = false)
    private Long institucionId;

    @NotBlank
    @Column(name = "institucion_nombre", nullable = false, length = 150)
    private String institucionNombre;

    @NotBlank
    @Column(name = "institucion_domicilio", nullable = false, length = 250)
    private String institucionDomicilio;

    @NotNull
    @Column(name = "fecha_entrega", nullable = false)
    private LocalDate fechaEntrega;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private EstadoRemito estado;

    @NotNull
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    @Column(name = "fecha_confirmacion")
    private Instant fechaConfirmacion;

    @NotNull
    @Column(name = "creado_por_usuario_id", nullable = false)
    private Long creadoPorUsuarioId;

    @PrePersist
    void asignarFechaCreacion() {
        if (fechaCreacion == null) {
            fechaCreacion = Instant.now();
        }
    }
}
