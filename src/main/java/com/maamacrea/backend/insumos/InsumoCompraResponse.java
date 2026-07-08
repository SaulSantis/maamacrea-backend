package com.maamacrea.backend.insumos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record InsumoCompraResponse(
        Long id,
        Long insumoId,
        LocalDate fechaCompra,
        BigDecimal cantidadComprada,
        String unidadMedida,
        BigDecimal precioCompraTotal,
        BigDecimal precioUnitario,
        BigDecimal precioNeto,
        BigDecimal iva,
        BigDecimal ancho,
        BigDecimal alto,
        String tipoDocumento,
        String numeroDocumento,
        String documentoUrl,
        String observacion,
        boolean vigente,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
