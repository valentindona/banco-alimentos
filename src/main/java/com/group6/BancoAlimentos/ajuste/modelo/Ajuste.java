package com.group6.BancoAlimentos.ajuste.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "ajustes_stock")
@Audited
@AuditTable(value = "ajustes_stock_aud")
public class Ajuste {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_id", nullable = false, updatable = false)
    private Long stockId;

    @Column(name = "producto_id", nullable = false, updatable = false)
    private Long productoId;

    @Column(name = "fecha_vencimiento", updatable = false)
    private LocalDate fechaVencimiento;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private Long usuarioId;

    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private Instant fechaHora;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private TipoAjuste tipo;

    @Column(name = "cantidad_anterior", nullable = false, precision = 12, scale = 3)
    private BigDecimal cantidadAnterior;

    @Column(name = "cantidad_nueva", nullable = false, precision = 12, scale = 3)
    private BigDecimal cantidadNueva;

    @Column(nullable = false, columnDefinition = "text")
    private String motivo;

    protected Ajuste() {
    }

    public Ajuste(
            Long stockId,
            Long productoId,
            LocalDate fechaVencimiento,
            Long usuarioId,
            Instant fechaHora,
            TipoAjuste tipo,
            BigDecimal cantidadAnterior,
            BigDecimal cantidadNueva,
            String motivo) {
        this.stockId = stockId;
        this.productoId = productoId;
        this.fechaVencimiento = fechaVencimiento;
        this.usuarioId = usuarioId;
        this.fechaHora = fechaHora;
        this.tipo = tipo;
        this.cantidadAnterior = cantidadAnterior;
        this.cantidadNueva = cantidadNueva;
        this.motivo = motivo;
    }

    public Long getId() {
        return id;
    }

    public Long getStockId() {
        return stockId;
    }

    public Long getProductoId() {
        return productoId;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public Instant getFechaHora() {
        return fechaHora;
    }

    public TipoAjuste getTipo() {
        return tipo;
    }

    public BigDecimal getCantidadAnterior() {
        return cantidadAnterior;
    }

    public BigDecimal getCantidadNueva() {
        return cantidadNueva;
    }

    public String getMotivo() {
        return motivo;
    }

    public void actualizar(TipoAjuste tipo, BigDecimal cantidadAnterior, BigDecimal cantidadNueva, String motivo) {
        this.tipo = tipo;
        this.cantidadAnterior = cantidadAnterior;
        this.cantidadNueva = cantidadNueva;
        this.motivo = motivo;
    }
}
