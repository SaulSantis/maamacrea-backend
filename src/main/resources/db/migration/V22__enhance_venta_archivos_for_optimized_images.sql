ALTER TABLE venta_archivos_diseno
    ADD COLUMN IF NOT EXISTS nombre_miniatura VARCHAR(255),
    ADD COLUMN IF NOT EXISTS ruta_miniatura VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS formato_final VARCHAR(40),
    ADD COLUMN IF NOT EXISTS tamano_original_bytes BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS tamano_optimizado_bytes BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS ancho_original INTEGER,
    ADD COLUMN IF NOT EXISTS alto_original INTEGER,
    ADD COLUMN IF NOT EXISTS ancho_optimizado INTEGER,
    ADD COLUMN IF NOT EXISTS alto_optimizado INTEGER,
    ADD COLUMN IF NOT EXISTS hash_sha256 CHAR(64);

UPDATE venta_archivos_diseno
SET tamano_optimizado_bytes = COALESCE(tamano_bytes, 0)
WHERE tamano_optimizado_bytes = 0;

UPDATE venta_archivos_diseno
SET formato_final = CASE
    WHEN lower(tipo_mime) = 'image/png' THEN 'PNG'
    WHEN lower(tipo_mime) IN ('image/jpeg', 'image/jpg') THEN 'JPEG'
    WHEN lower(tipo_mime) = 'image/webp' THEN 'WEBP'
    ELSE 'LEGACY'
END
WHERE formato_final IS NULL;

CREATE INDEX IF NOT EXISTS idx_venta_archivos_diseno_hash_sha256
    ON venta_archivos_diseno (hash_sha256);
