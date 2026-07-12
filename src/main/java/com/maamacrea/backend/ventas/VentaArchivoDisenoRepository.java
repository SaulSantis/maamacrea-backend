package com.maamacrea.backend.ventas;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VentaArchivoDisenoRepository extends JpaRepository<VentaArchivoDiseno, Long> {

    Optional<VentaArchivoDiseno> findByIdAndVentaId(Long id, Long ventaId);
}
