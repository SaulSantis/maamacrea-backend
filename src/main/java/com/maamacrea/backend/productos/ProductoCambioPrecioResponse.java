package com.maamacrea.backend.productos;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductoCambioPrecioResponse(
        Long insumoId,
        String codigoInsumo,
        String nombreInsumo,
        LocalDate ultimoCambioPrecio,
        BigDecimal precioAnterior,
        BigDecimal precioVigente,
        BigDecimal variacionPrecioPorcentual) {}
