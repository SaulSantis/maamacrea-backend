ALTER TABLE insumos
    ADD COLUMN IF NOT EXISTS unidad_ancho VARCHAR(20),
    ADD COLUMN IF NOT EXISTS unidad_alto VARCHAR(20);

ALTER TABLE insumo_compras
    ADD COLUMN IF NOT EXISTS unidad_ancho VARCHAR(20),
    ADD COLUMN IF NOT EXISTS unidad_alto VARCHAR(20);

UPDATE insumos
SET unidad_ancho = CASE
        WHEN ancho IS NULL THEN unidad_ancho
        WHEN LOWER(COALESCE(nombre, '')) LIKE '%papel%' THEN 'cm'
        WHEN LOWER(COALESCE(categoria, '')) LIKE '%textil%'
            OR LOWER(COALESCE(categoria, '')) LIKE '%materiales textiles%'
            OR LOWER(COALESCE(nombre, '')) LIKE '%tela%'
            OR LOWER(COALESCE(nombre, '')) LIKE '%bistrech%'
            OR LOWER(COALESCE(nombre, '')) LIKE '%bistretch%'
            THEN CASE WHEN ancho <= 10 THEN 'm' ELSE 'cm' END
        WHEN LOWER(COALESCE(unidad_medida, '')) IN ('m', 'metro', 'metros', 'metro lineal') THEN 'm'
        WHEN LOWER(COALESCE(unidad_medida, '')) IN ('cm', 'centimetro', 'centimetros') THEN 'cm'
        WHEN LOWER(COALESCE(unidad_medida, '')) = 'rollo' THEN 'cm'
        ELSE unidad_ancho
    END
WHERE unidad_ancho IS NULL;

UPDATE insumos
SET unidad_alto = CASE
        WHEN alto IS NULL THEN unidad_alto
        WHEN LOWER(COALESCE(nombre, '')) LIKE '%papel%' THEN 'm'
        WHEN LOWER(COALESCE(categoria, '')) LIKE '%textil%'
            OR LOWER(COALESCE(categoria, '')) LIKE '%materiales textiles%'
            OR LOWER(COALESCE(nombre, '')) LIKE '%tela%'
            OR LOWER(COALESCE(nombre, '')) LIKE '%bistrech%'
            OR LOWER(COALESCE(nombre, '')) LIKE '%bistretch%'
            THEN 'm'
        WHEN LOWER(COALESCE(unidad_medida, '')) IN ('m', 'metro', 'metros', 'metro lineal') THEN 'm'
        WHEN LOWER(COALESCE(unidad_medida, '')) IN ('cm', 'centimetro', 'centimetros') THEN 'cm'
        WHEN LOWER(COALESCE(unidad_medida, '')) = 'rollo' THEN 'm'
        ELSE unidad_alto
    END
WHERE unidad_alto IS NULL;

UPDATE insumo_compras compra
SET unidad_ancho = CASE
        WHEN compra.ancho IS NULL THEN compra.unidad_ancho
        WHEN LOWER(COALESCE(insumo.nombre, '')) LIKE '%papel%' THEN 'cm'
        WHEN LOWER(COALESCE(insumo.categoria, '')) LIKE '%textil%'
            OR LOWER(COALESCE(insumo.categoria, '')) LIKE '%materiales textiles%'
            OR LOWER(COALESCE(insumo.nombre, '')) LIKE '%tela%'
            OR LOWER(COALESCE(insumo.nombre, '')) LIKE '%bistrech%'
            OR LOWER(COALESCE(insumo.nombre, '')) LIKE '%bistretch%'
            THEN CASE WHEN compra.ancho <= 10 THEN 'm' ELSE 'cm' END
        WHEN LOWER(COALESCE(compra.unidad_medida, '')) IN ('m', 'metro', 'metros', 'metro lineal') THEN 'm'
        WHEN LOWER(COALESCE(compra.unidad_medida, '')) IN ('cm', 'centimetro', 'centimetros') THEN 'cm'
        WHEN LOWER(COALESCE(compra.unidad_medida, '')) = 'rollo' THEN 'cm'
        ELSE compra.unidad_ancho
    END
FROM insumos insumo
WHERE compra.insumo_id = insumo.id
  AND compra.unidad_ancho IS NULL;

UPDATE insumo_compras compra
SET unidad_alto = CASE
        WHEN compra.alto IS NULL THEN compra.unidad_alto
        WHEN LOWER(COALESCE(insumo.nombre, '')) LIKE '%papel%' THEN 'm'
        WHEN LOWER(COALESCE(insumo.categoria, '')) LIKE '%textil%'
            OR LOWER(COALESCE(insumo.categoria, '')) LIKE '%materiales textiles%'
            OR LOWER(COALESCE(insumo.nombre, '')) LIKE '%tela%'
            OR LOWER(COALESCE(insumo.nombre, '')) LIKE '%bistrech%'
            OR LOWER(COALESCE(insumo.nombre, '')) LIKE '%bistretch%'
            THEN 'm'
        WHEN LOWER(COALESCE(compra.unidad_medida, '')) IN ('m', 'metro', 'metros', 'metro lineal') THEN 'm'
        WHEN LOWER(COALESCE(compra.unidad_medida, '')) IN ('cm', 'centimetro', 'centimetros') THEN 'cm'
        WHEN LOWER(COALESCE(compra.unidad_medida, '')) = 'rollo' THEN 'm'
        ELSE compra.unidad_alto
    END
FROM insumos insumo
WHERE compra.insumo_id = insumo.id
  AND compra.unidad_alto IS NULL;
