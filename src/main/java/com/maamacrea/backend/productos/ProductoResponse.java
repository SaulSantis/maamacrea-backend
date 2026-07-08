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
        BigDecimal precioSugerido,
        BigDecimal ganancia,
        LocalDate ultimoCambioPrecioInsumos,
        boolean tieneCambiosPrecio,
        List<ProductoCambioPrecioResponse> cambiosPrecioInsumos,
        List<ProductoInsumoResponse> insumos) {}
