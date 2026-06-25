package com.maamacrea.backend.equipamiento;

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
@Table(name = "equipamientos")
public class Equipamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 80)
    private String codigo;

    @Column(nullable = false, length = 160)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_equipo", nullable = false, length = 40)
    private TipoEquipo tipoEquipo;

    @Column(length = 500)
    private String descripcion;

    @Column(length = 120)
    private String marca;

    @Column(length = 120)
    private String modelo;

    @Column(name = "fecha_compra")
    private LocalDate fechaCompra;

    @Column(name = "valor_compra", precision = 14, scale = 2)
    private BigDecimal valorCompra;

    @Column(name = "vida_util_estimada_meses")
    private Integer vidaUtilEstimadaMeses;

    @Column(name = "costo_por_uso", precision = 14, scale = 2)
    private BigDecimal costoPorUso;

    @Column(name = "desgaste_acumulado", precision = 14, scale = 2)
    private BigDecimal desgasteAcumulado;

    @Column(length = 500)
    private String mantenciones;

    @Column(name = "ahorro_reposicion", precision = 14, scale = 2)
    private BigDecimal ahorroReposicion;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private EstadoEquipo estado;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDate fechaRegistro;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (fechaRegistro == null) {
            fechaRegistro = now.toLocalDate();
        }
        fechaActualizacion = now;
    }

    @PreUpdate
    void preUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }
}
