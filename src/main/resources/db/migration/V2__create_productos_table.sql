CREATE TABLE productos (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    precio_venta NUMERIC(19, 2) NOT NULL,
    costo_materiales NUMERIC(19, 4) NOT NULL DEFAULT 0,
    precio_sugerido NUMERIC(19, 2) NOT NULL DEFAULT 0,
    ganancia NUMERIC(19, 2) NOT NULL DEFAULT 0
);

CREATE INDEX idx_productos_nombre ON productos (nombre);
