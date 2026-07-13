package com.maamacrea.backend.ventas;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
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
@Table(name = "venta_archivos_diseno")
public class VentaArchivoDiseno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venta_id", nullable = false)
    private Venta venta;

    @Column(name = "nombre_original", nullable = false, length = 255)
    private String nombreOriginal;

    @Column(name = "nombre_almacenado", nullable = false, length = 255)
    private String nombreAlmacenado;

    @Column(name = "ruta_almacenamiento", nullable = false, length = 1000)
    private String rutaAlmacenamiento;

    @Column(name = "nombre_miniatura", length = 255)
    private String nombreMiniatura;

    @Column(name = "ruta_miniatura", length = 1000)
    private String rutaMiniatura;

    @Column(name = "tipo_mime", nullable = false, length = 150)
    private String tipoMime;

    @Column(name = "tamano_bytes", nullable = false)
    private Long tamanoBytes = 0L;

    @Column(name = "formato_final", length = 40)
    private String formatoFinal;

    @Column(name = "tamano_original_bytes", nullable = false)
    private Long tamanoOriginalBytes = 0L;

    @Column(name = "tamano_optimizado_bytes", nullable = false)
    private Long tamanoOptimizadoBytes = 0L;

    @Column(name = "ancho_original")
    private Integer anchoOriginal;

    @Column(name = "alto_original")
    private Integer altoOriginal;

    @Column(name = "ancho_optimizado")
    private Integer anchoOptimizado;

    @Column(name = "alto_optimizado")
    private Integer altoOptimizado;

    @Column(name = "hash_sha256", length = 64)
    private String hashSha256;

    @Column(name = "orden_visual", nullable = false)
    private Integer ordenVisual = 0;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @PrePersist
    void prePersist() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }
    }
}
