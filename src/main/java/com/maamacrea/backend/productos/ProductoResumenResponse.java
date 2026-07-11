package com.maamacrea.backend.productos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductoResumenResponse(
        Long id,
        String codigo,
        String nombre,
        ProductoTipo tipoProducto,
        BigDecimal precioVenta,
        int cantidadInsumos,
        LocalDateTime fechaRegistro) {}
