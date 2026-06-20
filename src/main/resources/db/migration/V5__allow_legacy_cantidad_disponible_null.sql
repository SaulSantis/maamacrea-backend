DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'insumos'
          AND column_name = 'cantidad_disponible'
    ) THEN
        ALTER TABLE insumos
            ALTER COLUMN cantidad_disponible DROP NOT NULL;
    END IF;
END $$;
