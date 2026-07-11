package com.maamacrea.backend.ventas;

import com.maamacrea.backend.productos.ProductoTipo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record VentaResponse(
        Long id,
        Long productoId,
        String codigoProductoBase,
        String nombreProductoBase,
        ProductoTipo tipoProducto,
        String codigoVendido,
        String coleccionDiseno,
        String referenciaDiseno,
        String imagenDisenoUrl,
        BigDecimal cantidad,
        BigDecimal precioUnitario,
        BigDecimal totalVenta,
        String clienteNombre,
        String clienteApellidos,
        String clienteRut,
        String clienteTelefono,
        String clienteEmail,
        String clienteDireccion,
        String clienteComuna,
        BigDecimal valorEnvio,
        VentaMetodoPago metodoPago,
        LocalDate fechaPago,
        BigDecimal montoPagado,
        VentaEstadoPedido estadoPedido,
        BigDecimal costoMaterialesSnapshot,
        BigDecimal costoReposicionSnapshot,
        BigDecimal costoTotalSnapshot,
        BigDecimal valorVentaSnapshot,
        BigDecimal precioSugeridoSnapshot,
        BigDecimal gananciaDirectaSnapshot,
        LocalDate fechaVenta,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
