CREATE TABLE producto_insumos (
    id BIGSERIAL PRIMARY KEY,
    producto_id BIGINT NOT NULL,
    insumo_id BIGINT NOT NULL,
    cantidad_usada NUMERIC(19, 4) NOT NULL,
    CONSTRAINT fk_producto_insumos_producto
        FOREIGN KEY (producto_id) REFERENCES productos (id),
    CONSTRAINT fk_producto_insumos_insumo
        FOREIGN KEY (insumo_id) REFERENCES insumos (id),
    CONSTRAINT chk_producto_insumos_cantidad_usada_positive
        CHECK (cantidad_usada > 0)
);

CREATE INDEX idx_producto_insumos_producto_id ON producto_insumos (producto_id);
CREATE INDEX idx_producto_insumos_insumo_id ON producto_insumos (insumo_id);
