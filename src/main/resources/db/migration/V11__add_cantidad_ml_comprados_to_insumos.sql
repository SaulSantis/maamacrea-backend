ALTER TABLE insumos
    ADD COLUMN IF NOT EXISTS cantidad_ml_comprados NUMERIC(12, 4);

ALTER TABLE insumo_compras
    ADD COLUMN IF NOT EXISTS cantidad_ml_comprados NUMERIC(12, 4);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_insumos_cantidad_ml_comprados_positive'
    ) THEN
        ALTER TABLE insumos
            ADD CONSTRAINT chk_insumos_cantidad_ml_comprados_positive
            CHECK (cantidad_ml_comprados IS NULL OR cantidad_ml_comprados > 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_insumo_compras_cantidad_ml_comprados_positive'
    ) THEN
        ALTER TABLE insumo_compras
            ADD CONSTRAINT chk_insumo_compras_cantidad_ml_comprados_positive
            CHECK (cantidad_ml_comprados IS NULL OR cantidad_ml_comprados > 0);
    END IF;
END $$;

UPDATE insumo_compras compra
SET cantidad_ml_comprados = insumo.cantidad_ml_comprados
FROM insumos insumo
WHERE compra.insumo_id = insumo.id
  AND compra.cantidad_ml_comprados IS NULL
  AND insumo.cantidad_ml_comprados IS NOT NULL;
