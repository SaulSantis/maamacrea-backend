package com.maamacrea.backend.productos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

        @NotEmpty(message = "Debes asociar al menos un insumo al producto.")
        @Size(max = 100, message = "El producto no puede tener mas de 100 insumos asociados en una sola solicitud.")
        List<@Valid ProductoInsumoCreateRequest> insumos) {}
