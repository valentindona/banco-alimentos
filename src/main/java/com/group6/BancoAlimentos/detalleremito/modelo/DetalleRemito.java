package com.group6.BancoAlimentos.detalleremito.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
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
@Table(name = "detalles_remito", uniqueConstraints = {
        @UniqueConstraint(name = "uq_detalle_remito_stock", columnNames = {"remito_id", "stock_id"})
})
@Audited
@AuditTable(value = "detalles_remito_aud")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetalleRemito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "remito_id", nullable = false)
    private Long remitoId;

    @NotNull
    @Column(name = "stock_id", nullable = false)
    private Long stockId;

    @NotNull
    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @NotNull
    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @NotBlank
    @Column(name = "producto_nombre", nullable = false, length = 120)
    private String productoNombre;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal cantidad;
}
