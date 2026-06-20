package com.maamacrea.backend.insumos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record InsumoRequest(
        @NotBlank(message = "El codigo de producto es obligatorio.")
                @Size(max = 80, message = "El codigo de producto no puede superar los 80 caracteres.")
                String codigoProducto,
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
        @Positive(message = "El ancho debe ser mayor a cero.") BigDecimal ancho,
        @Positive(message = "El alto debe ser mayor a cero.") BigDecimal alto,
        @PositiveOrZero(message = "El precio neto no puede ser negativo.") BigDecimal precioNeto,
        @PositiveOrZero(message = "El IVA no puede ser negativo.") BigDecimal iva,
        @NotNull(message = "El precio total de compra es obligatorio.")
                @PositiveOrZero(message = "El precio total de compra no puede ser negativo.")
                BigDecimal precioCompraTotal,
        @Size(max = 120, message = "El proveedor no puede superar los 120 caracteres.")
                String proveedor,
        LocalDate fechaCompra,
        @Size(max = 40, message = "El tipo de documento no puede superar los 40 caracteres.")
                String tipoDocumento,
        @Size(max = 80, message = "El numero de documento no puede superar los 80 caracteres.")
                String numeroDocumento,
        @Size(max = 500, message = "La ruta del documento no puede superar los 500 caracteres.")
                String documentoUrl,
        @Size(max = 1000, message = "Las notas no pueden superar los 1000 caracteres.")
                String notas) {}
