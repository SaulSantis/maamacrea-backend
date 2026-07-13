package com.maamacrea.backend.ventas;

import java.time.LocalDateTime;

public record VentaArchivoDisenoResponse(
        Long id,
        String nombreOriginal,
        String tipoMime,
        String formatoFinal,
        Long tamanoOriginalBytes,
        Long tamanoOptimizadoBytes,
        Integer anchoOriginal,
        Integer altoOriginal,
        Integer anchoOptimizado,
        Integer altoOptimizado,
        Integer ordenVisual,
        LocalDateTime fechaRegistro,
        String urlMiniatura,
        String urlVisualizacion,
        String urlDescarga) {}
