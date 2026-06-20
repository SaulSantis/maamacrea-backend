package com.maamacrea.backend.insumos;

import java.util.Arrays;

public enum InsumoCategoria {
    MATERIALES_TEXTILES("Materiales textiles"),
    SUBLIMACION("Sublimacion"),
    COSTURA_Y_CONFECCION("Costura y confeccion"),
    EMPAQUE_Y_DESPACHO("Empaque y despacho");

    private final String label;

    InsumoCategoria(String label) {
        this.label = label;
    }

    public String getCodigo() {
        return name();
    }

    public String getLabel() {
        return label;
    }

    public static InsumoCategoria fromCodigo(String codigo) {
        return Arrays.stream(values())
                .filter(categoria -> categoria.name().equals(codigo))
                .findFirst()
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "La categoria debe ser una de las permitidas."));
    }
}
