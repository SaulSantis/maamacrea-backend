package com.maamacrea.backend.equipamiento;

import com.maamacrea.backend.ResourceNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EquipamientoService {

    private final EquipamientoRepository equipamientoRepository;

    public EquipamientoService(EquipamientoRepository equipamientoRepository) {
        this.equipamientoRepository = equipamientoRepository;
    }

    @Transactional(readOnly = true)
    public List<EquipamientoResponse> listarTodos() {
        return equipamientoRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EquipamientoResponse buscarPorId(Long id) {
        return toResponse(obtenerEquipamiento(id));
    }

    @Transactional
    public EquipamientoResponse crear(EquipamientoRequest request) {
        validarNegocios(request, null);

        Equipamiento equipamiento = new Equipamiento();
        applyRequest(equipamiento, request);
        return toResponse(equipamientoRepository.save(equipamiento));
    }

    @Transactional
    public EquipamientoResponse actualizar(Long id, EquipamientoRequest request) {
        Equipamiento equipamiento = obtenerEquipamiento(id);
        validarNegocios(request, equipamiento.getId());

        applyRequest(equipamiento, request);
        return toResponse(equipamientoRepository.save(equipamiento));
    }

    @Transactional
    public void eliminar(Long id) {
        equipamientoRepository.delete(obtenerEquipamiento(id));
    }

    private void validarNegocios(EquipamientoRequest request, Long equipamientoIdActual) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud de equipamiento es obligatoria.");
        }

        if (request.nombre() == null || request.nombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del equipo es obligatorio.");
        }

        if (request.tipoEquipo() == null) {
            throw new IllegalArgumentException("El tipo de equipo es obligatorio.");
        }

        validarNoNegativo(request.valorCompra(), "El valor de compra no puede ser negativo.");
        validarNoNegativo(request.costoPorUso(), "El costo por uso no puede ser negativo.");
        validarNoNegativo(request.desgasteAcumulado(), "El desgaste acumulado no puede ser negativo.");
        validarNoNegativo(request.ahorroReposicion(), "El ahorro para reposicion no puede ser negativo.");

        if (request.vidaUtilEstimadaMeses() != null && request.vidaUtilEstimadaMeses() <= 0) {
            throw new IllegalArgumentException("La vida util estimada debe ser mayor a cero.");
        }

        String codigo = normalizeOptionalText(request.codigo());
        if (codigo != null && equipamientoRepository.existsByCodigoIgnoreCase(codigo)) {
            if (equipamientoIdActual == null || codigoPerteneceAOtroRegistro(codigo, equipamientoIdActual)) {
                throw new IllegalArgumentException("Ya existe un equipo con el codigo indicado.");
            }
        }
    }

    private boolean codigoPerteneceAOtroRegistro(String codigo, Long equipamientoIdActual) {
        return equipamientoRepository.findByCodigoIgnoreCase(codigo)
                .map(item -> !item.getId().equals(equipamientoIdActual))
                .orElse(false);
    }

    private void applyRequest(Equipamiento equipamiento, EquipamientoRequest request) {
        equipamiento.setCodigo(normalizeOptionalText(request.codigo()));
        equipamiento.setNombre(request.nombre().trim());
        equipamiento.setTipoEquipo(request.tipoEquipo());
        equipamiento.setDescripcion(normalizeOptionalText(request.descripcion()));
        equipamiento.setMarca(normalizeOptionalText(request.marca()));
        equipamiento.setModelo(normalizeOptionalText(request.modelo()));
        equipamiento.setFechaCompra(request.fechaCompra());
        equipamiento.setValorCompra(scaleMoney(request.valorCompra()));
        equipamiento.setVidaUtilEstimadaMeses(request.vidaUtilEstimadaMeses());
        equipamiento.setCostoPorUso(scaleMoney(request.costoPorUso()));
        equipamiento.setDesgasteAcumulado(scaleMoney(request.desgasteAcumulado()));
        equipamiento.setMantenciones(normalizeOptionalText(request.mantenciones()));
        equipamiento.setAhorroReposicion(scaleMoney(request.ahorroReposicion()));
        equipamiento.setEstado(request.estado());
    }

    private Equipamiento obtenerEquipamiento(Long id) {
        return equipamientoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Equipamiento no encontrado: " + id));
    }

    private EquipamientoResponse toResponse(Equipamiento equipamiento) {
        return new EquipamientoResponse(
                equipamiento.getId(),
                equipamiento.getCodigo(),
                equipamiento.getNombre(),
                equipamiento.getTipoEquipo(),
                formatTipoEquipo(equipamiento.getTipoEquipo()),
                equipamiento.getDescripcion(),
                equipamiento.getMarca(),
                equipamiento.getModelo(),
                equipamiento.getFechaCompra(),
                equipamiento.getValorCompra(),
                equipamiento.getVidaUtilEstimadaMeses(),
                equipamiento.getCostoPorUso(),
                equipamiento.getDesgasteAcumulado(),
                equipamiento.getMantenciones(),
                equipamiento.getAhorroReposicion(),
                equipamiento.getEstado(),
                formatEstado(equipamiento.getEstado()),
                equipamiento.getFechaRegistro(),
                equipamiento.getFechaActualizacion());
    }

    private String formatTipoEquipo(TipoEquipo tipoEquipo) {
        if (tipoEquipo == null) {
            return "";
        }

        return switch (tipoEquipo) {
            case IMPRESORA -> "Impresora";
            case MAQUINA_COSER -> "Máquina de coser";
            case COMPUTADOR -> "Computador";
            case MAQUINA_ESTAMPADORA -> "Máquina estampadora";
            case OTRO -> "Otro";
        };
    }

    private String formatEstado(EstadoEquipo estado) {
        if (estado == null) {
            return "";
        }

        return switch (estado) {
            case ACTIVO -> "Activo";
            case EN_MANTENCION -> "En mantención";
            case FUERA_DE_USO -> "Fuera de uso";
        };
    }

    private BigDecimal scaleMoney(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    private void validarNoNegativo(BigDecimal value, String message) {
        if (value != null && value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
