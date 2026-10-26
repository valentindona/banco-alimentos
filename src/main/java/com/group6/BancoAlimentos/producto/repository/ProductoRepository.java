package com.group6.BancoAlimentos.producto.repository;

import com.group6.BancoAlimentos.producto.modelo.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
}
