package com.maamacrea.backend.insumos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record InsumoResponse(
        Long id,
        String codigoProducto,
        String nombre,
        String categoria,
        String categoriaLabel,
        String unidadMedida,
        BigDecimal cantidadComprada,
        BigDecimal ancho,
        BigDecimal alto,
        BigDecimal precioNeto,
        BigDecimal iva,
        BigDecimal precioCompraTotal,
        BigDecimal costoUnitario,
        String proveedor,
        LocalDate fechaCompra,
        String tipoDocumento,
        String numeroDocumento,
        String documentoUrl,
        String notas,
        Long compraVigenteId,
        long totalCompras,
        boolean tieneCambioPrecio,
        LocalDate ultimoCambioPrecio,
        BigDecimal precioCompraAnterior,
        BigDecimal variacionPrecioPorcentual,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
