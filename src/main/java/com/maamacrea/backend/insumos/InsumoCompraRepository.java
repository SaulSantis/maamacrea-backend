package com.maamacrea.backend.insumos;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InsumoCompraRepository extends JpaRepository<InsumoCompra, Long> {

    List<InsumoCompra> findByInsumoIdOrderByFechaCompraDescCreatedAtDescIdDesc(Long insumoId);

    Optional<InsumoCompra> findFirstByInsumoIdAndVigenteTrueOrderByFechaCompraDescCreatedAtDescIdDesc(
            Long insumoId);

    Optional<InsumoCompra> findFirstByInsumoIdOrderByFechaCompraDescCreatedAtDescIdDesc(Long insumoId);

    long countByInsumoId(Long insumoId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update InsumoCompra compra set compra.vigente = false where compra.insumo.id = :insumoId and compra.vigente = true")
    int clearVigenteByInsumoId(@Param("insumoId") Long insumoId);
}
