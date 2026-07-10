package com.maamacrea.backend.insumos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record InsumoCompraResponse(
        Long id,
        Long insumoId,
        LocalDate fechaCompra,
        BigDecimal cantidadComprada,
        BigDecimal cantidadMlComprados,
        BigDecimal contenidoPorUnidad,
        String unidadContenido,
        BigDecimal contenidoTotalComprado,
        String unidadMedida,
        BigDecimal precioCompraTotal,
        BigDecimal precioUnitario,
        BigDecimal precioNeto,
        BigDecimal iva,
        BigDecimal ancho,
        String unidadAncho,
        BigDecimal alto,
        String unidadAlto,
        String tipoDocumento,
        String numeroDocumento,
        String documentoUrl,
        String observacion,
        boolean vigente,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
