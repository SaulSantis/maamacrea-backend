package com.maamacrea.backend.productos;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoInsumoRepository extends JpaRepository<ProductoInsumo, Long> {

    List<ProductoInsumo> findByProductoId(Long productoId);
}
