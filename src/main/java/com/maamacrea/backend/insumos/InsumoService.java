package com.maamacrea.backend.insumos;

import com.maamacrea.backend.ResourceNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class InsumoService {

    private final InsumoRepository insumoRepository;

    public InsumoService(InsumoRepository insumoRepository) {
        this.insumoRepository = insumoRepository;
    }

    public List<InsumoResponse> listarTodos() {
        return insumoRepository.findAll().stream().map(this::toResponse).toList();
    }

    public InsumoResponse buscarPorId(Long id) {
        return toResponse(obtenerEntidad(id));
    }

    public InsumoResponse crear(InsumoRequest insumoRequest) {
        Insumo insumo = new Insumo();
        aplicarRequest(insumo, insumoRequest);
        prepararCostoUnitario(insumo);
        return toResponse(insumoRepository.save(insumo));
    }

    public InsumoResponse actualizar(Long id, InsumoRequest insumoRequest) {
        Insumo insumo = obtenerEntidad(id);
        aplicarRequest(insumo, insumoRequest);
        prepararCostoUnitario(insumo);
        return toResponse(insumoRepository.save(insumo));
    }

    public void eliminar(Long id) {
        Insumo insumo = obtenerEntidad(id);
        insumoRepository.delete(insumo);
    }

    private void prepararCostoUnitario(Insumo insumo) {
        BigDecimal cantidadComprada = valorRequerido(insumo.getCantidadComprada(), "cantidadComprada");
        BigDecimal precioCompraTotal =
                valorRequerido(insumo.getPrecioCompraTotal(), "precioCompraTotal");

        if (cantidadComprada.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad comprada debe ser mayor a cero.");
        }

        if (precioCompraTotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El precio de compra total no puede ser negativo.");
        }

        BigDecimal costoUnitario =
                precioCompraTotal.divide(cantidadComprada, 4, RoundingMode.HALF_UP);
        insumo.setCostoUnitario(costoUnitario);
    }

    private BigDecimal valorRequerido(BigDecimal valor, String campo) {
        if (valor == null) {
            throw new IllegalArgumentException("El campo " + campo + " es obligatorio.");
        }
        return valor;
    }

    private Insumo obtenerEntidad(Long id) {
        return insumoRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Insumo no encontrado."));
    }

    private void aplicarRequest(Insumo insumo, InsumoRequest insumoRequest) {
        InsumoCategoria categoria = InsumoCategoria.fromCodigo(insumoRequest.categoria());

        insumo.setNombre(insumoRequest.nombre().trim());
        insumo.setCategoria(categoria.getCodigo());
        insumo.setUnidadMedida(insumoRequest.unidadMedida().trim());
        insumo.setCantidadComprada(insumoRequest.cantidadComprada());
        insumo.setPrecioCompraTotal(insumoRequest.precioCompraTotal());
        insumo.setProveedor(normalizarTexto(insumoRequest.proveedor()));
        insumo.setFechaCompra(insumoRequest.fechaCompra());
        insumo.setNotas(normalizarTexto(insumoRequest.notas()));
    }

    private InsumoResponse toResponse(Insumo insumo) {
        InsumoCategoria categoria = InsumoCategoria.fromCodigo(insumo.getCategoria());

        return new InsumoResponse(
                insumo.getId(),
                insumo.getNombre(),
                categoria.getCodigo(),
                categoria.getLabel(),
                insumo.getUnidadMedida(),
                insumo.getCantidadComprada(),
                insumo.getPrecioCompraTotal(),
                insumo.getCostoUnitario(),
                insumo.getProveedor(),
                insumo.getFechaCompra(),
                insumo.getNotas(),
                insumo.getCreatedAt(),
                insumo.getUpdatedAt());
    }

    private String normalizarTexto(String valor) {
        if (valor == null) {
            return null;
        }

        String valorNormalizado = valor.trim();
        return valorNormalizado.isEmpty() ? null : valorNormalizado;
    }
}
