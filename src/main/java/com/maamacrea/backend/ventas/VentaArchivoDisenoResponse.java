package com.maamacrea.backend.ventas;

import java.time.LocalDateTime;

public record VentaArchivoDisenoResponse(
        Long id,
        String nombreOriginal,
        String tipoMime,
        Long tamanoBytes,
        Integer ordenVisual,
        LocalDateTime fechaRegistro,
        String urlVisualizacion,
        String urlDescarga) {}
