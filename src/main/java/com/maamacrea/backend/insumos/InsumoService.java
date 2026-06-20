package com.maamacrea.backend.insumos;

import com.maamacrea.backend.ResourceNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class InsumoService {

    private final InsumoRepository insumoRepository;
    private final InsumoDocumentoStorageService insumoDocumentoStorageService;

    public InsumoService(
            InsumoRepository insumoRepository,
            InsumoDocumentoStorageService insumoDocumentoStorageService) {
        this.insumoRepository = insumoRepository;
        this.insumoDocumentoStorageService = insumoDocumentoStorageService;
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

    public InsumoResponse actualizarDocumento(Long id, MultipartFile file) {
        Insumo insumo = obtenerEntidad(id);
        String documentoUrl = insumoDocumentoStorageService.guardarDocumento(id, file);
        insumo.setDocumentoUrl(documentoUrl);
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
        String codigoProducto = normalizarCodigoProducto(insumoRequest.codigoProducto());

        validarCodigoProductoDisponible(codigoProducto, insumo.getId());

        insumo.setCodigoProducto(codigoProducto);
        insumo.setNombre(insumoRequest.nombre().trim());
        insumo.setCategoria(categoria.getCodigo());
        insumo.setUnidadMedida(insumoRequest.unidadMedida().trim());
        insumo.setCantidadComprada(insumoRequest.cantidadComprada());
        insumo.setAncho(insumoRequest.ancho());
        insumo.setAlto(insumoRequest.alto());
        insumo.setPrecioNeto(insumoRequest.precioNeto());
        insumo.setIva(insumoRequest.iva());
        insumo.setPrecioCompraTotal(
                resolverPrecioCompraTotal(
                        insumoRequest.precioCompraTotal(),
                        insumoRequest.precioNeto(),
                        insumoRequest.iva()));
        insumo.setProveedor(normalizarTexto(insumoRequest.proveedor()));
        insumo.setFechaCompra(insumoRequest.fechaCompra());
        insumo.setTipoDocumento(normalizarTipoDocumento(insumoRequest.tipoDocumento()));
        insumo.setNumeroDocumento(normalizarTexto(insumoRequest.numeroDocumento()));
        if (insumoRequest.documentoUrl() != null) {
            insumo.setDocumentoUrl(normalizarTexto(insumoRequest.documentoUrl()));
        }
        insumo.setNotas(normalizarTexto(insumoRequest.notas()));
    }

    private void validarCodigoProductoDisponible(String codigoProducto, Long insumoId) {
        boolean codigoDuplicado =
                insumoId == null
                        ? insumoRepository.existsByCodigoProductoIgnoreCase(codigoProducto)
                        : insumoRepository.existsByCodigoProductoIgnoreCaseAndIdNot(
                                codigoProducto, insumoId);

        if (codigoDuplicado) {
            throw new IllegalArgumentException(
                    "Ya existe un insumo con el codigo de producto informado.");
        }
    }

    private BigDecimal resolverPrecioCompraTotal(
            BigDecimal precioCompraTotal, BigDecimal precioNeto, BigDecimal iva) {
        if (precioCompraTotal != null) {
            return precioCompraTotal;
        }

        if (precioNeto != null && iva != null) {
            return precioNeto.add(iva);
        }

        return null;
    }

    private String normalizarCodigoProducto(String codigoProducto) {
        return codigoProducto.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizarTipoDocumento(String tipoDocumento) {
        String valorNormalizado = normalizarTexto(tipoDocumento);
        if (valorNormalizado == null) {
            return null;
        }

        return InsumoTipoDocumento.fromCodigo(valorNormalizado.toUpperCase(Locale.ROOT)).getCodigo();
    }

    private InsumoResponse toResponse(Insumo insumo) {
        InsumoCategoria categoria = InsumoCategoria.fromCodigo(insumo.getCategoria());

        return new InsumoResponse(
                insumo.getId(),
                insumo.getCodigoProducto(),
                insumo.getNombre(),
                categoria.getCodigo(),
                categoria.getLabel(),
                insumo.getUnidadMedida(),
                insumo.getCantidadComprada(),
                insumo.getAncho(),
                insumo.getAlto(),
                insumo.getPrecioNeto(),
                insumo.getIva(),
                insumo.getPrecioCompraTotal(),
                insumo.getCostoUnitario(),
                insumo.getProveedor(),
                insumo.getFechaCompra(),
                insumo.getTipoDocumento(),
                insumo.getNumeroDocumento(),
                insumo.getDocumentoUrl(),
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
