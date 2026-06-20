DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'insumos'
          AND column_name = 'precio_compra'
    ) THEN
        ALTER TABLE insumos
            ALTER COLUMN precio_compra DROP NOT NULL;
    END IF;
END $$;
