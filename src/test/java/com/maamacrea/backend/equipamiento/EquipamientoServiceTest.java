package com.maamacrea.backend.equipamiento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.maamacrea.backend.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EquipamientoServiceTest {

    @Mock
    private EquipamientoRepository equipamientoRepository;

    @InjectMocks
    private EquipamientoService equipamientoService;

    @Test
    void creaEquipamientoValido() {
        Equipamiento persistido = equipamientoPersistido(1L, "EQ-IMP-001", "Impresora Epson F570");

        when(equipamientoRepository.existsByCodigoIgnoreCase("EQ-IMP-001")).thenReturn(false);
        when(equipamientoRepository.save(any(Equipamiento.class))).thenReturn(persistido);

        EquipamientoResponse response = equipamientoService.crear(requestValido());

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.nombre()).isEqualTo("Impresora Epson F570");
        assertThat(response.tipoEquipoTexto()).isEqualTo("Impresora");
    }

    @Test
    void fallaSiElNombreVieneVacio() {
        EquipamientoRequest request = new EquipamientoRequest(
                "EQ-IMP-001",
                "   ",
                TipoEquipo.IMPRESORA,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                EstadoEquipo.ACTIVO);

        assertThatThrownBy(() -> equipamientoService.crear(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El nombre del equipo es obligatorio.");
    }

    @Test
    void fallaSiElTipoEquipoNoExiste() {
        EquipamientoRequest request = new EquipamientoRequest(
                "EQ-IMP-001",
                "Impresora Epson F570",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                EstadoEquipo.ACTIVO);

        assertThatThrownBy(() -> equipamientoService.crear(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El tipo de equipo es obligatorio.");
    }

    @Test
    void listaEquipamientos() {
        when(equipamientoRepository.findAll()).thenReturn(List.of(
                equipamientoPersistido(1L, "EQ-IMP-001", "Impresora Epson F570"),
                equipamientoPersistido(2L, "EQ-COS-001", "Máquina recta industrial")));

        List<EquipamientoResponse> response = equipamientoService.listarTodos();

        assertThat(response).hasSize(2);
    }

    @Test
    void obtieneEquipamientoPorId() {
        when(equipamientoRepository.findById(1L))
                .thenReturn(Optional.of(equipamientoPersistido(1L, "EQ-IMP-001", "Impresora Epson F570")));

        EquipamientoResponse response = equipamientoService.buscarPorId(1L);

        assertThat(response.codigo()).isEqualTo("EQ-IMP-001");
    }

    @Test
    void actualizaEquipamiento() {
        Equipamiento existente = equipamientoPersistido(1L, "EQ-IMP-001", "Impresora Epson F570");
        when(equipamientoRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(equipamientoRepository.existsByCodigoIgnoreCase("EQ-IMP-001")).thenReturn(true);
        when(equipamientoRepository.findByCodigoIgnoreCase("EQ-IMP-001")).thenReturn(Optional.of(existente));
        when(equipamientoRepository.save(any(Equipamiento.class))).thenReturn(existente);

        EquipamientoResponse response = equipamientoService.actualizar(1L, requestValido());

        assertThat(response.id()).isEqualTo(1L);
        verify(equipamientoRepository).save(any(Equipamiento.class));
    }

    @Test
    void eliminaEquipamiento() {
        Equipamiento existente = equipamientoPersistido(1L, "EQ-IMP-001", "Impresora Epson F570");
        when(equipamientoRepository.findById(1L)).thenReturn(Optional.of(existente));

        equipamientoService.eliminar(1L);

        verify(equipamientoRepository).delete(existente);
    }

    @Test
    void fallaSiNoExisteElEquipamiento() {
        when(equipamientoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> equipamientoService.buscarPorId(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Equipamiento no encontrado: 999");
    }

    @Test
    void fallaSiLleganMontosNegativos() {
        EquipamientoRequest request = new EquipamientoRequest(
                "EQ-IMP-001",
                "Impresora Epson F570",
                TipoEquipo.IMPRESORA,
                null,
                null,
                null,
                null,
                new BigDecimal("-1"),
                60,
                null,
                null,
                null,
                null,
                EstadoEquipo.ACTIVO);

        assertThatThrownBy(() -> equipamientoService.crear(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El valor de compra no puede ser negativo.");
    }

    private EquipamientoRequest requestValido() {
        return new EquipamientoRequest(
                "EQ-IMP-001",
                "Impresora Epson F570",
                TipoEquipo.IMPRESORA,
                "Equipo usado para sublimación.",
                "Epson",
                "F570",
                LocalDate.of(2026, 1, 10),
                new BigDecimal("1200000"),
                60,
                new BigDecimal("2500"),
                new BigDecimal("15000"),
                "Mantención preventiva anual",
                new BigDecimal("50000"),
                EstadoEquipo.ACTIVO);
    }

    private Equipamiento equipamientoPersistido(Long id, String codigo, String nombre) {
        Equipamiento equipamiento = new Equipamiento();
        equipamiento.setId(id);
        equipamiento.setCodigo(codigo);
        equipamiento.setNombre(nombre);
        equipamiento.setTipoEquipo(TipoEquipo.IMPRESORA);
        equipamiento.setEstado(EstadoEquipo.ACTIVO);
        equipamiento.setFechaRegistro(LocalDate.of(2026, 1, 10));
        equipamiento.setFechaActualizacion(LocalDateTime.of(2026, 1, 10, 10, 0));
        return equipamiento;
    }
}
