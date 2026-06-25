package com.maamacrea.backend.equipamiento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record EquipamientoResponse(
        Long id,
        String codigo,
        String nombre,
        TipoEquipo tipoEquipo,
        String tipoEquipoTexto,
        String descripcion,
        String marca,
        String modelo,
        LocalDate fechaCompra,
        BigDecimal valorCompra,
        Integer vidaUtilEstimadaMeses,
        BigDecimal costoPorUso,
        BigDecimal desgasteAcumulado,
        String mantenciones,
        BigDecimal ahorroReposicion,
        EstadoEquipo estado,
        String estadoTexto,
        LocalDate fechaRegistro,
        LocalDateTime fechaActualizacion) {
}
