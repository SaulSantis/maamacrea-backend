package com.maamacrea.backend.insumos;

import java.util.Arrays;

public enum InsumoTipoDocumento {
    FACTURA("Factura"),
    BOLETA("Boleta"),
    SIN_DOCUMENTO("Sin documento");

    private final String label;

    InsumoTipoDocumento(String label) {
        this.label = label;
    }

    public String getCodigo() {
        return name();
    }

    public String getLabel() {
        return label;
    }

    public static InsumoTipoDocumento fromCodigo(String codigo) {
        return Arrays.stream(values())
                .filter(tipoDocumento -> tipoDocumento.name().equals(codigo))
                .findFirst()
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "El tipo de documento debe ser uno de los valores permitidos."));
    }
}
