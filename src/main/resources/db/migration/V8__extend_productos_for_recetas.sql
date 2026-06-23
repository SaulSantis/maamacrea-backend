ALTER TABLE productos
    ADD COLUMN codigo VARCHAR(80);

UPDATE productos
SET codigo = CONCAT('PROD-', id)
WHERE codigo IS NULL;

ALTER TABLE productos
    ALTER COLUMN codigo SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_productos_codigo
    ON productos (LOWER(codigo));

ALTER TABLE productos
    ADD COLUMN tipo_producto VARCHAR(40);

UPDATE productos
SET tipo_producto = 'COJIN_PERSONALIZADO'
WHERE tipo_producto IS NULL;

ALTER TABLE productos
    ALTER COLUMN tipo_producto SET NOT NULL;

ALTER TABLE productos
    ADD COLUMN created_at TIMESTAMP;

UPDATE productos
SET created_at = CURRENT_TIMESTAMP
WHERE created_at IS NULL;

ALTER TABLE productos
    ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE productos
    ADD COLUMN updated_at TIMESTAMP;

UPDATE productos
SET updated_at = CURRENT_TIMESTAMP
WHERE updated_at IS NULL;

ALTER TABLE productos
    ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE producto_insumos
    ADD COLUMN ancho_usado_cm NUMERIC(12, 3);

ALTER TABLE producto_insumos
    ADD COLUMN alto_largo_usado_cm NUMERIC(12, 3);

ALTER TABLE producto_insumos
    ADD COLUMN consumo VARCHAR(120);

ALTER TABLE producto_insumos
    ADD COLUMN costo_estimado NUMERIC(19, 4);

ALTER TABLE producto_insumos
    ADD COLUMN created_at TIMESTAMP;

UPDATE producto_insumos
SET created_at = CURRENT_TIMESTAMP
WHERE created_at IS NULL;

ALTER TABLE producto_insumos
    ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE producto_insumos
    ADD COLUMN updated_at TIMESTAMP;

UPDATE producto_insumos
SET updated_at = CURRENT_TIMESTAMP
WHERE updated_at IS NULL;

ALTER TABLE producto_insumos
    ALTER COLUMN updated_at SET NOT NULL;
