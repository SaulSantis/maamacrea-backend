package com.maamacrea.backend.insumos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "insumo_compras")
public class InsumoCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "insumo_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Insumo insumo;

    @Column(name = "fecha_compra", nullable = false)
    private LocalDate fechaCompra;

    @Column(name = "cantidad_comprada", nullable = false, precision = 12, scale = 3)
    private BigDecimal cantidadComprada;

    @Column(name = "cantidad_ml_comprados", precision = 12, scale = 4)
    private BigDecimal cantidadMlComprados;

    @Column(name = "contenido_por_unidad", precision = 12, scale = 4)
    private BigDecimal contenidoPorUnidad;

    @Column(name = "unidad_contenido", length = 20)
    private String unidadContenido;

    @Column(name = "contenido_total_comprado", precision = 14, scale = 4)
    private BigDecimal contenidoTotalComprado;

    @Column(name = "unidad_medida", nullable = false, length = 40)
    private String unidadMedida;

    @Column(name = "precio_compra_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioCompraTotal;

    @Column(name = "precio_unitario", precision = 19, scale = 4)
    private BigDecimal precioUnitario;

    @Column(name = "precio_neto", precision = 12, scale = 2)
    private BigDecimal precioNeto;

    @Column(precision = 12, scale = 2)
    private BigDecimal iva;

    @Column(precision = 12, scale = 3)
    private BigDecimal ancho;

    @Column(name = "unidad_ancho", length = 20)
    private String unidadAncho;

    @Column(precision = 12, scale = 3)
    private BigDecimal alto;

    @Column(name = "unidad_alto", length = 20)
    private String unidadAlto;

    @Column(name = "tipo_documento", length = 40)
    private String tipoDocumento;

    @Column(name = "numero_documento", length = 80)
    private String numeroDocumento;

    @Column(name = "documento_url", length = 500)
    private String documentoUrl;

    @Column(name = "observacion", columnDefinition = "TEXT")
    private String observacion;

    @Column(nullable = false)
    private boolean vigente;

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
