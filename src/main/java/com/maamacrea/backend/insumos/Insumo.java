package com.maamacrea.backend.insumos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
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
@Table(name = "insumos")
public class Insumo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_producto", nullable = false, length = 80)
    private String codigoProducto;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 80)
    private String categoria;

    @Column(name = "unidad_medida", nullable = false, length = 40)
    private String unidadMedida;

    @Column(name = "cantidad_comprada", nullable = false, precision = 12, scale = 3)
    private BigDecimal cantidadComprada;

    @Column(precision = 12, scale = 3)
    private BigDecimal ancho;

    @Column(precision = 12, scale = 3)
    private BigDecimal alto;

    @Column(name = "precio_neto", precision = 12, scale = 2)
    private BigDecimal precioNeto;

    @Column(precision = 12, scale = 2)
    private BigDecimal iva;

    @Column(name = "precio_compra_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioCompraTotal;

    @Column(name = "costo_unitario", nullable = false, precision = 12, scale = 4)
    private BigDecimal costoUnitario;

    @Column(length = 120)
    private String proveedor;

    @Column(name = "fecha_compra")
    private LocalDate fechaCompra;

    @Column(name = "tipo_documento", length = 40)
    private String tipoDocumento;

    @Column(name = "numero_documento", length = 80)
    private String numeroDocumento;

    @Column(name = "documento_url", length = 500)
    private String documentoUrl;

    @Column(columnDefinition = "TEXT")
    private String notas;

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

    @Transient
    public BigDecimal getPrecioCompra() {
        return precioCompraTotal;
    }

    public void setPrecioCompra(BigDecimal precioCompra) {
        this.precioCompraTotal = precioCompra;
    }

    @Transient
    public BigDecimal getCantidadDisponible() {
        return cantidadComprada;
    }

    public void setCantidadDisponible(BigDecimal cantidadDisponible) {
        this.cantidadComprada = cantidadDisponible;
    }
}
