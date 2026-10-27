package com.group6.BancoAlimentos.factura.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
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
@Table(name = "facturas")
@Audited
@AuditTable(value = "facturas_aud")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Factura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 80)
    private String numero;

    @Column(name = "institucion_id")
    private Long institucionId;

    @Column(name = "donante_id")
    private Long donanteId;

    @Column(name = "donacion_id")
    private Long donacionId;

    @NotNull
    @Column(nullable = false)
    private LocalDate fecha;

    @NotBlank
    @Column(nullable = false, length = 30)
    private String estado;

    @PositiveOrZero
    @Column(precision = 12, scale = 2)
    private BigDecimal importe;
}
