package com.maamacrea.backend.productos;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoInsumoRepository extends JpaRepository<ProductoInsumo, Long> {

    List<ProductoInsumo> findByProductoId(Long productoId);

    List<ProductoInsumo> findByProductoIdOrderByIdAsc(Long productoId);

    List<ProductoInsumo> findByInsumoId(Long insumoId);

    long countByInsumoId(Long insumoId);

    boolean existsByInsumoId(Long insumoId);

    void deleteByInsumoId(Long insumoId);

    void deleteByProductoId(Long productoId);
}
