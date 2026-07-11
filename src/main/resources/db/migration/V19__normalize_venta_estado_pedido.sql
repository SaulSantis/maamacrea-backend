UPDATE ventas
SET estado_pedido = 'DISENO_Y_CONFECCION'
WHERE estado_pedido IN ('DISENO', 'CONFECCION');

UPDATE ventas
SET estado_pedido = 'ENTREGADO_A_CORREOS'
WHERE estado_pedido = 'ENVIO';

UPDATE ventas
SET estado_pedido = 'RECIBIDO_POR_CLIENTE'
WHERE estado_pedido = 'CERRADO';
