package com.maamacrea.backend.insumos;

public record InsumoDependenciasResponse(
        Long insumoId,
        boolean tieneDependencias,
        long productosAsociados,
        long comprasRegistradas,
        boolean impactaCosteo) {}
