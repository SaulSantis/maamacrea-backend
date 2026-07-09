package com.maamacrea.backend.insumos;

import com.maamacrea.backend.ResourceNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
public class InsumoService {

    private final InsumoRepository insumoRepository;
    private final InsumoCompraRepository insumoCompraRepository;
    private final InsumoDocumentoStorageService insumoDocumentoStorageService;

    public InsumoService(
            InsumoRepository insumoRepository,
            InsumoCompraRepository insumoCompraRepository,
            InsumoDocumentoStorageService insumoDocumentoStorageService) {
        this.insumoRepository = insumoRepository;
        this.insumoCompraRepository = insumoCompraRepository;
        this.insumoDocumentoStorageService = insumoDocumentoStorageService;
    }

    public List<InsumoResponse> listarTodos() {
        return insumoRepository.findAll().stream().map(this::toResponse).toList();
    }

    public InsumoResponse buscarPorId(Long id) {
        return toResponse(obtenerEntidad(id));
    }

    public List<InsumoCompraResponse> listarCompras(Long insumoId) {
        obtenerEntidad(insumoId);
        return obtenerComprasOrdenadas(insumoId).stream().map(this::toCompraResponse).toList();
    }

    public InsumoCompraResponse obtenerPrecioVigente(Long insumoId) {
        obtenerEntidad(insumoId);
        InsumoCompra compra = obtenerCompraVigenteInterna(insumoId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe historial de compras para este insumo."));
        return toCompraResponse(compra);
    }

    @Transactional
    public InsumoResponse crear(InsumoRequest insumoRequest) {
        Insumo insumo = new Insumo();
        aplicarCamposBase(insumo, insumoRequest);
        Insumo guardado = insumoRepository.save(insumo);
        registrarNuevaCompraInterna(guardado, buildCompraInput(insumoRequest));
        return toResponse(insumoRepository.save(guardado));
    }

    @Transactional
    public InsumoResponse actualizar(Long id, InsumoRequest insumoRequest) {
        Insumo insumo = obtenerEntidad(id);
        aplicarCamposBase(insumo, insumoRequest);

        CompraInput compraInput = buildCompraInput(insumoRequest);
        InsumoCompra compraVigente = obtenerCompraVigenteInterna(id).orElse(null);

        if (compraVigente == null) {
            registrarNuevaCompraInterna(insumo, compraInput);
        } else if (tieneCambioDeCompra(compraVigente, compraInput)) {
            registrarNuevaCompraInterna(insumo, compraInput);
        } else {
            actualizarMetadatosCompra(compraVigente, compraInput);
            InsumoCompra compraActualizada = insumoCompraRepository.save(compraVigente);
            sincronizarResumenCompra(insumo, compraActualizada);
        }

        return toResponse(insumoRepository.save(insumo));
    }

    @Transactional
    public InsumoCompraResponse registrarCompra(Long id, InsumoCompraRequest compraRequest) {
        Insumo insumo = obtenerEntidad(id);
        InsumoCompra compra = registrarNuevaCompraInterna(insumo, buildCompraInput(compraRequest));
        insumoRepository.save(insumo);
        return toCompraResponse(compra);
    }

    @Transactional
    public InsumoResponse actualizarDocumento(Long id, MultipartFile file) {
        Insumo insumo = obtenerEntidad(id);
        String documentoUrl = insumoDocumentoStorageService.guardarDocumento(id, file);
        insumo.setDocumentoUrl(documentoUrl);

        obtenerCompraVigenteInterna(id).ifPresent(compra -> {
            compra.setDocumentoUrl(documentoUrl);
            insumoCompraRepository.save(compra);
        });

        return toResponse(insumoRepository.save(insumo));
    }

    @Transactional
    public void eliminar(Long id) {
        Insumo insumo = obtenerEntidad(id);
        insumoRepository.delete(insumo);
    }

    private void aplicarCamposBase(Insumo insumo, InsumoRequest insumoRequest) {
        InsumoCategoria categoria = InsumoCategoria.fromCodigo(insumoRequest.categoria());
        String codigoProducto = normalizarCodigoProducto(insumoRequest.codigoProducto());

        validarCodigoProductoDisponible(codigoProducto, insumo.getId());

        insumo.setCodigoProducto(codigoProducto);
        insumo.setNombre(insumoRequest.nombre().trim());
        insumo.setCategoria(categoria.getCodigo());
        insumo.setProveedor(normalizarTexto(insumoRequest.proveedor()));
    }

    private InsumoCompra registrarNuevaCompraInterna(Insumo insumo, CompraInput compraInput) {
        validarCompraInput(compraInput);
        insumoCompraRepository.clearVigenteByInsumoId(insumo.getId());

        InsumoCompra compra = new InsumoCompra();
        compra.setInsumo(insumo);
        aplicarCompraInput(compra, compraInput);
        compra.setVigente(true);

        InsumoCompra guardada = insumoCompraRepository.save(compra);
        sincronizarResumenCompra(insumo, guardada);
        return guardada;
    }

    private void aplicarCompraInput(InsumoCompra compra, CompraInput compraInput) {
        BigDecimal precioUnitario = calcularPrecioUnitario(
                compraInput.cantidadComprada(),
                compraInput.cantidadMlComprados(),
                compraInput.unidadMedida(),
                compraInput.precioCompraTotal());
        compra.setFechaCompra(compraInput.fechaCompra());
        compra.setCantidadComprada(compraInput.cantidadComprada().setScale(3, RoundingMode.HALF_UP));
        compra.setCantidadMlComprados(scaleOptional(compraInput.cantidadMlComprados(), 4));
        compra.setUnidadMedida(compraInput.unidadMedida());
        compra.setPrecioCompraTotal(compraInput.precioCompraTotal().setScale(2, RoundingMode.HALF_UP));
        compra.setPrecioUnitario(precioUnitario);
        compra.setPrecioNeto(scaleOptional(compraInput.precioNeto(), 2));
        compra.setIva(scaleOptional(compraInput.iva(), 2));
        compra.setAncho(scaleOptional(compraInput.ancho(), 3));
        compra.setAlto(scaleOptional(compraInput.alto(), 3));
        compra.setTipoDocumento(compraInput.tipoDocumento());
        compra.setNumeroDocumento(compraInput.numeroDocumento());
        compra.setDocumentoUrl(compraInput.documentoUrl());
        compra.setObservacion(compraInput.observacion());
    }

    private void actualizarMetadatosCompra(InsumoCompra compra, CompraInput compraInput) {
        compra.setFechaCompra(compraInput.fechaCompra());
        compra.setTipoDocumento(compraInput.tipoDocumento());
        compra.setNumeroDocumento(compraInput.numeroDocumento());
        compra.setDocumentoUrl(compraInput.documentoUrl());
        compra.setObservacion(compraInput.observacion());
        compra.setVigente(true);
    }

    private void sincronizarResumenCompra(Insumo insumo, InsumoCompra compra) {
        insumo.setUnidadMedida(compra.getUnidadMedida());
        insumo.setCantidadComprada(compra.getCantidadComprada());
        insumo.setCantidadMlComprados(compra.getCantidadMlComprados());
        insumo.setAncho(compra.getAncho());
        insumo.setAlto(compra.getAlto());
        insumo.setPrecioNeto(compra.getPrecioNeto());
        insumo.setIva(compra.getIva());
        insumo.setPrecioCompraTotal(compra.getPrecioCompraTotal());
        insumo.setCostoUnitario(compra.getPrecioUnitario());
        insumo.setFechaCompra(compra.getFechaCompra());
        insumo.setTipoDocumento(compra.getTipoDocumento());
        insumo.setNumeroDocumento(compra.getNumeroDocumento());
        insumo.setDocumentoUrl(compra.getDocumentoUrl());
        insumo.setNotas(compra.getObservacion());
    }

    private CompraInput buildCompraInput(InsumoRequest insumoRequest) {
        return new CompraInput(
                insumoRequest.fechaCompra(),
                insumoRequest.cantidadComprada(),
                insumoRequest.cantidadMlComprados(),
                normalizarUnidadMedida(insumoRequest.unidadMedida()),
                resolverPrecioCompraTotal(insumoRequest.precioCompraTotal(), insumoRequest.precioNeto(), insumoRequest.iva()),
                insumoRequest.precioNeto(),
                insumoRequest.iva(),
                insumoRequest.ancho(),
                insumoRequest.alto(),
                normalizarTipoDocumento(insumoRequest.tipoDocumento()),
                normalizarTexto(insumoRequest.numeroDocumento()),
                normalizarTexto(insumoRequest.documentoUrl()),
                normalizarTexto(insumoRequest.notas()));
    }

    private CompraInput buildCompraInput(InsumoCompraRequest compraRequest) {
        return new CompraInput(
                compraRequest.fechaCompra(),
                compraRequest.cantidadComprada(),
                compraRequest.cantidadMlComprados(),
                normalizarUnidadMedida(compraRequest.unidadMedida()),
                compraRequest.precioCompraTotal(),
                compraRequest.precioNeto(),
                compraRequest.iva(),
                compraRequest.ancho(),
                compraRequest.alto(),
                normalizarTipoDocumento(compraRequest.tipoDocumento()),
                normalizarTexto(compraRequest.numeroDocumento()),
                normalizarTexto(compraRequest.documentoUrl()),
                normalizarTexto(compraRequest.observacion()));
    }

    private void validarCompraInput(CompraInput compraInput) {
        if (compraInput.fechaCompra() == null) {
            throw new IllegalArgumentException("La fecha de compra es obligatoria.");
        }
        if (compraInput.cantidadComprada() == null || compraInput.cantidadComprada().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad comprada debe ser mayor a cero.");
        }
        if (compraInput.unidadMedida() == null || compraInput.unidadMedida().isBlank()) {
            throw new IllegalArgumentException("La unidad de medida es obligatoria.");
        }
        if (isUnidadMililitros(compraInput.unidadMedida())
                && (compraInput.cantidadMlComprados() == null
                        || compraInput.cantidadMlComprados().compareTo(BigDecimal.ZERO) <= 0)) {
            throw new IllegalArgumentException("La cantidad de ML comprados es obligatoria para insumos medidos en ML.");
        }
        if (compraInput.precioCompraTotal() == null || compraInput.precioCompraTotal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El precio total de compra debe ser mayor a cero.");
        }
        validarMedidaPositiva(compraInput.ancho(), "El ancho debe ser mayor a cero.");
        validarMedidaPositiva(compraInput.alto(), "El alto debe ser mayor a cero.");
        validarNoNegativo(compraInput.precioNeto(), "El precio neto no puede ser negativo.");
        validarNoNegativo(compraInput.iva(), "El IVA no puede ser negativo.");
    }

    private void validarCodigoProductoDisponible(String codigoProducto, Long insumoId) {
        boolean codigoDuplicado =
                insumoId == null
                        ? insumoRepository.existsByCodigoProductoIgnoreCase(codigoProducto)
                        : insumoRepository.existsByCodigoProductoIgnoreCaseAndIdNot(codigoProducto, insumoId);

        if (codigoDuplicado) {
            throw new IllegalArgumentException(
                    "Ya existe un insumo con el codigo de producto informado.");
        }
    }

    private Optional<InsumoCompra> obtenerCompraVigenteInterna(Long insumoId) {
        Optional<InsumoCompra> vigente =
                insumoCompraRepository.findFirstByInsumoIdAndVigenteTrueOrderByFechaCompraDescCreatedAtDescIdDesc(insumoId);
        if (vigente != null && vigente.isPresent()) {
            return vigente;
        }

        Optional<InsumoCompra> ultima =
                insumoCompraRepository.findFirstByInsumoIdOrderByFechaCompraDescCreatedAtDescIdDesc(insumoId);
        return ultima == null ? Optional.empty() : ultima;
    }

    private List<InsumoCompra> obtenerComprasOrdenadas(Long insumoId) {
        List<InsumoCompra> compras =
                insumoCompraRepository.findByInsumoIdOrderByFechaCompraDescCreatedAtDescIdDesc(insumoId);
        return compras == null ? List.of() : compras;
    }

    private CompraResumen construirResumenCompra(Long insumoId) {
        List<InsumoCompra> compras = obtenerComprasOrdenadas(insumoId);
        if (compras.isEmpty()) {
            return new CompraResumen(null, 0, false, null, null, null);
        }

        InsumoCompra vigente = obtenerCompraVigenteInterna(insumoId).orElse(compras.get(0));
        InsumoCompra anterior = compras.stream()
                .filter(compra -> !Objects.equals(compra.getId(), vigente.getId()))
                .findFirst()
                .orElse(null);

        boolean tieneCambioPrecio = anterior != null
                && !sameBigDecimal(vigente.getPrecioCompraTotal(), anterior.getPrecioCompraTotal());
        return new CompraResumen(
                vigente.getId(),
                compras.size(),
                tieneCambioPrecio,
                tieneCambioPrecio ? vigente.getFechaCompra() : null,
                anterior == null ? null : anterior.getPrecioCompraTotal(),
                calcularVariacionPorcentual(
                        anterior == null ? null : anterior.getPrecioCompraTotal(),
                        vigente.getPrecioCompraTotal()));
    }

    private boolean tieneCambioDeCompra(InsumoCompra compraActual, CompraInput compraInput) {
        return !sameBigDecimal(compraActual.getCantidadComprada(), compraInput.cantidadComprada())
                || !sameBigDecimal(compraActual.getCantidadMlComprados(), compraInput.cantidadMlComprados())
                || !Objects.equals(normalizarUnidadMedida(compraActual.getUnidadMedida()), compraInput.unidadMedida())
                || !sameBigDecimal(compraActual.getPrecioCompraTotal(), compraInput.precioCompraTotal())
                || !sameBigDecimal(compraActual.getPrecioNeto(), compraInput.precioNeto())
                || !sameBigDecimal(compraActual.getIva(), compraInput.iva())
                || !sameBigDecimal(compraActual.getAncho(), compraInput.ancho())
                || !sameBigDecimal(compraActual.getAlto(), compraInput.alto());
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

    private BigDecimal calcularPrecioUnitario(
            BigDecimal cantidadComprada,
            BigDecimal cantidadMlComprados,
            String unidadMedida,
            BigDecimal precioCompraTotal) {
        BigDecimal divisor = isUnidadMililitros(unidadMedida) ? cantidadMlComprados : cantidadComprada;
        return precioCompraTotal.divide(divisor, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularVariacionPorcentual(BigDecimal precioAnterior, BigDecimal precioActual) {
        if (precioAnterior == null || precioActual == null || precioAnterior.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        return precioActual.subtract(precioAnterior)
                .multiply(new BigDecimal("100"))
                .divide(precioAnterior, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal scaleOptional(BigDecimal value, int scale) {
        return value == null ? null : value.setScale(scale, RoundingMode.HALF_UP);
    }

    private boolean sameBigDecimal(BigDecimal left, BigDecimal right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.compareTo(right) == 0;
    }

    private void validarMedidaPositiva(BigDecimal value, String message) {
        if (value != null && value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private void validarNoNegativo(BigDecimal value, String message) {
        if (value != null && value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private Insumo obtenerEntidad(Long id) {
        return insumoRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Insumo no encontrado."));
    }

    private String normalizarCodigoProducto(String codigoProducto) {
        return codigoProducto.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizarUnidadMedida(String unidadMedida) {
        return unidadMedida == null ? null : unidadMedida.trim();
    }

    private boolean isUnidadMililitros(String unidadMedida) {
        String valorNormalizado = normalizarUnidadMedida(unidadMedida);
        if (valorNormalizado == null) {
            return false;
        }

        String comparable = valorNormalizado.toLowerCase(Locale.ROOT);
        return comparable.equals("ml") || comparable.equals("mililitro") || comparable.equals("mililitros");
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
        CompraResumen resumen = construirResumenCompra(insumo.getId());

        return new InsumoResponse(
                insumo.getId(),
                insumo.getCodigoProducto(),
                insumo.getNombre(),
                categoria.getCodigo(),
                categoria.getLabel(),
                insumo.getUnidadMedida(),
                insumo.getCantidadComprada(),
                insumo.getCantidadMlComprados(),
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
                resumen.compraVigenteId(),
                resumen.totalCompras(),
                resumen.tieneCambioPrecio(),
                resumen.ultimoCambioPrecio(),
                resumen.precioCompraAnterior(),
                resumen.variacionPrecioPorcentual(),
                insumo.getCreatedAt(),
                insumo.getUpdatedAt());
    }

    private InsumoCompraResponse toCompraResponse(InsumoCompra compra) {
        return new InsumoCompraResponse(
                compra.getId(),
                compra.getInsumo().getId(),
                compra.getFechaCompra(),
                compra.getCantidadComprada(),
                compra.getCantidadMlComprados(),
                compra.getUnidadMedida(),
                compra.getPrecioCompraTotal(),
                compra.getPrecioUnitario(),
                compra.getPrecioNeto(),
                compra.getIva(),
                compra.getAncho(),
                compra.getAlto(),
                compra.getTipoDocumento(),
                compra.getNumeroDocumento(),
                compra.getDocumentoUrl(),
                compra.getObservacion(),
                compra.isVigente(),
                compra.getCreatedAt(),
                compra.getUpdatedAt());
    }

    private String normalizarTexto(String valor) {
        if (valor == null) {
            return null;
        }

        String valorNormalizado = valor.trim();
        return valorNormalizado.isEmpty() ? null : valorNormalizado;
    }

    private record CompraInput(
            LocalDate fechaCompra,
            BigDecimal cantidadComprada,
            BigDecimal cantidadMlComprados,
            String unidadMedida,
            BigDecimal precioCompraTotal,
            BigDecimal precioNeto,
            BigDecimal iva,
            BigDecimal ancho,
            BigDecimal alto,
            String tipoDocumento,
            String numeroDocumento,
            String documentoUrl,
            String observacion) {}

    private record CompraResumen(
            Long compraVigenteId,
            long totalCompras,
            boolean tieneCambioPrecio,
            LocalDate ultimoCambioPrecio,
            BigDecimal precioCompraAnterior,
            BigDecimal variacionPrecioPorcentual) {}
}
