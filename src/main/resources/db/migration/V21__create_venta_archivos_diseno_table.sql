CREATE TABLE venta_archivos_diseno (
    id BIGSERIAL PRIMARY KEY,
    venta_id BIGINT NOT NULL,
    nombre_original VARCHAR(255) NOT NULL,
    nombre_almacenado VARCHAR(255) NOT NULL,
    ruta_almacenamiento VARCHAR(1000) NOT NULL,
    tipo_mime VARCHAR(150) NOT NULL,
    tamano_bytes BIGINT NOT NULL DEFAULT 0,
    orden_visual INTEGER NOT NULL DEFAULT 0,
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_venta_archivo_diseno_venta
        FOREIGN KEY (venta_id)
        REFERENCES ventas(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_venta_archivos_diseno_venta_id
    ON venta_archivos_diseno (venta_id);

CREATE INDEX idx_venta_archivos_diseno_venta_orden
    ON venta_archivos_diseno (venta_id, orden_visual, id);

INSERT INTO venta_archivos_diseno (
    venta_id,
    nombre_original,
    nombre_almacenado,
    ruta_almacenamiento,
    tipo_mime,
    tamano_bytes,
    orden_visual,
    fecha_registro
)
SELECT
    venta.id,
    COALESCE(NULLIF(substring(venta.imagen_diseno_url from '[^/]+$'), ''), 'archivo-legado'),
    COALESCE(NULLIF(substring(venta.imagen_diseno_url from '[^/]+$'), ''), 'archivo-legado'),
    venta.imagen_diseno_url,
    CASE
        WHEN lower(venta.imagen_diseno_url) LIKE '%.png' THEN 'image/png'
        WHEN lower(venta.imagen_diseno_url) LIKE '%.jpg' THEN 'image/jpeg'
        WHEN lower(venta.imagen_diseno_url) LIKE '%.jpeg' THEN 'image/jpeg'
        WHEN lower(venta.imagen_diseno_url) LIKE '%.webp' THEN 'image/webp'
        WHEN lower(venta.imagen_diseno_url) LIKE '%.pdf' THEN 'application/pdf'
        ELSE 'application/octet-stream'
    END,
    0,
    0,
    COALESCE(venta.updated_at, venta.created_at, CURRENT_TIMESTAMP)
FROM ventas venta
WHERE venta.imagen_diseno_url IS NOT NULL
  AND btrim(venta.imagen_diseno_url) <> ''
  AND NOT EXISTS (
      SELECT 1
      FROM venta_archivos_diseno archivo
      WHERE archivo.venta_id = venta.id
  );
