package com.group6.BancoAlimentos.producto.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "productos")
@Audited
@AuditTable(value = "productos_aud")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String nombre;

    @Size(max = 1000)
    @Column(length = 1000)
    private String descripcion;

    @NotBlank
    @Size(max = 50)
    @Column(name = "unidad_medida", nullable = false, length = 50)
    private String unidadMedida;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;
}
