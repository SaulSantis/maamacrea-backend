package com.maamacrea.backend.productos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ProductoResponse(
        Long id,
        String codigo,
        String nombre,
        ProductoTipo tipoProducto,
        LocalDateTime fechaRegistro,
        BigDecimal precioVenta,
        BigDecimal costoMateriales,
        BigDecimal costoElectricidad,
        BigDecimal costoDesgasteEquipo,
        BigDecimal totalCostosAdicionales,
        BigDecimal costoTotalProduccion,
        BigDecimal precioSugerido,
        BigDecimal ganancia,
        LocalDate ultimoCambioPrecioInsumos,
        boolean tieneCambiosPrecio,
        boolean costeoCompleto,
        List<String> advertenciasCosteo,
        List<ProductoCambioPrecioResponse> cambiosPrecioInsumos,
        List<ProductoInsumoResponse> insumos) {

    public ProductoResponse(
            Long id,
            String codigo,
            String nombre,
            ProductoTipo tipoProducto,
            LocalDateTime fechaRegistro,
            BigDecimal precioVenta,
            BigDecimal costoMateriales,
            BigDecimal precioSugerido,
            BigDecimal ganancia,
            LocalDate ultimoCambioPrecioInsumos,
            boolean tieneCambiosPrecio,
            boolean costeoCompleto,
            List<String> advertenciasCosteo,
            List<ProductoCambioPrecioResponse> cambiosPrecioInsumos,
            List<ProductoInsumoResponse> insumos) {
        this(
                id,
                codigo,
                nombre,
                tipoProducto,
                fechaRegistro,
                precioVenta,
                costoMateriales,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                costoMateriales,
                precioSugerido,
                ganancia,
                ultimoCambioPrecioInsumos,
                tieneCambiosPrecio,
                costeoCompleto,
                advertenciasCosteo,
                cambiosPrecioInsumos,
                insumos);
    }
}
