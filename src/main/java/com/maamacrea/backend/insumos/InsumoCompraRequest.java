package com.maamacrea.backend.insumos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record InsumoCompraRequest(
        @NotNull(message = "La fecha de compra es obligatoria.") LocalDate fechaCompra,
        @NotNull(message = "La cantidad comprada es obligatoria.")
                @Positive(message = "La cantidad comprada debe ser mayor a cero.")
                BigDecimal cantidadComprada,
        @NotBlank(message = "La unidad de medida es obligatoria.")
                @Size(max = 40, message = "La unidad de medida no puede superar los 40 caracteres.")
                String unidadMedida,
        @Positive(message = "El precio total de compra debe ser mayor a cero.")
                BigDecimal precioCompraTotal,
        @PositiveOrZero(message = "El precio neto no puede ser negativo.") BigDecimal precioNeto,
        @PositiveOrZero(message = "El IVA no puede ser negativo.") BigDecimal iva,
        @Positive(message = "El ancho debe ser mayor a cero.") BigDecimal ancho,
        @Positive(message = "El alto debe ser mayor a cero.") BigDecimal alto,
        @Size(max = 40, message = "El tipo de documento no puede superar los 40 caracteres.")
                String tipoDocumento,
        @Size(max = 80, message = "El numero de documento no puede superar los 80 caracteres.")
                String numeroDocumento,
        @Size(max = 500, message = "La ruta del documento no puede superar los 500 caracteres.")
                String documentoUrl,
        @Size(max = 1000, message = "La observacion no puede superar los 1000 caracteres.")
                String observacion) {}
