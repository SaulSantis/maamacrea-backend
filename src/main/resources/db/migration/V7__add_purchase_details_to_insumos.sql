ALTER TABLE insumos
    ADD COLUMN IF NOT EXISTS codigo_producto VARCHAR(80),
    ADD COLUMN IF NOT EXISTS ancho NUMERIC(12, 3),
    ADD COLUMN IF NOT EXISTS alto NUMERIC(12, 3),
    ADD COLUMN IF NOT EXISTS precio_neto NUMERIC(12, 2),
    ADD COLUMN IF NOT EXISTS iva NUMERIC(12, 2),
    ADD COLUMN IF NOT EXISTS tipo_documento VARCHAR(40),
    ADD COLUMN IF NOT EXISTS numero_documento VARCHAR(80),
    ADD COLUMN IF NOT EXISTS documento_url VARCHAR(500);

UPDATE insumos
SET codigo_producto = COALESCE(NULLIF(BTRIM(codigo_producto), ''), 'LEGACY-' || id)
WHERE codigo_producto IS NULL
   OR BTRIM(codigo_producto) = '';

ALTER TABLE insumos
    ALTER COLUMN codigo_producto SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_insumos_ancho_positive'
    ) THEN
        ALTER TABLE insumos
            ADD CONSTRAINT chk_insumos_ancho_positive CHECK (ancho IS NULL OR ancho > 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_insumos_alto_positive'
    ) THEN
        ALTER TABLE insumos
            ADD CONSTRAINT chk_insumos_alto_positive CHECK (alto IS NULL OR alto > 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_insumos_precio_neto_non_negative'
    ) THEN
        ALTER TABLE insumos
            ADD CONSTRAINT chk_insumos_precio_neto_non_negative CHECK (precio_neto IS NULL OR precio_neto >= 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_insumos_iva_non_negative'
    ) THEN
        ALTER TABLE insumos
            ADD CONSTRAINT chk_insumos_iva_non_negative CHECK (iva IS NULL OR iva >= 0);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_insumos_codigo_producto ON insumos (codigo_producto);
