package com.maamacrea.backend.insumos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record InsumoRequest(
        @NotBlank(message = "El nombre es obligatorio.")
                @Size(max = 120, message = "El nombre no puede superar los 120 caracteres.")
                String nombre,
        @NotBlank(message = "La categoria es obligatoria.")
                @Size(max = 80, message = "La categoria no puede superar los 80 caracteres.")
                String categoria,
        @NotBlank(message = "La unidad de medida es obligatoria.")
                @Size(max = 40, message = "La unidad de medida no puede superar los 40 caracteres.")
                String unidadMedida,
        @NotNull(message = "La cantidad comprada es obligatoria.")
                @Positive(message = "La cantidad comprada debe ser mayor a cero.")
                BigDecimal cantidadComprada,
        @NotNull(message = "El precio total de compra es obligatorio.")
                @PositiveOrZero(message = "El precio total de compra no puede ser negativo.")
                BigDecimal precioCompraTotal,
        @Size(max = 120, message = "El proveedor no puede superar los 120 caracteres.")
                String proveedor,
        LocalDate fechaCompra,
        String notas) {}
