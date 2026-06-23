package com.maamacrea.backend.productos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record ProductoInsumoCreateRequest(
        @NotNull(message = "El insumo es obligatorio.")
        Long insumoId,

        @NotNull(message = "La cantidad usada es obligatoria.")
        @Positive(message = "La cantidad usada debe ser mayor a cero.")
        BigDecimal cantidadUsada,

        @Positive(message = "El ancho usado debe ser mayor a cero.")
        BigDecimal anchoUsadoCm,

        @Positive(message = "El alto o largo usado debe ser mayor a cero.")
        BigDecimal altoLargoUsadoCm,

        String consumo,

        @PositiveOrZero(message = "El costo estimado no puede ser negativo.")
        BigDecimal costoEstimado) {}
