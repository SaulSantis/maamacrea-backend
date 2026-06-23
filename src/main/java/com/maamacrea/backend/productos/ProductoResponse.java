package com.maamacrea.backend.productos;

import java.math.BigDecimal;
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
        List<ProductoInsumoResponse> insumos) {}
