package com.group6.BancoAlimentos.donacion.service;

import com.group6.BancoAlimentos.Common.excepcion.ReglaNegocioException;
import com.group6.BancoAlimentos.donacion.dto.DonacionRespuesta;
import com.group6.BancoAlimentos.donacion.dto.DonacionSolicitud;
import com.group6.BancoAlimentos.donacion.mapper.DonacionMapper;
import com.group6.BancoAlimentos.donacion.modelo.Donacion;
import com.group6.BancoAlimentos.donacion.repository.DonacionRepository;
import com.group6.BancoAlimentos.donante.modelo.Donante;
import com.group6.BancoAlimentos.donante.service.DonanteService;
import com.group6.BancoAlimentos.factura.modelo.Factura;
import com.group6.BancoAlimentos.factura.service.FacturaService;
import com.group6.BancoAlimentos.itemdonacion.modelo.ItemDonacion;
import com.group6.BancoAlimentos.itemdonacion.dto.ItemDonacionSolicitud;
import com.group6.BancoAlimentos.itemdonacion.service.ItemDonacionService;
import com.group6.BancoAlimentos.producto.dto.ProductoSolicitud;
import com.group6.BancoAlimentos.producto.modelo.Producto;
import com.group6.BancoAlimentos.producto.service.ProductoService;
import com.group6.BancoAlimentos.stock.service.StockService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DonacionService {

    private final DonacionRepository donacionRepository;
    private final DonanteService donanteService;
    private final ProductoService productoService;
    private final ItemDonacionService itemDonacionService;
    private final StockService stockService;
    private final FacturaService facturaService;
    private final DonacionMapper donacionMapper;

    public DonacionService(
            DonacionRepository donacionRepository,
            DonanteService donanteService,
            ProductoService productoService,
            ItemDonacionService itemDonacionService,
            StockService stockService,
            FacturaService facturaService,
            DonacionMapper donacionMapper) {
        this.donacionRepository = donacionRepository;
        this.donanteService = donanteService;
        this.productoService = productoService;
        this.itemDonacionService = itemDonacionService;
        this.stockService = stockService;
        this.facturaService = facturaService;
        this.donacionMapper = donacionMapper;
    }

    @Transactional
    public DonacionRespuesta confirmar(DonacionSolicitud solicitud) {
        validarSolicitud(solicitud);
        Donante donante = donanteService.obtenerModeloActivo(solicitud.donanteId());
        List<LineaPreparada> lineas = solicitud.items().stream().map(this::prepararLinea).toList();

        Donacion donacion = donacionRepository.save(Donacion.builder()
                .donante(donante)
                .fecha(solicitud.fecha())
                .build());
        List<ItemDonacion> items = new ArrayList<>();
        for (LineaPreparada linea : lineas) {
            ItemDonacion item = itemDonacionService.crear(
                    donacion,
                    linea.producto(),
                    linea.nombreSnapshot(),
                    linea.tipoAlimentoSnapshot(),
                    linea.unidadMedidaSnapshot(),
                    linea.cantidad(),
                    linea.fechaVencimiento());
            items.add(item);
            stockService.ingresar(linea.producto().getId(), linea.fechaVencimiento(), linea.cantidad());
        }
        Factura factura = facturaService.emitir(donante.getId(), donacion.getId(), donacion.getFecha());
        return donacionMapper.aRespuesta(donacion, items, factura);
    }

    private LineaPreparada prepararLinea(ItemDonacionSolicitud solicitud) {
        Producto producto;
        String nombre;
        String tipo;
        String unidad;
        if (solicitud.productoId() == null) {
            producto = productoService.crearParaDonacion(new ProductoSolicitud(
                    solicitud.nombreProducto().trim(),
                    null,
                    solicitud.tipoAlimento().trim(),
                    solicitud.unidadMedida().trim()));
            nombre = producto.getNombre();
            tipo = producto.getTipoAlimento();
            unidad = producto.getUnidadMedida();
        } else {
            producto = productoService.buscarModeloActivo(solicitud.productoId());
            nombre = valorOBase(solicitud.nombreProducto(), producto.getNombre());
            tipo = valorOBase(solicitud.tipoAlimento(), producto.getTipoAlimento());
            unidad = valorOBase(solicitud.unidadMedida(), producto.getUnidadMedida());
            if (tipo == null || tipo.isBlank() || unidad == null || unidad.isBlank()) {
                throw new ReglaNegocioException(
                        "El producto histórico debe clasificarse con tipo y unidad antes de recibirlo");
            }
        }
        return new LineaPreparada(
                producto,
                nombre,
                tipo,
                unidad,
                solicitud.cantidad().setScale(3),
                solicitud.fechaVencimiento());
    }

    private void validarSolicitud(DonacionSolicitud solicitud) {
        if (solicitud == null || solicitud.donanteId() == null || solicitud.fecha() == null) {
            throw new ReglaNegocioException("Donante y fecha de recepción son obligatorios");
        }
        if (solicitud.items() == null || solicitud.items().isEmpty()) {
            throw new ReglaNegocioException("La donación debe incluir al menos un producto");
        }
        solicitud.items().forEach(item -> validarLinea(item, solicitud.fecha()));
    }

    private void validarLinea(ItemDonacionSolicitud item, LocalDate fechaDonacion) {
        if (item == null || item.cantidad() == null || item.cantidad().signum() <= 0
                || item.cantidad().scale() > 3) {
            throw new ReglaNegocioException("La cantidad debe ser positiva y tener hasta tres decimales");
        }
        if (item.fechaVencimiento() == null
                || !item.fechaVencimiento().isAfter(fechaDonacion)
                || !item.fechaVencimiento().isAfter(LocalDate.now())) {
            throw new ReglaNegocioException("El vencimiento debe ser posterior a la recepción y a hoy");
        }
        if (item.productoId() == null && (vacio(item.nombreProducto())
                || vacio(item.tipoAlimento()) || vacio(item.unidadMedida()))) {
            throw new ReglaNegocioException("Un producto manual requiere nombre, tipo y unidad completos");
        }
    }

    private String valorOBase(String solicitado, String base) {
        return vacio(solicitado) ? base : solicitado.trim();
    }

    private boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }

    private record LineaPreparada(
            Producto producto,
            String nombreSnapshot,
            String tipoAlimentoSnapshot,
            String unidadMedidaSnapshot,
            BigDecimal cantidad,
            LocalDate fechaVencimiento) {}
}
