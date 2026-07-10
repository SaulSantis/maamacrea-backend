package com.maamacrea.backend.ventas;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    List<Venta> findAllByOrderByFechaVentaDescCreatedAtDescIdDesc();

    List<Venta> findAllByOrderByFechaVentaDescCreatedAtDescIdDesc(Pageable pageable);
}
