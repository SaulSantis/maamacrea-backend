package com.maamacrea.backend.productos;

import java.time.LocalDateTime;

public record ProductoResumenResponse(
        Long id,
        String codigo,
        String nombre,
        ProductoTipo tipoProducto,
        int cantidadInsumos,
        LocalDateTime fechaRegistro) {}
