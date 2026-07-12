package com.maamacrea.backend.ventas;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Objects;

public record VentaMultipartRequest(
        @Valid VentaRequest venta,
        List<Long> archivosEliminarIds) {

    public List<Long> normalizedDeletedFileIds() {
        if (archivosEliminarIds == null) {
            return List.of();
        }

        return archivosEliminarIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }
}
