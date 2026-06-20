package com.maamacrea.backend.insumos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record InsumoResponse(
        Long id,
        String nombre,
        String categoria,
        String categoriaLabel,
        String unidadMedida,
        BigDecimal cantidadComprada,
        BigDecimal precioCompraTotal,
        BigDecimal costoUnitario,
        String proveedor,
        LocalDate fechaCompra,
        String notas,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
