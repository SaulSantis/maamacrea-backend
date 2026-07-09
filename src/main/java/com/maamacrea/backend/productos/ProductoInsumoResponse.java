package com.maamacrea.backend.productos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductoInsumoResponse(
        Long id,
        Long insumoId,
        String codigoInsumo,
        String nombreInsumo,
        String categoriaInsumo,
        String unidadMedidaInsumo,
        BigDecimal cantidadUsada,
        BigDecimal anchoUsadoCm,
        BigDecimal altoLargoUsadoCm,
        String medidaUsadaTexto,
        String consumo,
        BigDecimal costoEstimado,
        String mensajeCosto,
        LocalDateTime fechaRegistro) {}
