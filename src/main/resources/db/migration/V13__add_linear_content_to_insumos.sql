ALTER TABLE insumos
    ADD COLUMN IF NOT EXISTS contenido_por_unidad NUMERIC(12, 4),
    ADD COLUMN IF NOT EXISTS unidad_contenido VARCHAR(20),
    ADD COLUMN IF NOT EXISTS contenido_total_comprado NUMERIC(14, 4);

ALTER TABLE insumo_compras
    ADD COLUMN IF NOT EXISTS contenido_por_unidad NUMERIC(12, 4),
    ADD COLUMN IF NOT EXISTS unidad_contenido VARCHAR(20),
    ADD COLUMN IF NOT EXISTS contenido_total_comprado NUMERIC(14, 4);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_insumos_contenido_por_unidad_positive'
    ) THEN
        ALTER TABLE insumos
            ADD CONSTRAINT chk_insumos_contenido_por_unidad_positive
                CHECK (contenido_por_unidad IS NULL OR contenido_por_unidad > 0);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_insumos_contenido_total_positive'
    ) THEN
        ALTER TABLE insumos
            ADD CONSTRAINT chk_insumos_contenido_total_positive
                CHECK (contenido_total_comprado IS NULL OR contenido_total_comprado > 0);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_insumo_compras_contenido_por_unidad_positive'
    ) THEN
        ALTER TABLE insumo_compras
            ADD CONSTRAINT chk_insumo_compras_contenido_por_unidad_positive
                CHECK (contenido_por_unidad IS NULL OR contenido_por_unidad > 0);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_insumo_compras_contenido_total_positive'
    ) THEN
        ALTER TABLE insumo_compras
            ADD CONSTRAINT chk_insumo_compras_contenido_total_positive
                CHECK (contenido_total_comprado IS NULL OR contenido_total_comprado > 0);
    END IF;
END $$;
