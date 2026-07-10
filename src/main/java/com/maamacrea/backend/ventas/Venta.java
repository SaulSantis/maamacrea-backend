package com.maamacrea.backend.ventas;

import com.maamacrea.backend.productos.ProductoTipo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ventas")
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(name = "codigo_producto_base", nullable = false, length = 80)
    private String codigoProductoBase;

    @Column(name = "nombre_producto_base", nullable = false, length = 160)
    private String nombreProductoBase;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_producto", nullable = false, length = 40)
    private ProductoTipo tipoProducto;

    @Column(name = "codigo_vendido", nullable = false, length = 80)
    private String codigoVendido;

    @Column(name = "coleccion_diseno", length = 120)
    private String coleccionDiseno;

    @Column(name = "referencia_diseno", length = 200)
    private String referenciaDiseno;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 19, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "total_venta", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalVenta;

    @Column(name = "cliente_nombre", nullable = false, length = 120)
    private String clienteNombre;

    @Column(name = "cliente_apellidos", length = 160)
    private String clienteApellidos;

    @Column(name = "cliente_rut", length = 40)
    private String clienteRut;

    @Column(name = "cliente_telefono", nullable = false, length = 40)
    private String clienteTelefono;

    @Column(name = "cliente_email", length = 160)
    private String clienteEmail;

    @Column(name = "cliente_direccion", columnDefinition = "TEXT")
    private String clienteDireccion;

    @Column(name = "cliente_comuna", length = 120)
    private String clienteComuna;

    @Column(name = "valor_envio", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorEnvio;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", nullable = false, length = 40)
    private VentaMetodoPago metodoPago;

    @Column(name = "fecha_pago", nullable = false)
    private LocalDate fechaPago;

    @Column(name = "monto_pagado", nullable = false, precision = 19, scale = 2)
    private BigDecimal montoPagado;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_pedido", nullable = false, length = 40)
    private VentaEstadoPedido estadoPedido;

    @Column(name = "costo_materiales_snapshot", nullable = false, precision = 19, scale = 4)
    private BigDecimal costoMaterialesSnapshot;

    @Column(name = "costo_reposicion_snapshot", nullable = false, precision = 19, scale = 4)
    private BigDecimal costoReposicionSnapshot;

    @Column(name = "costo_total_snapshot", nullable = false, precision = 19, scale = 4)
    private BigDecimal costoTotalSnapshot;

    @Column(name = "valor_venta_snapshot", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorVentaSnapshot;

    @Column(name = "precio_sugerido_snapshot", precision = 19, scale = 2)
    private BigDecimal precioSugeridoSnapshot;

    @Column(name = "ganancia_directa_snapshot", nullable = false, precision = 19, scale = 2)
    private BigDecimal gananciaDirectaSnapshot;

    @Column(name = "fecha_venta", nullable = false)
    private LocalDate fechaVenta;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
