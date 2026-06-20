CREATE TABLE insumos (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL,
    categoria VARCHAR(80) NOT NULL,
    unidad_medida VARCHAR(40) NOT NULL,
    cantidad_comprada NUMERIC(12, 3) NOT NULL,
    precio_compra_total NUMERIC(12, 2) NOT NULL,
    costo_unitario NUMERIC(12, 4) NOT NULL,
    proveedor VARCHAR(120),
    fecha_compra DATE,
    notas TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_insumos_cantidad_comprada_positive CHECK (cantidad_comprada > 0),
    CONSTRAINT chk_insumos_precio_compra_total_non_negative CHECK (precio_compra_total >= 0),
    CONSTRAINT chk_insumos_costo_unitario_non_negative CHECK (costo_unitario >= 0)
);

CREATE INDEX idx_insumos_nombre ON insumos (nombre);
CREATE INDEX idx_insumos_categoria ON insumos (categoria);
