package com.maamacrea.backend.productos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record ProductoCreateRequest(
        @NotBlank(message = "El codigo del producto es obligatorio.")
        @Size(max = 80, message = "El codigo del producto no puede superar los 80 caracteres.")
        String codigo,

        @NotBlank(message = "El nombre del producto es obligatorio.")
        @Size(max = 120, message = "El nombre del producto no puede superar los 120 caracteres.")
        String nombre,

        @NotNull(message = "El tipo de producto es obligatorio.")
        ProductoTipo tipoProducto,

        @NotNull(message = "El valor de venta final del producto es obligatorio.")
        @DecimalMin(value = "0.00", inclusive = true, message = "El valor de venta final del producto no puede ser negativo.")
        BigDecimal precioVenta,

        @DecimalMin(value = "0.00", inclusive = true, message = "El costo de electricidad no puede ser negativo.")
        BigDecimal costoElectricidad,

        @DecimalMin(value = "0.00", inclusive = true, message = "El costo de desgaste de equipo no puede ser negativo.")
        BigDecimal costoDesgasteEquipo,

        @NotEmpty(message = "Debes asociar al menos un insumo al producto.")
        @Size(max = 100, message = "El producto no puede tener mas de 100 insumos asociados en una sola solicitud.")
        List<@Valid ProductoInsumoCreateRequest> insumos) {

    public ProductoCreateRequest(
            String codigo,
            String nombre,
            ProductoTipo tipoProducto,
            BigDecimal precioVenta,
            List<@Valid ProductoInsumoCreateRequest> insumos) {
        this(codigo, nombre, tipoProducto, precioVenta, null, null, insumos);
    }
}
