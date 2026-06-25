package com.maamacrea.backend.equipamiento;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipamientoRepository extends JpaRepository<Equipamiento, Long> {

    boolean existsByCodigoIgnoreCase(String codigo);

    Optional<Equipamiento> findByCodigoIgnoreCase(String codigo);
}
