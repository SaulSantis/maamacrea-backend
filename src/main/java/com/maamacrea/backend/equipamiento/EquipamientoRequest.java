package com.maamacrea.backend.equipamiento;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record EquipamientoRequest(
        @Size(max = 80, message = "El codigo no puede superar los 80 caracteres.")
        String codigo,

        @NotBlank(message = "El nombre del equipo es obligatorio.")
        @Size(max = 160, message = "El nombre no puede superar los 160 caracteres.")
        String nombre,

        @NotNull(message = "El tipo de equipo es obligatorio.")
        TipoEquipo tipoEquipo,

        @Size(max = 500, message = "La descripcion no puede superar los 500 caracteres.")
        String descripcion,

        @Size(max = 120, message = "La marca no puede superar los 120 caracteres.")
        String marca,

        @Size(max = 120, message = "El modelo no puede superar los 120 caracteres.")
        String modelo,

        LocalDate fechaCompra,

        @PositiveOrZero(message = "El valor de compra no puede ser negativo.")
        BigDecimal valorCompra,

        @Positive(message = "La vida util estimada debe ser mayor a cero.")
        Integer vidaUtilEstimadaMeses,

        @PositiveOrZero(message = "El costo por uso no puede ser negativo.")
        BigDecimal costoPorUso,

        @PositiveOrZero(message = "El desgaste acumulado no puede ser negativo.")
        BigDecimal desgasteAcumulado,

        @Size(max = 500, message = "El detalle de mantenciones no puede superar los 500 caracteres.")
        String mantenciones,

        @PositiveOrZero(message = "El ahorro para reposicion no puede ser negativo.")
        BigDecimal ahorroReposicion,

        EstadoEquipo estado) {
}
