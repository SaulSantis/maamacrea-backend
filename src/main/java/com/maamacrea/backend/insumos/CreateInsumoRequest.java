package com.maamacrea.backend.insumos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateInsumoRequest(
        @NotBlank(message = "El codigo de producto es obligatorio.")
                @Size(max = 80, message = "El codigo de producto no puede superar los 80 caracteres.")
                String codigoProducto,
        @NotBlank(message = "El nombre es obligatorio.")
                @Size(max = 120, message = "El nombre no puede superar los 120 caracteres.")
                String nombre,
        @NotBlank(message = "La categoria es obligatoria.")
                @Size(max = 80, message = "La categoria no puede superar los 80 caracteres.")
                String categoria) {}
