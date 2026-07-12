package com.maamacrea.backend.productos;

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
@Table(name = "productos")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_producto", nullable = false, length = 40)
    private ProductoTipo tipoProducto;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "precio_venta", nullable = false, precision = 19, scale = 2)
    private BigDecimal precioVenta = BigDecimal.ZERO;

    @Column(name = "costo_materiales", nullable = false, precision = 19, scale = 4)
    private BigDecimal costoMateriales = BigDecimal.ZERO;

    @Column(name = "costo_electricidad", nullable = false, precision = 19, scale = 2)
    private BigDecimal costoElectricidad = BigDecimal.ZERO;

    @Column(name = "costo_desgaste_equipo", nullable = false, precision = 19, scale = 2)
    private BigDecimal costoDesgasteEquipo = BigDecimal.ZERO;

    @Column(name = "precio_sugerido", nullable = false, precision = 19, scale = 2)
    private BigDecimal precioSugerido = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal ganancia = BigDecimal.ZERO;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        applyDefaultMonetaryValues();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
        applyDefaultMonetaryValues();
    }

    private void applyDefaultMonetaryValues() {
        if (precioVenta == null) {
            precioVenta = BigDecimal.ZERO;
        }
        if (costoMateriales == null) {
            costoMateriales = BigDecimal.ZERO;
        }
        if (costoElectricidad == null) {
            costoElectricidad = BigDecimal.ZERO;
        }
        if (costoDesgasteEquipo == null) {
            costoDesgasteEquipo = BigDecimal.ZERO;
        }
        if (precioSugerido == null) {
            precioSugerido = BigDecimal.ZERO;
        }
        if (ganancia == null) {
            ganancia = BigDecimal.ZERO;
        }
    }
}
