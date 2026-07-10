ALTER TABLE ventas
    ADD COLUMN coleccion_diseno VARCHAR(120),
    ADD COLUMN cliente_comuna VARCHAR(120),
    ADD COLUMN valor_envio NUMERIC(19, 2) NOT NULL DEFAULT 0,
    ADD COLUMN valor_venta_snapshot NUMERIC(19, 2) NOT NULL DEFAULT 0;

UPDATE ventas
SET costo_total_snapshot = costo_materiales_snapshot,
    valor_venta_snapshot = total_venta
WHERE TRUE;

ALTER TABLE ventas
    ADD CONSTRAINT chk_ventas_valor_envio_nonnegative CHECK (valor_envio >= 0);
