package com.group6.BancoAlimentos.donacion.repository;

import com.group6.BancoAlimentos.donacion.modelo.Donacion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DonacionRepository extends JpaRepository<Donacion, Long> {}
