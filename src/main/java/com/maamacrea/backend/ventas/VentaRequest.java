package com.maamacrea.backend.ventas;

import java.math.BigDecimal;
import java.time.LocalDate;

public record VentaRequest(
        Long productoId,
        String codigoVendido,
        String coleccionDiseno,
        String referenciaDiseno,
        BigDecimal cantidad,
        BigDecimal precioUnitario,
        BigDecimal totalVenta,
        String clienteNombre,
        String clienteApellidos,
        String clienteRut,
        String clienteTelefono,
        String clienteEmail,
        String clienteDireccion,
        String referenciasDireccion,
        String clienteComuna,
        BigDecimal valorEnvio,
        VentaMetodoPago metodoPago,
        LocalDate fechaPago,
        BigDecimal montoPagado,
        VentaEstadoPedido estadoPedido,
        LocalDate fechaVenta) {}
