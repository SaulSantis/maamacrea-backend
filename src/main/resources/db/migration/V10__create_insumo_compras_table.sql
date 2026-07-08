CREATE TABLE insumo_compras (
    id BIGSERIAL PRIMARY KEY,
    insumo_id BIGINT NOT NULL,
    fecha_compra DATE NOT NULL,
    cantidad_comprada NUMERIC(12, 3) NOT NULL,
    unidad_medida VARCHAR(40) NOT NULL,
    precio_compra_total NUMERIC(12, 2) NOT NULL,
    precio_unitario NUMERIC(19, 4),
    precio_neto NUMERIC(12, 2),
    iva NUMERIC(12, 2),
    ancho NUMERIC(12, 3),
    alto NUMERIC(12, 3),
    tipo_documento VARCHAR(40),
    numero_documento VARCHAR(80),
    documento_url VARCHAR(500),
    observacion TEXT,
    vigente BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_insumo_compras_insumo FOREIGN KEY (insumo_id) REFERENCES insumos (id) ON DELETE CASCADE,
    CONSTRAINT chk_insumo_compras_cantidad_positive CHECK (cantidad_comprada > 0),
    CONSTRAINT chk_insumo_compras_precio_total_positive CHECK (precio_compra_total > 0),
    CONSTRAINT chk_insumo_compras_precio_neto_non_negative CHECK (precio_neto IS NULL OR precio_neto >= 0),
    CONSTRAINT chk_insumo_compras_iva_non_negative CHECK (iva IS NULL OR iva >= 0),
    CONSTRAINT chk_insumo_compras_ancho_positive CHECK (ancho IS NULL OR ancho > 0),
    CONSTRAINT chk_insumo_compras_alto_positive CHECK (alto IS NULL OR alto > 0)
);

CREATE INDEX idx_insumo_compras_insumo ON insumo_compras (insumo_id);
CREATE INDEX idx_insumo_compras_fecha_compra ON insumo_compras (fecha_compra DESC);
CREATE UNIQUE INDEX ux_insumo_compras_one_vigente ON insumo_compras (insumo_id) WHERE vigente;

INSERT INTO insumo_compras (
    insumo_id,
    fecha_compra,
    cantidad_comprada,
    unidad_medida,
    precio_compra_total,
    precio_unitario,
    precio_neto,
    iva,
    ancho,
    alto,
    tipo_documento,
    numero_documento,
    documento_url,
    observacion,
    vigente,
    created_at,
    updated_at
)
SELECT
    insumo.id,
    COALESCE(insumo.fecha_compra, CAST(insumo.created_at AS DATE)),
    insumo.cantidad_comprada,
    insumo.unidad_medida,
    insumo.precio_compra_total,
    CASE
        WHEN insumo.cantidad_comprada > 0
            THEN ROUND(insumo.precio_compra_total / insumo.cantidad_comprada, 4)
        ELSE NULL
    END,
    insumo.precio_neto,
    insumo.iva,
    insumo.ancho,
    insumo.alto,
    insumo.tipo_documento,
    insumo.numero_documento,
    insumo.documento_url,
    insumo.notas,
    TRUE,
    insumo.created_at,
    insumo.updated_at
FROM insumos insumo
WHERE NOT EXISTS (
    SELECT 1
    FROM insumo_compras compra
    WHERE compra.insumo_id = insumo.id
);
