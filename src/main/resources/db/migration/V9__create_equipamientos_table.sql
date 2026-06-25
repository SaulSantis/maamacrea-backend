CREATE TABLE equipamientos (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(80),
    nombre VARCHAR(160) NOT NULL,
    tipo_equipo VARCHAR(40) NOT NULL,
    descripcion VARCHAR(500),
    marca VARCHAR(120),
    modelo VARCHAR(120),
    fecha_compra DATE,
    valor_compra NUMERIC(14, 2),
    vida_util_estimada_meses INTEGER,
    costo_por_uso NUMERIC(14, 2),
    desgaste_acumulado NUMERIC(14, 2),
    mantenciones VARCHAR(500),
    ahorro_reposicion NUMERIC(14, 2),
    estado VARCHAR(40),
    fecha_registro DATE NOT NULL DEFAULT CURRENT_DATE,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
