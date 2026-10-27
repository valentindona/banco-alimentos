package com.group6.BancoAlimentos.itemdonacion.modelo;

import com.group6.BancoAlimentos.donacion.modelo.Donacion;
import com.group6.BancoAlimentos.producto.modelo.Producto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "items_donacion")
@Audited
@AuditTable(value = "items_donacion_aud")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemDonacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "donacion_id", nullable = false)
    private Donacion donacion;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @NotBlank
    @Column(name = "producto_nombre", nullable = false, length = 120)
    private String productoNombre;

    @NotBlank
    @Column(name = "tipo_alimento", nullable = false, length = 80)
    private String tipoAlimento;

    @NotBlank
    @Column(name = "unidad_medida", nullable = false, length = 50)
    private String unidadMedida;

    @NotNull
    @DecimalMin(value = "0", inclusive = false)
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal cantidad;

    @NotNull
    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;
}
