package com.group6.BancoAlimentos.stock.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "stock")
@Audited
@AuditTable(value = "stock_aud")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    @Column(name = "cantidad_disponible", nullable = false, precision = 12, scale = 3)
    private BigDecimal cantidadDisponible;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private EstadoStock estado;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion")
    private Instant fechaActualizacion;
}
