CREATE TABLE ventas (
    id BIGSERIAL PRIMARY KEY,
    producto_id BIGINT NOT NULL,
    codigo_producto_base VARCHAR(80) NOT NULL,
    nombre_producto_base VARCHAR(160) NOT NULL,
    tipo_producto VARCHAR(40) NOT NULL,
    codigo_vendido VARCHAR(80) NOT NULL,
    referencia_diseno VARCHAR(200),
    cantidad NUMERIC(12, 3) NOT NULL,
    precio_unitario NUMERIC(19, 2) NOT NULL,
    total_venta NUMERIC(19, 2) NOT NULL,
    cliente_nombre VARCHAR(120) NOT NULL,
    cliente_apellidos VARCHAR(160),
    cliente_rut VARCHAR(40),
    cliente_telefono VARCHAR(40) NOT NULL,
    cliente_email VARCHAR(160),
    cliente_direccion TEXT,
    metodo_pago VARCHAR(40) NOT NULL,
    fecha_pago DATE NOT NULL,
    monto_pagado NUMERIC(19, 2) NOT NULL,
    estado_pedido VARCHAR(40) NOT NULL,
    costo_materiales_snapshot NUMERIC(19, 4) NOT NULL DEFAULT 0,
    costo_reposicion_snapshot NUMERIC(19, 4) NOT NULL DEFAULT 0,
    costo_total_snapshot NUMERIC(19, 4) NOT NULL DEFAULT 0,
    precio_sugerido_snapshot NUMERIC(19, 2),
    ganancia_directa_snapshot NUMERIC(19, 2) NOT NULL DEFAULT 0,
    fecha_venta DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ventas_producto_id ON ventas(producto_id);
CREATE INDEX idx_ventas_fecha_venta ON ventas(fecha_venta DESC, created_at DESC, id DESC);

ALTER TABLE ventas
    ADD CONSTRAINT chk_ventas_cantidad_positive CHECK (cantidad > 0),
    ADD CONSTRAINT chk_ventas_precio_unitario_positive CHECK (precio_unitario > 0),
    ADD CONSTRAINT chk_ventas_total_positive CHECK (total_venta > 0),
    ADD CONSTRAINT chk_ventas_monto_pagado_nonnegative CHECK (monto_pagado >= 0);
