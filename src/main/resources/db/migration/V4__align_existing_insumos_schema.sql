ALTER TABLE insumos
    ADD COLUMN IF NOT EXISTS categoria VARCHAR(80),
    ADD COLUMN IF NOT EXISTS precio_compra_total NUMERIC(12, 2),
    ADD COLUMN IF NOT EXISTS costo_unitario NUMERIC(12, 4),
    ADD COLUMN IF NOT EXISTS proveedor VARCHAR(120),
    ADD COLUMN IF NOT EXISTS fecha_compra DATE,
    ADD COLUMN IF NOT EXISTS notas TEXT,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

UPDATE insumos
SET categoria = COALESCE(categoria, 'General'),
    precio_compra_total = COALESCE(precio_compra_total, precio_compra, 0),
    costo_unitario = COALESCE(
        costo_unitario,
        CASE
            WHEN cantidad_comprada > 0 THEN ROUND(COALESCE(precio_compra, 0) / cantidad_comprada, 4)
            ELSE 0
        END
    ),
    created_at = COALESCE(created_at, CURRENT_TIMESTAMP),
    updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP);

ALTER TABLE insumos
    ALTER COLUMN nombre TYPE VARCHAR(120),
    ALTER COLUMN categoria TYPE VARCHAR(80),
    ALTER COLUMN unidad_medida TYPE VARCHAR(40),
    ALTER COLUMN cantidad_comprada TYPE NUMERIC(12, 3) USING ROUND(cantidad_comprada, 3),
    ALTER COLUMN precio_compra_total TYPE NUMERIC(12, 2),
    ALTER COLUMN costo_unitario TYPE NUMERIC(12, 4),
    ALTER COLUMN categoria SET NOT NULL,
    ALTER COLUMN precio_compra_total SET NOT NULL,
    ALTER COLUMN costo_unitario SET NOT NULL,
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_insumos_nombre ON insumos (nombre);
CREATE INDEX IF NOT EXISTS idx_insumos_categoria ON insumos (categoria);
