ALTER TABLE insumos
    ALTER COLUMN unidad_medida DROP NOT NULL,
    ALTER COLUMN cantidad_comprada DROP NOT NULL,
    ALTER COLUMN precio_compra_total DROP NOT NULL,
    ALTER COLUMN costo_unitario DROP NOT NULL;
